package com.paklog.wes.pick.domain.entity;

import com.paklog.wes.pick.domain.valueobject.Priority;
import com.paklog.wes.pick.domain.valueobject.InstructionStatus;
import com.paklog.wes.pick.domain.valueobject.Location;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pick instruction entity
 * Represents a single item to be picked in a session
 */
public class PickInstruction {

    private String instructionId;
    private String itemSku;
    private String itemDescription;
    private int expectedQuantity;
    private int pickedQuantity;
    private Location location;
    private String orderId;
    private InstructionStatus status;
    private int sequenceNumber;
    private Priority priority;
    private double weight;
    private String uom;
    private List<String> specialHandling;
    private String shortPickReason;
    private LocalDateTime pickedAt;

    // For MongoDB/persistence
    public PickInstruction() {
        this.specialHandling = new ArrayList<>();
    }

    public PickInstruction(
            String instructionId,
            String itemSku,
            String itemDescription,
            int expectedQuantity,
            Location location,
            String orderId,
            Priority priority
    ) {
        this.instructionId = Objects.requireNonNull(instructionId, "Instruction ID cannot be null");
        this.itemSku = Objects.requireNonNull(itemSku, "Item SKU cannot be null");
        this.itemDescription = itemDescription;
        this.expectedQuantity = expectedQuantity;
        this.pickedQuantity = 0;
        this.location = Objects.requireNonNull(location, "Location cannot be null");
        this.orderId = orderId;
        this.status = InstructionStatus.PENDING;
        this.priority = priority != null ? priority : Priority.NORMAL;
        this.specialHandling = new ArrayList<>();

        if (expectedQuantity <= 0) {
            throw new IllegalArgumentException("Expected quantity must be positive");
        }
    }

    /**
     * Start picking this instruction
     */
    public void start() {
        ensureStatus(InstructionStatus.PENDING);
        this.status = InstructionStatus.IN_PROGRESS;
    }

    /**
     * Confirm full pick
     */
    public void confirmPick(int quantity) {
        ensureStatus(InstructionStatus.IN_PROGRESS);

        if (quantity <= 0) {
            throw new IllegalArgumentException("Picked quantity must be positive");
        }

        if (quantity > expectedQuantity) {
            throw new IllegalArgumentException(
                    String.format("Picked quantity (%d) exceeds expected quantity (%d)",
                            quantity, expectedQuantity)
            );
        }

        this.pickedQuantity = quantity;
        this.pickedAt = LocalDateTime.now();

        if (quantity == expectedQuantity) {
            this.status = InstructionStatus.PICKED;
        } else {
            this.status = InstructionStatus.SHORT_PICKED;
        }
    }

    /**
     * Handle short pick scenario
     */
    public void shortPick(int actualQuantity, String reason) {
        ensureStatus(InstructionStatus.IN_PROGRESS);

        if (actualQuantity < 0) {
            throw new IllegalArgumentException("Actual quantity cannot be negative");
        }

        if (actualQuantity >= expectedQuantity) {
            throw new IllegalArgumentException("Use confirmPick for full quantity picks");
        }

        this.pickedQuantity = actualQuantity;
        this.shortPickReason = reason;
        this.status = InstructionStatus.SHORT_PICKED;
        this.pickedAt = LocalDateTime.now();
    }

    /**
     * Skip this instruction
     */
    public void skip(String reason) {
        ensureStatus(InstructionStatus.PENDING, InstructionStatus.IN_PROGRESS);
        this.status = InstructionStatus.SKIPPED;
        this.shortPickReason = reason;
    }

    /**
     * Cancel instruction
     */
    public void cancel() {
        if (status.isComplete()) {
            throw new IllegalStateException("Cannot cancel completed instruction");
        }
        this.status = InstructionStatus.CANCELLED;
    }

    /**
     * Set sequence number for path optimization
     */
    public void setSequenceNumber(int sequenceNumber) {
        if (sequenceNumber < 0) {
            throw new IllegalArgumentException("Sequence number cannot be negative");
        }
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Check if instruction is complete
     */
    public boolean isComplete() {
        return status.isComplete();
    }

    /**
     * Check if this was a short pick
     */
    public boolean isShortPick() {
        return status == InstructionStatus.SHORT_PICKED;
    }

    /**
     * Get shortage quantity
     */
    public int getShortageQuantity() {
        if (status != InstructionStatus.SHORT_PICKED) {
            return 0;
        }
        return expectedQuantity - pickedQuantity;
    }

    /**
     * Calculate pick accuracy for this instruction
     */
    public double getAccuracy() {
        if (expectedQuantity == 0) {
            return 100.0;
        }
        return (pickedQuantity / (double) expectedQuantity) * 100.0;
    }

    /**
     * Add special handling requirement
     */
    public void addSpecialHandling(String handling) {
        if (handling != null && !handling.isBlank()) {
            this.specialHandling.add(handling);
        }
    }

    /**
     * Check if instruction requires special handling
     */
    public boolean hasSpecialHandling() {
        return !specialHandling.isEmpty();
    }

    private void ensureStatus(InstructionStatus... allowedStatuses) {
        for (InstructionStatus allowed : allowedStatuses) {
            if (this.status == allowed) {
                return;
            }
        }
        throw new IllegalStateException(
                String.format("Invalid status transition. Current: %s, Expected: %s",
                        this.status, List.of(allowedStatuses))
        );
    }

    // Getters and setters

    public String getInstructionId() {
        return instructionId;
    }

    public void setInstructionId(String instructionId) {
        this.instructionId = instructionId;
    }

    public String getItemSku() {
        return itemSku;
    }

    public void setItemSku(String itemSku) {
        this.itemSku = itemSku;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public int getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(int expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public int getPickedQuantity() {
        return pickedQuantity;
    }

    public void setPickedQuantity(int pickedQuantity) {
        this.pickedQuantity = pickedQuantity;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public InstructionStatus getStatus() {
        return status;
    }

    public void setStatus(InstructionStatus status) {
        this.status = status;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public List<String> getSpecialHandling() {
        return specialHandling;
    }

    public void setSpecialHandling(List<String> specialHandling) {
        this.specialHandling = specialHandling;
    }

    public String getShortPickReason() {
        return shortPickReason;
    }

    public void setShortPickReason(String shortPickReason) {
        this.shortPickReason = shortPickReason;
    }

    public LocalDateTime getPickedAt() {
        return pickedAt;
    }

    public void setPickedAt(LocalDateTime pickedAt) {
        this.pickedAt = pickedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PickInstruction that = (PickInstruction) o;
        return Objects.equals(instructionId, that.instructionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instructionId);
    }

    @Override
    public String toString() {
        return "PickInstruction{" +
                "instructionId='" + instructionId + '\'' +
                ", itemSku='" + itemSku + '\'' +
                ", expectedQuantity=" + expectedQuantity +
                ", pickedQuantity=" + pickedQuantity +
                ", location=" + location +
                ", status=" + status +
                '}';
    }
}
