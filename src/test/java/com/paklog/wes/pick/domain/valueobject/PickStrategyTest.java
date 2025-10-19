package com.paklog.wes.pick.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PickStrategy Tests")
class PickStrategyTest {

    @Test
    @DisplayName("Should identify strategies requiring put wall support")
    void shouldIdentifyPutWallRequirement() {
        assertThat(PickStrategy.CLUSTER.requiresPutWall()).isTrue();
        assertThat(PickStrategy.BATCH.requiresPutWall()).isTrue();
        assertThat(PickStrategy.SINGLE.requiresPutWall()).isFalse();
    }

    @Test
    @DisplayName("Should evaluate multiple order support and capacities")
    void shouldEvaluateOrderCapabilities() {
        assertThat(PickStrategy.BATCH.supportsMultipleOrders()).isTrue();
        assertThat(PickStrategy.SINGLE.supportsMultipleOrders()).isFalse();

        assertThat(PickStrategy.SINGLE.getMaxOrdersPerSession()).isEqualTo(1);
        assertThat(PickStrategy.WAVE.getMaxOrdersPerSession()).isEqualTo(50);
    }
}
