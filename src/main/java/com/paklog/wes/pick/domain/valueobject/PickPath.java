package com.paklog.wes.pick.domain.valueobject;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Optimized path through warehouse locations
 * Represents the sequence in which picks should be made
 */
public record PickPath(
        List<PathNode> nodes,
        double totalDistance,
        Duration estimatedDuration,
        String algorithm
) {

    public PickPath {
        Objects.requireNonNull(nodes, "Nodes cannot be null");
        Objects.requireNonNull(algorithm, "Algorithm cannot be null");

        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("Path must have at least one node");
        }
        if (totalDistance < 0) {
            throw new IllegalArgumentException("Total distance cannot be negative");
        }
    }

    /**
     * Get the first node in the path
     */
    public PathNode getFirstNode() {
        return nodes.get(0);
    }

    /**
     * Get node at specific index
     */
    public PathNode getNodeAt(int index) {
        if (index < 0 || index >= nodes.size()) {
            throw new IndexOutOfBoundsException("Invalid node index: " + index);
        }
        return nodes.get(index);
    }

    /**
     * Get total number of picks
     */
    public int getTotalPicks() {
        return nodes.size();
    }

    /**
     * Check if path is optimized (vs sequential)
     */
    public boolean isOptimized() {
        return !algorithm.equals("SEQUENTIAL");
    }

    /**
     * Calculate progress percentage
     */
    public double calculateProgress(int currentNodeIndex) {
        if (nodes.isEmpty()) {
            return 100.0;
        }
        return (currentNodeIndex / (double) nodes.size()) * 100.0;
    }

    /**
     * Represents a single node in the pick path
     */
    public record PathNode(
            String instructionId,
            Location location,
            int sequenceNumber,
            double distanceFromPrevious
    ) {
        public PathNode {
            Objects.requireNonNull(instructionId, "Instruction ID cannot be null");
            Objects.requireNonNull(location, "Location cannot be null");

            if (sequenceNumber < 0) {
                throw new IllegalArgumentException("Sequence number cannot be negative");
            }
            if (distanceFromPrevious < 0) {
                throw new IllegalArgumentException("Distance cannot be negative");
            }
        }
    }
}
