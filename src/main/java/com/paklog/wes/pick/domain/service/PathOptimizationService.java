package com.paklog.wes.pick.domain.service;

import com.paklog.wes.pick.domain.entity.PickInstruction;
import com.paklog.wes.pick.domain.valueobject.Location;
import com.paklog.wes.pick.domain.valueobject.PickPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Domain service for optimizing pick paths
 * Implements S-Shape algorithm for warehouse picking
 */
@Service
public class PathOptimizationService {

    private static final Logger logger = LoggerFactory.getLogger(PathOptimizationService.class);

    private static final double AVERAGE_WALKING_SPEED_MPS = 1.4; // meters per second
    private static final double PICK_TIME_SECONDS = 15.0; // Average time to pick one item

    /**
     * Optimize pick path using best algorithm for instruction count
     */
    public PickPath optimizePath(List<PickInstruction> instructions, Location startLocation) {
        if (instructions == null || instructions.isEmpty()) {
            throw new IllegalArgumentException("Instructions cannot be empty");
        }

        logger.debug("Optimizing path for {} instructions", instructions.size());

        // Choose algorithm based on instruction count
        if (instructions.size() <= 10) {
            return optimizeWithNearestNeighbor(instructions, startLocation);
        } else {
            return optimizeWithSShape(instructions, startLocation);
        }
    }

    /**
     * S-Shape algorithm - traverse aisles in S pattern
     * Most efficient for larger pick lists
     */
    public PickPath optimizeWithSShape(List<PickInstruction> instructions, Location startLocation) {
        logger.debug("Using S-Shape algorithm for {} instructions", instructions.size());

        // Group instructions by aisle
        Map<String, List<PickInstruction>> instructionsByAisle = instructions.stream()
                .collect(Collectors.groupingBy(i -> i.getLocation().aisle()));

        // Sort aisles
        List<String> sortedAisles = new ArrayList<>(instructionsByAisle.keySet());
        Collections.sort(sortedAisles);

        List<PickPath.PathNode> nodes = new ArrayList<>();
        double totalDistance = 0.0;
        int sequenceNumber = 0;
        Location previousLocation = startLocation;
        boolean ascending = true;

        // Traverse aisles in S-shape
        for (String aisle : sortedAisles) {
            List<PickInstruction> aisleInstructions = instructionsByAisle.get(aisle);

            // Sort by bay number (ascending or descending based on S-shape)
            final boolean isAscending = ascending; // Make effectively final for lambda
            aisleInstructions.sort((a, b) -> {
                int bayA = extractBayNumber(a.getLocation());
                int bayB = extractBayNumber(b.getLocation());
                return isAscending ? Integer.compare(bayA, bayB) : Integer.compare(bayB, bayA);
            });

            // Add instructions in order
            for (PickInstruction instruction : aisleInstructions) {
                double distance = previousLocation != null
                        ? previousLocation.distanceFrom(instruction.getLocation())
                        : 0.0;

                nodes.add(new PickPath.PathNode(
                        instruction.getInstructionId(),
                        instruction.getLocation(),
                        sequenceNumber++,
                        distance
                ));

                totalDistance += distance;
                previousLocation = instruction.getLocation();
            }

            // Flip direction for next aisle (S-shape)
            ascending = !ascending;
        }

        Duration estimatedDuration = calculateEstimatedDuration(totalDistance, instructions.size());

        logger.debug("S-Shape path generated: {} nodes, distance: {}, duration: {}",
                nodes.size(), totalDistance, estimatedDuration);

        return new PickPath(nodes, totalDistance, estimatedDuration, "S_SHAPE");
    }

    /**
     * Nearest Neighbor algorithm - always pick closest unpicked item
     * Good for small pick lists
     */
    public PickPath optimizeWithNearestNeighbor(List<PickInstruction> instructions, Location startLocation) {
        logger.debug("Using Nearest Neighbor algorithm for {} instructions", instructions.size());

        List<PickInstruction> remaining = new ArrayList<>(instructions);
        List<PickPath.PathNode> nodes = new ArrayList<>();
        double totalDistance = 0.0;
        int sequenceNumber = 0;
        Location currentLocation = startLocation;

        while (!remaining.isEmpty()) {
            // Find nearest instruction
            PickInstruction nearest = findNearestInstruction(remaining, currentLocation);
            remaining.remove(nearest);

            double distance = currentLocation.distanceFrom(nearest.getLocation());

            nodes.add(new PickPath.PathNode(
                    nearest.getInstructionId(),
                    nearest.getLocation(),
                    sequenceNumber++,
                    distance
            ));

            totalDistance += distance;
            currentLocation = nearest.getLocation();
        }

        Duration estimatedDuration = calculateEstimatedDuration(totalDistance, instructions.size());

        logger.debug("Nearest Neighbor path generated: {} nodes, distance: {}, duration: {}",
                nodes.size(), totalDistance, estimatedDuration);

        return new PickPath(nodes, totalDistance, estimatedDuration, "NEAREST_NEIGHBOR");
    }

    /**
     * Create sequential path without optimization
     * Fallback method if optimization fails
     */
    public PickPath createSequentialPath(List<PickInstruction> instructions) {
        logger.debug("Creating sequential path for {} instructions", instructions.size());

        List<PickPath.PathNode> nodes = new ArrayList<>();
        double totalDistance = 0.0;
        Location previousLocation = null;

        for (int i = 0; i < instructions.size(); i++) {
            PickInstruction instruction = instructions.get(i);
            double distance = previousLocation != null
                    ? previousLocation.distanceFrom(instruction.getLocation())
                    : 0.0;

            nodes.add(new PickPath.PathNode(
                    instruction.getInstructionId(),
                    instruction.getLocation(),
                    i,
                    distance
            ));

            totalDistance += distance;
            previousLocation = instruction.getLocation();
        }

        Duration estimatedDuration = calculateEstimatedDuration(totalDistance, instructions.size());

        return new PickPath(nodes, totalDistance, estimatedDuration, "SEQUENTIAL");
    }

    // Helper methods

    private PickInstruction findNearestInstruction(List<PickInstruction> instructions, Location currentLocation) {
        return instructions.stream()
                .min(Comparator.comparingDouble(i -> currentLocation.distanceFrom(i.getLocation())))
                .orElseThrow(() -> new IllegalStateException("No instructions available"));
    }

    private int extractBayNumber(Location location) {
        try {
            String bay = location.bay().replaceAll("[^0-9]", "");
            return bay.isEmpty() ? 0 : Integer.parseInt(bay);
        } catch (NumberFormatException e) {
            logger.warn("Could not parse bay number from: {}", location.bay());
            return 0;
        }
    }

    private Duration calculateEstimatedDuration(double totalDistance, int pickCount) {
        // Walking time + pick time
        double walkingTimeSeconds = totalDistance / AVERAGE_WALKING_SPEED_MPS;
        double pickTimeSeconds = pickCount * PICK_TIME_SECONDS;
        long totalSeconds = Math.round(walkingTimeSeconds + pickTimeSeconds);

        return Duration.ofSeconds(totalSeconds);
    }

    /**
     * Calculate distance savings compared to sequential path
     */
    public double calculateSavings(PickPath optimizedPath, List<PickInstruction> instructions) {
        PickPath sequentialPath = createSequentialPath(instructions);
        double sequentialDistance = sequentialPath.totalDistance();
        double optimizedDistance = optimizedPath.totalDistance();

        if (sequentialDistance == 0) {
            return 0.0;
        }

        return ((sequentialDistance - optimizedDistance) / sequentialDistance) * 100.0;
    }
}
