package com.paklog.wes.pick.domain.aggregate;

import com.paklog.wes.pick.domain.shared.AggregateRoot;
import com.paklog.wes.pick.domain.shared.DomainEvent;
import com.paklog.wes.pick.domain.entity.PickInstruction;
import com.paklog.wes.pick.domain.event.PickConfirmedEvent;
import com.paklog.wes.pick.domain.event.PickSessionCancelledEvent;
import com.paklog.wes.pick.domain.event.PickSessionCompletedEvent;
import com.paklog.wes.pick.domain.event.PickSessionStartedEvent;
import com.paklog.wes.pick.domain.event.ShortPickEvent;
import com.paklog.wes.pick.domain.valueobject.InstructionStatus;
import com.paklog.wes.pick.domain.valueobject.PickPath;
import com.paklog.wes.pick.domain.valueobject.PickStrategy;
import com.paklog.wes.pick.domain.valueobject.SessionStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PickSession aggregate root
 * Manages the complete lifecycle of a picking session
 */
@AggregateRoot
@Document(collection = "pick_sessions")
public class PickSession {

    @Id
    private String sessionId;

    @Version
    private Long version;

    private String taskId;
    private String workerId;
    private String warehouseId;
    private PickStrategy strategy;
    private SessionStatus status;
    private String cartId;
    private List<PickInstruction> pickInstructions;
    private PickPath optimizedPath;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime pausedAt;
    private int currentInstructionIndex;
    private String cancellationReason;

    private List<DomainEvent> domainEvents = new ArrayList<>();

    // For MongoDB/persistence
    public PickSession() {
        this.pickInstructions = new ArrayList<>();
    }

    /**
     * Create a new pick session
     */
    public static PickSession create(
            String taskId,
            String workerId,
            String warehouseId,
            PickStrategy strategy,
            String cartId,
            List<PickInstruction> instructions
    ) {
        PickSession session = new PickSession();
        session.sessionId = generateSessionId();
        session.taskId = Objects.requireNonNull(taskId, "Task ID cannot be null");
        session.workerId = Objects.requireNonNull(workerId, "Worker ID cannot be null");
        session.warehouseId = Objects.requireNonNull(warehouseId, "Warehouse ID cannot be null");
        session.strategy = Objects.requireNonNull(strategy, "Strategy cannot be null");
        session.cartId = Objects.requireNonNull(cartId, "Cart ID cannot be null");
        session.pickInstructions = new ArrayList<>(Objects.requireNonNull(instructions, "Instructions cannot be null"));
        session.status = SessionStatus.CREATED;
        session.createdAt = LocalDateTime.now();
        session.currentInstructionIndex = 0;

        // Validate
        if (session.pickInstructions.isEmpty()) {
            throw new IllegalArgumentException("Session must have at least one pick instruction");
        }

        return session;
    }

    /**
     * Start the pick session
     */
    public void start(PickPath optimizedPath) {
        ensureStatus(SessionStatus.CREATED);
        Objects.requireNonNull(optimizedPath, "Optimized path cannot be null");

        this.optimizedPath = optimizedPath;
        this.status = SessionStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();

        // Apply path sequence to instructions
        applyPathSequence();

        // Register event
        registerEvent(new PickSessionStartedEvent(
                this.sessionId,
                this.taskId,
                this.workerId,
                this.warehouseId,
                this.strategy,
                this.cartId,
                this.pickInstructions.size()
        ));
    }

    /**
     * Confirm pick for current instruction
     */
    public void confirmPick(String instructionId, int quantity) {
        ensureStatus(SessionStatus.IN_PROGRESS);

        PickInstruction instruction = findInstruction(instructionId);

        // Auto-start instruction if PENDING
        if (instruction.getStatus() == InstructionStatus.PENDING) {
            instruction.start();
        }

        instruction.confirmPick(quantity);

        // Register event
        registerEvent(new PickConfirmedEvent(
                this.sessionId,
                instructionId,
                instruction.getItemSku(),
                quantity,
                instruction.getLocation(),
                this.workerId
        ));

        // Move to next instruction if current is complete
        moveToNextInstruction();
    }

    /**
     * Handle short pick scenario
     */
    public void shortPick(String instructionId, int actualQuantity, String reason) {
        ensureStatus(SessionStatus.IN_PROGRESS);

        PickInstruction instruction = findInstruction(instructionId);

        // Auto-start instruction if PENDING
        if (instruction.getStatus() == InstructionStatus.PENDING) {
            instruction.start();
        }

        instruction.shortPick(actualQuantity, reason);

        // Register event
        registerEvent(new ShortPickEvent(
                this.sessionId,
                instructionId,
                instruction.getItemSku(),
                instruction.getExpectedQuantity(),
                actualQuantity,
                instruction.getLocation(),
                reason,
                this.workerId
        ));

        // Move to next instruction
        moveToNextInstruction();
    }

    /**
     * Pause the session
     */
    public void pause() {
        ensureStatus(SessionStatus.IN_PROGRESS);
        this.status = SessionStatus.PAUSED;
        this.pausedAt = LocalDateTime.now();
    }

    /**
     * Resume the session
     */
    public void resume() {
        ensureStatus(SessionStatus.PAUSED);
        this.status = SessionStatus.IN_PROGRESS;
        this.pausedAt = null;
    }

    /**
     * Complete the session
     */
    public void complete() {
        ensureStatus(SessionStatus.IN_PROGRESS);

        // Verify all instructions are complete
        if (hasPendingInstructions()) {
            throw new IllegalStateException("Cannot complete session with pending instructions");
        }

        this.status = SessionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();

        // Register event
        registerEvent(new PickSessionCompletedEvent(
                this.sessionId,
                this.taskId,
                this.workerId,
                this.warehouseId,
                this.pickInstructions.size(),
                getCompletedInstructionCount(),
                getShortPickCount(),
                calculateAccuracy(),
                getDuration()
        ));
    }

    /**
     * Cancel the session
     */
    public void cancel(String reason) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot cancel session in terminal state: " + status);
        }

        this.status = SessionStatus.CANCELLED;
        this.cancellationReason = reason;
        this.completedAt = LocalDateTime.now();

        // Register event
        registerEvent(new PickSessionCancelledEvent(
                this.sessionId,
                this.taskId,
                this.workerId,
                this.warehouseId,
                reason,
                getCompletedInstructionCount(),
                this.pickInstructions.size()
        ));
    }

    /**
     * Get current instruction
     */
    public PickInstruction getCurrentInstruction() {
        if (currentInstructionIndex >= pickInstructions.size()) {
            return null; // All instructions completed
        }
        return pickInstructions.get(currentInstructionIndex);
    }

    /**
     * Get next instruction
     */
    public PickInstruction getNextInstruction() {
        int nextIndex = currentInstructionIndex + 1;
        if (nextIndex >= pickInstructions.size()) {
            return null;
        }
        return pickInstructions.get(nextIndex);
    }

    /**
     * Calculate session progress
     */
    public double getProgress() {
        if (pickInstructions.isEmpty()) {
            return 100.0;
        }
        return (getCompletedInstructionCount() / (double) pickInstructions.size()) * 100.0;
    }

    /**
     * Calculate pick accuracy
     */
    public double calculateAccuracy() {
        if (pickInstructions.isEmpty()) {
            return 100.0;
        }

        int totalExpected = pickInstructions.stream()
                .mapToInt(PickInstruction::getExpectedQuantity)
                .sum();

        int totalPicked = pickInstructions.stream()
                .filter(i -> i.getStatus().isComplete())
                .mapToInt(PickInstruction::getPickedQuantity)
                .sum();

        if (totalExpected == 0) {
            return 100.0;
        }

        return (totalPicked / (double) totalExpected) * 100.0;
    }

    /**
     * Get session duration
     */
    public Duration getDuration() {
        if (startedAt == null) {
            return Duration.ZERO;
        }

        LocalDateTime endTime = completedAt != null ? completedAt : LocalDateTime.now();
        return Duration.between(startedAt, endTime);
    }

    /**
     * Get completed instruction count
     */
    public int getCompletedInstructionCount() {
        return (int) pickInstructions.stream()
                .filter(PickInstruction::isComplete)
                .count();
    }

    /**
     * Get short pick count
     */
    public int getShortPickCount() {
        return (int) pickInstructions.stream()
                .filter(PickInstruction::isShortPick)
                .count();
    }

    /**
     * Check if there are pending instructions
     */
    public boolean hasPendingInstructions() {
        return pickInstructions.stream()
                .anyMatch(i -> !i.isComplete());
    }

    /**
     * Get all instructions with specific status
     */
    public List<PickInstruction> getInstructionsByStatus(InstructionStatus status) {
        return pickInstructions.stream()
                .filter(i -> i.getStatus() == status)
                .collect(Collectors.toList());
    }

    // Private helper methods

    private void applyPathSequence() {
        if (optimizedPath == null || optimizedPath.nodes().isEmpty()) {
            return;
        }

        for (PickPath.PathNode node : optimizedPath.nodes()) {
            PickInstruction instruction = pickInstructions.stream()
                    .filter(i -> i.getInstructionId().equals(node.instructionId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Path node references unknown instruction: " + node.instructionId()));

            instruction.setSequenceNumber(node.sequenceNumber());
        }

        // Sort instructions by sequence number
        pickInstructions.sort((a, b) -> Integer.compare(a.getSequenceNumber(), b.getSequenceNumber()));
    }

    private void moveToNextInstruction() {
        while (currentInstructionIndex < pickInstructions.size()) {
            PickInstruction current = pickInstructions.get(currentInstructionIndex);
            if (current.isComplete()) {
                currentInstructionIndex++;
            } else {
                break;
            }
        }

        // Auto-complete if all done
        if (currentInstructionIndex >= pickInstructions.size() && status == SessionStatus.IN_PROGRESS) {
            complete();
        }
    }

    private PickInstruction findInstruction(String instructionId) {
        return pickInstructions.stream()
                .filter(i -> i.getInstructionId().equals(instructionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Instruction not found: " + instructionId));
    }

    private void ensureStatus(SessionStatus... allowedStatuses) {
        for (SessionStatus allowed : allowedStatuses) {
            if (this.status == allowed) {
                return;
            }
        }
        throw new IllegalStateException(
                String.format("Invalid status transition. Current: %s, Expected: %s",
                        this.status, List.of(allowedStatuses))
        );
    }

    private void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    private static String generateSessionId() {
        return "SESSION-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Getters and setters

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public PickStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(PickStrategy strategy) {
        this.strategy = strategy;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    public List<PickInstruction> getPickInstructions() {
        return pickInstructions;
    }

    public void setPickInstructions(List<PickInstruction> pickInstructions) {
        this.pickInstructions = pickInstructions;
    }

    public PickPath getOptimizedPath() {
        return optimizedPath;
    }

    public void setOptimizedPath(PickPath optimizedPath) {
        this.optimizedPath = optimizedPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getPausedAt() {
        return pausedAt;
    }

    public void setPausedAt(LocalDateTime pausedAt) {
        this.pausedAt = pausedAt;
    }

    public int getCurrentInstructionIndex() {
        return currentInstructionIndex;
    }

    public void setCurrentInstructionIndex(int currentInstructionIndex) {
        this.currentInstructionIndex = currentInstructionIndex;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public List<DomainEvent> getDomainEvents() {
        return domainEvents;
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PickSession that = (PickSession) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        return "PickSession{" +
                "sessionId='" + sessionId + '\'' +
                ", taskId='" + taskId + '\'' +
                ", workerId='" + workerId + '\'' +
                ", strategy=" + strategy +
                ", status=" + status +
                ", instructionCount=" + pickInstructions.size() +
                ", progress=" + String.format("%.1f%%", getProgress()) +
                '}';
    }
}
