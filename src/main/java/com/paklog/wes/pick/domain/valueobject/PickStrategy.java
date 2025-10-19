package com.paklog.wes.pick.domain.valueobject;

/**
 * Pick strategy determines how items are picked
 * Different strategies optimize for different warehouse layouts and order patterns
 */
public enum PickStrategy {

    /**
     * Single order picking - one order at a time
     * Simple, good for small orders or priority orders
     */
    SINGLE,

    /**
     * Batch picking - multiple orders with same SKU picked together
     * Efficient for orders with common items
     */
    BATCH,

    /**
     * Zone picking - all items in a zone picked by one worker
     * Good for large warehouses with zone specialization
     */
    ZONE,

    /**
     * Wave picking - orders grouped into waves and picked together
     * Balances workload across warehouse
     */
    WAVE,

    /**
     * Cluster picking - multiple orders picked to same cart
     * Put wall required for order consolidation
     */
    CLUSTER;

    public boolean requiresPutWall() {
        return this == CLUSTER || this == BATCH;
    }

    public boolean supportsMultipleOrders() {
        return this == BATCH || this == CLUSTER || this == WAVE;
    }

    public int getMaxOrdersPerSession() {
        return switch (this) {
            case SINGLE -> 1;
            case BATCH -> 10;
            case ZONE -> 20;
            case WAVE -> 50;
            case CLUSTER -> 8; // Limited by cart capacity
        };
    }
}
