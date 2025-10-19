package com.paklog.wes.pick.domain.entity;

import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.pick.domain.valueobject.InstructionStatus;
import com.paklog.wes.pick.domain.valueobject.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PickInstruction Tests")
class PickInstructionTest {

    private static final Location LOCATION = new Location("A", "01", "01", "01");

    @Test
    @DisplayName("Should start instruction from pending state")
    void shouldStartInstruction() {
        PickInstruction instruction = createInstruction();

        instruction.start();

        assertThat(instruction.getStatus()).isEqualTo(InstructionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should confirm pick with full quantity")
    void shouldConfirmPick() {
        PickInstruction instruction = createInstruction();
        instruction.start();

        instruction.confirmPick(10);

        assertThat(instruction.getStatus()).isEqualTo(InstructionStatus.PICKED);
        assertThat(instruction.getPickedQuantity()).isEqualTo(10);
        assertThat(instruction.isComplete()).isTrue();
        assertThat(instruction.getAccuracy()).isEqualTo(100.0);
        assertThat(instruction.getPickedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should confirm pick with short quantity")
    void shouldConfirmShortPickViaConfirmPick() {
        PickInstruction instruction = createInstruction();
        instruction.start();

        instruction.confirmPick(8);

        assertThat(instruction.getStatus()).isEqualTo(InstructionStatus.SHORT_PICKED);
        assertThat(instruction.getPickedQuantity()).isEqualTo(8);
        assertThat(instruction.getShortageQuantity()).isEqualTo(2);
        assertThat(instruction.getAccuracy()).isEqualTo(80.0);
    }

    @Test
    @DisplayName("Should handle explicit short pick")
    void shouldHandleShortPick() {
        PickInstruction instruction = createInstruction();
        instruction.start();

        instruction.shortPick(3, "Damaged goods");

        assertThat(instruction.getStatus()).isEqualTo(InstructionStatus.SHORT_PICKED);
        assertThat(instruction.getPickedQuantity()).isEqualTo(3);
        assertThat(instruction.getShortPickReason()).isEqualTo("Damaged goods");
        assertThat(instruction.getShortageQuantity()).isEqualTo(7);
        assertThat(instruction.getAccuracy()).isEqualTo(30.0);
        assertThat(instruction.getPickedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should skip instruction with reason")
    void shouldSkipInstruction() {
        PickInstruction instruction = createInstruction();

        instruction.skip("Different order priority");

        assertThat(instruction.getStatus()).isEqualTo(InstructionStatus.SKIPPED);
        assertThat(instruction.getShortPickReason()).isEqualTo("Different order priority");
        assertThat(instruction.isComplete()).isTrue();
    }

    @Test
    @DisplayName("Should add special handling flag")
    void shouldAddSpecialHandling() {
        PickInstruction instruction = createInstruction();

        instruction.addSpecialHandling("Fragile");

        assertThat(instruction.hasSpecialHandling()).isTrue();
        assertThat(instruction.getSpecialHandling()).containsExactly("Fragile");
    }

    @Test
    @DisplayName("Should reject invalid quantities")
    void shouldRejectInvalidQuantities() {
        PickInstruction instruction = createInstruction();
        instruction.start();

        assertThatThrownBy(() -> instruction.confirmPick(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");

        assertThatThrownBy(() -> instruction.confirmPick(12))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds expected");

        assertThatThrownBy(() -> instruction.shortPick(-1, "invalid"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> instruction.shortPick(10, "use confirm"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("confirmPick");
    }

    @Test
    @DisplayName("Should reject invalid state transitions")
    void shouldRejectInvalidStateTransitions() {
        PickInstruction instruction = createInstruction();
        instruction.start();
        instruction.confirmPick(10);

        assertThatThrownBy(instruction::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("completed");
    }

    @Test
    @DisplayName("Should cancel instruction before completion")
    void shouldCancelInstruction() {
        PickInstruction instruction = createInstruction();
        instruction.start();

        instruction.cancel();

        assertThat(instruction.getStatus()).isEqualTo(InstructionStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should set sequence number with validation")
    void shouldSetSequenceNumber() {
        PickInstruction instruction = createInstruction();

        instruction.setSequenceNumber(5);
        assertThat(instruction.getSequenceNumber()).isEqualTo(5);

        assertThatThrownBy(() -> instruction.setSequenceNumber(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    @DisplayName("Should expose instruction attributes through getters")
    void shouldExposeInstructionAttributes() {
        PickInstruction instruction = createInstruction();

        assertThat(instruction.getInstructionId()).isEqualTo("INST-1");
        assertThat(instruction.getItemSku()).isEqualTo("SKU-1");
        assertThat(instruction.getExpectedQuantity()).isEqualTo(10);
        assertThat(instruction.getLocation()).isEqualTo(LOCATION);
        assertThat(instruction.getOrderId()).isEqualTo("ORDER-1");
        assertThat(instruction.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(instruction.hasSpecialHandling()).isFalse();
    }

    private PickInstruction createInstruction() {
        return new PickInstruction(
                "INST-1",
                "SKU-1",
                "Test item",
                10,
                LOCATION,
                "ORDER-1",
                Priority.HIGH
        );
    }
}
