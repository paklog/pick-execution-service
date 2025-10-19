package com.paklog.wes.pick.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PickPath Value Object Tests")
class PickPathTest {

    private final Location location = new Location("A", "01", "01", "01");

    @Test
    @DisplayName("Should create pick path with nodes")
    void shouldCreatePickPath() {
        PickPath.PathNode node = new PickPath.PathNode("INST-1", location, 0, 0.0);
        PickPath path = new PickPath(List.of(node), 12.5, Duration.ofMinutes(5), "NEAREST_NEIGHBOR");

        assertThat(path.getFirstNode()).isEqualTo(node);
        assertThat(path.getTotalPicks()).isEqualTo(1);
        assertThat(path.isOptimized()).isTrue();
    }

    @Test
    @DisplayName("Should access node by index and calculate progress")
    void shouldAccessNodeAndCalculateProgress() {
        PickPath path = new PickPath(
                List.of(
                        new PickPath.PathNode("INST-1", location, 0, 0.0),
                        new PickPath.PathNode("INST-2", location, 1, 10.0)
                ),
                20.0,
                Duration.ofMinutes(8),
                "S_SHAPE"
        );

        assertThat(path.getNodeAt(1).instructionId()).isEqualTo("INST-2");
        assertThat(path.calculateProgress(1)).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Should validate arguments on creation")
    void shouldValidateArguments() {
        assertThatThrownBy(() -> new PickPath(List.of(), 10.0, Duration.ofMinutes(1), "TEST"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one node");

        assertThatThrownBy(() -> new PickPath(
                List.of(new PickPath.PathNode("INST-1", location, 0, 0.0)),
                -1,
                Duration.ofMinutes(1),
                "TEST"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");

        assertThatThrownBy(() -> new PickPath.PathNode("INST-1", location, -1, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");

        assertThatThrownBy(() -> new PickPath.PathNode("INST-1", location, 0, -5.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    @DisplayName("Should treat sequential algorithm as non-optimized")
    void shouldDetectSequentialAlgorithm() {
        PickPath path = new PickPath(
                List.of(new PickPath.PathNode("INST-1", location, 0, 0.0)),
                5.0,
                Duration.ZERO,
                "SEQUENTIAL"
        );

        assertThat(path.isOptimized()).isFalse();
    }
}
