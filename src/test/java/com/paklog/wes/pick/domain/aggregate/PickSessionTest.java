package com.paklog.wes.pick.domain.aggregate;

import com.paklog.domain.valueobject.Priority;
import com.paklog.domain.shared.DomainEvent;
import com.paklog.wes.pick.domain.entity.PickInstruction;
import com.paklog.wes.pick.domain.valueobject.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PickSession aggregate
 */
@DisplayName("PickSession Tests")
class PickSessionTest {

    @Test
    @DisplayName("Should create pick session successfully")
    void shouldCreatePickSession() {
        // Given
        List<PickInstruction> instructions = createTestInstructions(3);

        // When
        PickSession session = PickSession.create(
                "TASK-001",
                "WORKER-001",
                "WH-001",
                PickStrategy.BATCH,
                "CART-001",
                instructions
        );

        // Then
        assertThat(session.getSessionId()).isNotNull();
        assertThat(session.getTaskId()).isEqualTo("TASK-001");
        assertThat(session.getWorkerId()).isEqualTo("WORKER-001");
        assertThat(session.getStatus()).isEqualTo(SessionStatus.CREATED);
        assertThat(session.getPickInstructions()).hasSize(3);
    }

    @Test
    @DisplayName("Should reject session creation without instructions")
    void shouldRejectCreationWithoutInstructions() {
        // When/Then
        assertThatThrownBy(() -> PickSession.create(
                "TASK-001",
                "WORKER-001",
                "WH-001",
                PickStrategy.SINGLE,
                "CART-001",
                List.of()
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("at least one");
    }

    @Test
    @DisplayName("Should start session with optimized path")
    void shouldStartSession() {
        // Given
        List<PickInstruction> instructions = createTestInstructions(3);
        PickSession session = PickSession.create(
                "TASK-001", "WORKER-001", "WH-001",
                PickStrategy.BATCH, "CART-001", instructions
        );

        PickPath path = createTestPath(instructions);

        // When
        session.start(path);

        // Then
        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(session.getStartedAt()).isNotNull();
        assertThat(session.getOptimizedPath()).isEqualTo(path);
    }

    @Test
    @DisplayName("Should confirm pick successfully")
    void shouldConfirmPick() {
        // Given
        PickSession session = createStartedSession(2);
        PickInstruction instruction = session.getCurrentInstruction();

        // When
        session.confirmPick(instruction.getInstructionId(), instruction.getExpectedQuantity());

        // Then
        assertThat(instruction.getStatus()).isEqualTo(InstructionStatus.PICKED);
        assertThat(instruction.getPickedQuantity()).isEqualTo(instruction.getExpectedQuantity());
    }

    @Test
    @DisplayName("Should handle short pick")
    void shouldHandleShortPick() {
        // Given
        PickSession session = createStartedSession(2);
        PickInstruction instruction = session.getCurrentInstruction();
        int expectedQty = instruction.getExpectedQuantity();

        // When
        session.shortPick(instruction.getInstructionId(), expectedQty - 5, "Not enough stock");

        // Then
        assertThat(instruction.getStatus()).isEqualTo(InstructionStatus.SHORT_PICKED);
        assertThat(instruction.getPickedQuantity()).isEqualTo(expectedQty - 5);
        assertThat(instruction.getShortPickReason()).isEqualTo("Not enough stock");
    }

    @Test
    @DisplayName("Should pause and resume session")
    void shouldPauseAndResumeSession() {
        // Given
        PickSession session = createStartedSession(2);

        // When - Pause
        session.pause();

        // Then
        assertThat(session.getStatus()).isEqualTo(SessionStatus.PAUSED);
        assertThat(session.getPausedAt()).isNotNull();

        // When - Resume
        session.resume();

        // Then
        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(session.getPausedAt()).isNull();
    }

    @Test
    @DisplayName("Should complete session when all picks done")
    void shouldCompleteSession() {
        // Given
        PickSession session = createStartedSession(2);

        // Pick all instructions
        for (PickInstruction instruction : session.getPickInstructions()) {
            session.confirmPick(instruction.getInstructionId(), instruction.getExpectedQuantity());
        }

        // Then - Should auto-complete
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getCompletedAt()).isNotNull();
        assertThat(session.getCurrentInstruction()).isNull();
        assertThat(session.getNextInstruction()).isNull();
        assertThat(session.hasPendingInstructions()).isFalse();
    }

    @Test
    @DisplayName("Should cancel session")
    void shouldCancelSession() {
        // Given
        PickSession session = createStartedSession(2);

        // When
        session.cancel("Worker unavailable");

        // Then
        assertThat(session.getStatus()).isEqualTo(SessionStatus.CANCELLED);
        assertThat(session.getCancellationReason()).isEqualTo("Worker unavailable");
        assertThat(session.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should calculate progress correctly")
    void shouldCalculateProgress() {
        // Given
        PickSession session = createStartedSession(4);

        // When - Complete 2 out of 4
        List<PickInstruction> instructions = new ArrayList<>(session.getPickInstructions());
        session.confirmPick(instructions.get(0).getInstructionId(), instructions.get(0).getExpectedQuantity());
        session.confirmPick(instructions.get(1).getInstructionId(), instructions.get(1).getExpectedQuantity());

        // Then
        assertThat(session.getProgress()).isEqualTo(50.0);
        assertThat(session.getCompletedInstructionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should calculate accuracy correctly")
    void shouldCalculateAccuracy() {
        // Given
        PickSession session = createStartedSession(3);
        List<PickInstruction> instructions = new ArrayList<>(session.getPickInstructions());

        // When - Pick with different accuracies
        session.confirmPick(instructions.get(0).getInstructionId(), instructions.get(0).getExpectedQuantity()); // 100%
        session.shortPick(instructions.get(1).getInstructionId(), 5, "shortage"); // 50% (expected 10, got 5)
        session.confirmPick(instructions.get(2).getInstructionId(), instructions.get(2).getExpectedQuantity()); // 100%

        // Then
        double accuracy = session.calculateAccuracy();
        assertThat(accuracy).isBetween(80.0, 90.0); // ~83.3%
    }

    @Test
    @DisplayName("Should get current instruction")
    void shouldGetCurrentInstruction() {
        // Given
        PickSession session = createStartedSession(3);

        // When
        PickInstruction current = session.getCurrentInstruction();

        // Then
        assertThat(current).isNotNull();
        assertThat(current.getSequenceNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should move to next instruction after pick")
    void shouldMoveToNextInstruction() {
        // Given
        PickSession session = createStartedSession(3);
        PickInstruction firstInstruction = session.getCurrentInstruction();

        // When
        session.confirmPick(firstInstruction.getInstructionId(), firstInstruction.getExpectedQuantity());

        // Then
        PickInstruction secondInstruction = session.getCurrentInstruction();
        assertThat(secondInstruction).isNotEqualTo(firstInstruction);
        assertThat(secondInstruction.getSequenceNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should validate state transitions")
    void shouldValidateStateTransitions() {
        // Given
        PickSession session = createStartedSession(2);

        // When/Then - Cannot start already started session
        assertThatThrownBy(() -> session.start(createTestPath(session.getPickInstructions())))
                .isInstanceOf(IllegalStateException.class);

        // When/Then - Cannot resume non-paused session
        assertThatThrownBy(() -> session.resume())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Should get short pick count")
    void shouldGetShortPickCount() {
        // Given
        PickSession session = createStartedSession(3);
        List<PickInstruction> instructions = new ArrayList<>(session.getPickInstructions());

        // When
        session.confirmPick(instructions.get(0).getInstructionId(), instructions.get(0).getExpectedQuantity());
        session.shortPick(instructions.get(1).getInstructionId(), 5, "shortage");
        session.shortPick(instructions.get(2).getInstructionId(), 3, "damage");

        // Then
        assertThat(session.getShortPickCount()).isEqualTo(2);
        assertThat(session.getInstructionsByStatus(InstructionStatus.SHORT_PICKED)).hasSize(2);
    }

    @Test
    @DisplayName("Should calculate session duration")
    void shouldCalculateDuration() throws InterruptedException {
        // Given
        PickSession session = createStartedSession(1);

        // When
        Thread.sleep(100); // Wait a bit
        Duration duration = session.getDuration();

        // Then
        assertThat(duration).isNotNull();
        assertThat(duration.toMillis()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should register domain events during lifecycle")
    void shouldRegisterDomainEvents() {
        List<PickInstruction> instructions = createTestInstructions(3);
        PickSession session = PickSession.create(
                "TASK-001", "WORKER-001", "WH-001",
                PickStrategy.BATCH, "CART-001", instructions
        );
        PickPath path = createTestPath(instructions);
        session.start(path);

        PickInstruction first = session.getCurrentInstruction();
        session.confirmPick(first.getInstructionId(), first.getExpectedQuantity());

        PickInstruction second = session.getCurrentInstruction();
        session.shortPick(second.getInstructionId(), second.getExpectedQuantity() - 2, "partial");

        session.cancel("Manual stop");

        List<DomainEvent> events = session.getDomainEvents();
        assertThat(events).hasSize(4);
        assertThat(events.getFirst()).isInstanceOf(com.paklog.wes.pick.domain.event.PickSessionStartedEvent.class);
        assertThat(events.get(1)).isInstanceOf(com.paklog.wes.pick.domain.event.PickConfirmedEvent.class);
        assertThat(events.get(2)).isInstanceOf(com.paklog.wes.pick.domain.event.ShortPickEvent.class);
        assertThat(events.get(3)).isInstanceOf(com.paklog.wes.pick.domain.event.PickSessionCancelledEvent.class);

        session.clearDomainEvents();
        assertThat(session.getDomainEvents()).isEmpty();
    }

    // Helper methods

    private List<PickInstruction> createTestInstructions(int count) {
        List<PickInstruction> instructions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PickInstruction instruction = new PickInstruction(
                    "INST-" + (i + 1),
                    "SKU-" + (i + 1),
                    "Item " + (i + 1),
                    10,
                    new Location("A", String.format("%02d", i + 1), "01", "01"),
                    "ORDER-001",
                    Priority.NORMAL
            );
            instructions.add(instruction);
        }
        return instructions;
    }

    private PickPath createTestPath(List<PickInstruction> instructions) {
        List<PickPath.PathNode> nodes = new ArrayList<>();
        for (int i = 0; i < instructions.size(); i++) {
            PickInstruction instruction = instructions.get(i);
            nodes.add(new PickPath.PathNode(
                    instruction.getInstructionId(),
                    instruction.getLocation(),
                    i,
                    10.0
            ));
        }
        return new PickPath(nodes, 100.0, Duration.ofMinutes(10), "TEST");
    }

    private PickSession createStartedSession(int instructionCount) {
        List<PickInstruction> instructions = createTestInstructions(instructionCount);
        PickSession session = PickSession.create(
                "TASK-001", "WORKER-001", "WH-001",
                PickStrategy.BATCH, "CART-001", instructions
        );
        PickPath path = createTestPath(instructions);
        session.start(path);
        return session;
    }
}
