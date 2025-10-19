package com.paklog.wes.pick.domain.event;

import com.paklog.wes.pick.domain.valueobject.Location;
import com.paklog.wes.pick.domain.valueobject.PickStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Domain Events Tests")
class PickEventsTest {

    @Test
    @DisplayName("Should expose values from pick session started event")
    void shouldExposeStartedEventValues() {
        PickSessionStartedEvent event = new PickSessionStartedEvent(
                "SESSION-1",
                "TASK-1",
                "WORKER-1",
                "WH-1",
                PickStrategy.BATCH,
                "CART-1",
                5
        );

        assertThat(event.getSessionId()).isEqualTo("SESSION-1");
        assertThat(event.getStrategy()).isEqualTo(PickStrategy.BATCH);
        assertThat(event.getTotalInstructions()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should compute metrics from pick session completion event")
    void shouldComputeCompletionMetrics() {
        Duration duration = Duration.ofMinutes(120);
        PickSessionCompletedEvent event = new PickSessionCompletedEvent(
                "SESSION-2",
                "TASK-2",
                "WORKER-1",
                "WH-1",
                10,
                8,
                2,
                85.5,
                duration
        );

        assertThat(event.getPicksPerHour()).isEqualTo(4.0);
        assertThat(event.getShortPicks()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should compute completion percentage on cancel event")
    void shouldComputeCancellationMetrics() {
        PickSessionCancelledEvent event = new PickSessionCancelledEvent(
                "SESSION-3",
                "TASK-3",
                "WORKER-2",
                "WH-1",
                "Worker unavailable",
                4,
                8
        );

        assertThat(event.getCompletionPercentage()).isEqualTo(50.0);
        assertThat(event.getReason()).isEqualTo("Worker unavailable");
    }

    @Test
    @DisplayName("Should provide quantities on pick confirmation and short pick events")
    void shouldExposePickQuantities() {
        Location location = new Location("A", "01", "01", "01");

        PickConfirmedEvent confirmedEvent = new PickConfirmedEvent(
                "SESSION-4",
                "INST-1",
                "SKU-1",
                5,
                location,
                "WORKER-1"
        );

        ShortPickEvent shortPickEvent = new ShortPickEvent(
                "SESSION-4",
                "INST-2",
                "SKU-2",
                10,
                6,
                location,
                "Damaged items",
                "WORKER-1"
        );

        assertThat(confirmedEvent.getQuantity()).isEqualTo(5);
        assertThat(confirmedEvent.getLocation()).isEqualTo(location);
        assertThat(shortPickEvent.getShortageQuantity()).isEqualTo(4);
        assertThat(shortPickEvent.getReason()).isEqualTo("Damaged items");
    }
}
