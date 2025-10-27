package com.paklog.wes.pick.domain.valueobject;

/**
 * Pick task priority levels
 * Copied from paklog-domain to eliminate compilation dependency
 */
public enum Priority {
    URGENT,   // Pick immediately (expedited orders)
    HIGH,     // High priority picks
    NORMAL,   // Standard picks
    LOW;      // Low priority, batch picks

    /**
     * Parse priority from string (case-insensitive)
     */
    public static Priority fromString(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }
        try {
            return Priority.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
