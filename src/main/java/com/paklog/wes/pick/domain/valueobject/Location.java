package com.paklog.wes.pick.domain.valueobject;

import java.util.Objects;

/**
 * Warehouse location identifier
 * Represents a specific physical location in the warehouse
 */
public record Location(
        String aisle,
        String bay,
        String level,
        String position
) {

    public Location {
        Objects.requireNonNull(aisle, "Aisle cannot be null");
        Objects.requireNonNull(bay, "Bay cannot be null");
        Objects.requireNonNull(level, "Level cannot be null");
        // Position can be null for some locations
    }

    /**
     * Calculate Manhattan distance between two locations
     * Used for path optimization
     */
    public double distanceFrom(Location other) {
        if (other == null) {
            return Double.MAX_VALUE;
        }

        // If in different aisles, distance is large
        if (!this.aisle.equals(other.aisle)) {
            return Math.abs(parseNumeric(this.aisle) - parseNumeric(other.aisle)) * 100.0;
        }

        // Same aisle - calculate based on bay and level
        double bayDistance = Math.abs(parseNumeric(this.bay) - parseNumeric(other.bay)) * 10.0;
        double levelDistance = Math.abs(parseNumeric(this.level) - parseNumeric(other.level)) * 2.0;

        return bayDistance + levelDistance;
    }

    /**
     * Get display string for UI
     */
    public String toDisplayString() {
        if (position != null) {
            return String.format("%s-%s-%s-%s", aisle, bay, level, position);
        }
        return String.format("%s-%s-%s", aisle, bay, level);
    }

    /**
     * Check if location is in same aisle
     */
    public boolean isSameAisle(Location other) {
        return other != null && this.aisle.equals(other.aisle);
    }

    /**
     * Check if location is adjacent (same aisle, adjacent bay)
     */
    public boolean isAdjacent(Location other) {
        if (!isSameAisle(other)) {
            return false;
        }

        int thisBay = parseNumeric(this.bay);
        int otherBay = parseNumeric(other.bay);

        return Math.abs(thisBay - otherBay) == 1;
    }

    private int parseNumeric(String value) {
        try {
            // Remove any non-numeric prefix (e.g., "A01" -> "01")
            String numeric = value.replaceAll("[^0-9]", "");
            return numeric.isEmpty() ? 0 : Integer.parseInt(numeric);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
