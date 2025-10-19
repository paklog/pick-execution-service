package com.paklog.wes.pick.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Location Value Object Tests")
class LocationTest {

    @Test
    @DisplayName("Should calculate Manhattan distance between locations")
    void shouldCalculateDistance() {
        Location reference = new Location("A01", "05", "02", "01");
        Location sameAisle = new Location("A01", "07", "03", "01");
        Location otherAisle = new Location("B05", "01", "01", "01");

        assertThat(reference.distanceFrom(sameAisle)).isEqualTo((Math.abs(7 - 5) * 10.0) + (Math.abs(3 - 2) * 2.0));
        assertThat(reference.distanceFrom(otherAisle)).isEqualTo(Math.abs(5 - 1) * 100.0);
    }

    @Test
    @DisplayName("Should render display string with optional position")
    void shouldRenderDisplayString() {
        Location withPosition = new Location("A", "01", "01", "01");
        Location withoutPosition = new Location("A", "01", "01", null);

        assertThat(withPosition.toDisplayString()).isEqualTo("A-01-01-01");
        assertThat(withoutPosition.toDisplayString()).isEqualTo("A-01-01");
    }

    @Test
    @DisplayName("Should identify aisle comparisons")
    void shouldCompareAisles() {
        Location location = new Location("A", "01", "01", "01");
        Location sameAisle = new Location("A", "02", "01", "01");
        Location otherAisle = new Location("B", "01", "01", "01");

        assertThat(location.isSameAisle(sameAisle)).isTrue();
        assertThat(location.isSameAisle(otherAisle)).isFalse();
    }

    @Test
    @DisplayName("Should identify adjacent bays in same aisle")
    void shouldIdentifyAdjacentBays() {
        Location first = new Location("A", "01", "01", "01");
        Location adjacent = new Location("A", "02", "01", "01");
        Location nonAdjacent = new Location("A", "05", "01", "01");
        Location differentAisle = new Location("B", "02", "01", "01");

        assertThat(first.isAdjacent(adjacent)).isTrue();
        assertThat(first.isAdjacent(nonAdjacent)).isFalse();
        assertThat(first.isAdjacent(differentAisle)).isFalse();
    }
}
