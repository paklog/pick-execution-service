package com.paklog.wes.pick.domain.service;

import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.pick.domain.entity.PickInstruction;
import com.paklog.wes.pick.domain.valueobject.Location;
import com.paklog.wes.pick.domain.valueobject.PickPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PathOptimizationService Tests")
class PathOptimizationServiceTest {

    private final PathOptimizationService service = new PathOptimizationService();

    @Test
    @DisplayName("Should select nearest neighbor for small instruction sets")
    void shouldOptimizeWithNearestNeighbor() {
        Location start = new Location("A", "01", "01", "01");
        List<PickInstruction> instructions = List.of(
                instruction("INST-1", new Location("A", "02", "01", "01")),
                instruction("INST-2", new Location("A", "03", "01", "01")),
                instruction("INST-3", new Location("A", "04", "01", "01"))
        );

        PickPath path = service.optimizePath(instructions, start);

        assertThat(path.algorithm()).isEqualTo("NEAREST_NEIGHBOR");
        assertThat(path.nodes()).hasSize(3);
        assertThat(path.nodes()).isSortedAccordingTo((a, b) -> Integer.compare(a.sequenceNumber(), b.sequenceNumber()));
    }

    @Test
    @DisplayName("Should select S-Shape algorithm for larger instruction sets")
    void shouldOptimizeWithSShape() {
        Location start = new Location("A", "01", "01", "01");
        List<PickInstruction> instructions = new ArrayList<>();

        // 12 instructions across two aisles
        for (int i = 0; i < 6; i++) {
            instructions.add(instruction("A-" + i, new Location("A", String.format("%02d", i + 1), "01", "01")));
            instructions.add(instruction("B-" + i, new Location("B", String.format("%02d", i + 1), "01", "01")));
        }

        PickPath path = service.optimizePath(instructions, start);

        assertThat(path.algorithm()).isEqualTo("S_SHAPE");
        assertThat(path.nodes()).hasSize(12);
        assertThat(path.totalDistance()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should calculate sequential fallback path")
    void shouldCreateSequentialPath() {
        List<PickInstruction> instructions = List.of(
                instruction("INST-1", new Location("A", "01", "01", "01")),
                instruction("INST-2", new Location("A", "05", "01", "01"))
        );

        PickPath sequential = service.createSequentialPath(instructions);

        assertThat(sequential.algorithm()).isEqualTo("SEQUENTIAL");
        assertThat(sequential.getTotalPicks()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should compute savings compared to sequential path")
    void shouldComputeSavings() {
        List<PickInstruction> instructions = List.of(
                instruction("INST-1", new Location("A", "01", "01", "01")),
                instruction("INST-2", new Location("A", "05", "01", "01")),
                instruction("INST-3", new Location("A", "07", "01", "01"))
        );
        Location start = new Location("A", "01", "01", "01");

        PickPath optimized = service.optimizePath(instructions, start);
        double savings = service.calculateSavings(optimized, instructions);

        assertThat(savings).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("Should reject empty instructions")
    void shouldRejectEmptyInstructions() {
        assertThatThrownBy(() -> service.optimizePath(List.of(), new Location("A", "01", "01", "01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be empty");
    }

    private PickInstruction instruction(String id, Location location) {
        return new PickInstruction(
                id,
                "SKU-" + id,
                "Item " + id,
                1,
                location,
                "ORDER-1",
                Priority.NORMAL
        );
    }
}
