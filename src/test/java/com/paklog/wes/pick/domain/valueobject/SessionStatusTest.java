package com.paklog.wes.pick.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionStatus Tests")
class SessionStatusTest {

    @Test
    @DisplayName("Should identify active and terminal states")
    void shouldIdentifyStates() {
        assertThat(SessionStatus.IN_PROGRESS.isActive()).isTrue();
        assertThat(SessionStatus.PAUSED.isActive()).isTrue();
        assertThat(SessionStatus.COMPLETED.isActive()).isFalse();

        assertThat(SessionStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(SessionStatus.CREATED.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("Should validate status transitions")
    void shouldValidateTransitions() {
        assertThat(SessionStatus.CREATED.canTransitionTo(SessionStatus.IN_PROGRESS)).isTrue();
        assertThat(SessionStatus.CREATED.canTransitionTo(SessionStatus.COMPLETED)).isFalse();
        assertThat(SessionStatus.IN_PROGRESS.canTransitionTo(SessionStatus.FAILED)).isTrue();
        assertThat(SessionStatus.PAUSED.canTransitionTo(SessionStatus.IN_PROGRESS)).isTrue();
        assertThat(SessionStatus.CANCELLED.canTransitionTo(SessionStatus.CREATED)).isFalse();
    }
}
