package com.paklog.wes.pick.infrastructure.events;

import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.pick.application.command.StartPickSessionCommand;
import com.paklog.wes.pick.application.service.PickSessionService;
import com.paklog.wes.pick.domain.aggregate.PickSession;
import com.paklog.wes.pick.domain.entity.PickInstruction;
import com.paklog.wes.pick.domain.valueobject.Location;
import com.paklog.wes.pick.domain.valueobject.PickStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Event handler for Task events
 * Listens to task-related events and creates pick sessions
 */
@Component
public class TaskEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(TaskEventHandler.class);

    private final PickSessionService pickSessionService;

    public TaskEventHandler(PickSessionService pickSessionService) {
        this.pickSessionService = pickSessionService;
    }

    /**
     * Handle TaskCreatedEvent from task-execution-service
     * Creates pick sessions for PICK tasks
     */
    @KafkaListener(
            topics = "${paklog.kafka.topics.task-events:wes-task-events}",
            groupId = "${paklog.kafka.consumer.group-id:pick-execution-service}"
    )
    public void handleTaskCreated(Map<String, Object> eventData) {
        try {
            String eventType = (String) eventData.get("type");

            if (!"TaskCreatedEvent".equals(eventType)) {
                return; // Ignore other event types
            }

            String taskType = (String) eventData.get("taskType");
            if (!"PICK".equals(taskType)) {
                return; // Only handle PICK tasks
            }

            logger.info("Received TaskCreatedEvent for PICK task: {}", eventData);

            String taskId = (String) eventData.get("taskId");
            String warehouseId = (String) eventData.get("warehouseId");
            String referenceId = (String) eventData.get("referenceId"); // Wave ID

            @SuppressWarnings("unchecked")
            Map<String, Object> context = (Map<String, Object>) eventData.get("context");

            // Create pick session for this task
            createPickSession(taskId, warehouseId, referenceId, context);

            logger.info("Created pick session for task {}", taskId);

        } catch (Exception e) {
            logger.error("Error handling TaskCreatedEvent", e);
            // In production, publish to dead letter queue
        }
    }

    private void createPickSession(String taskId,
                                   String warehouseId,
                                   String waveId,
                                   Map<String, Object> context) {
        if (context == null) {
            logger.warn("Ignoring task {} creation event - missing context payload", taskId);
            return;
        }

        try {
            PickStrategy strategy = parseStrategy(context.get("strategy"));
            List<PickInstruction> instructions = extractPickInstructions(context);

            if (instructions.isEmpty()) {
                logger.warn("Ignoring task {} creation event - no pick instructions provided", taskId);
                return;
            }

            String workerId = stringValue(context.get("workerId"), "UNASSIGNED");
            String cartId = stringValue(context.get("cartId"), waveId != null ? waveId : "SYSTEM-CART");

            StartPickSessionCommand command = new StartPickSessionCommand(
                    taskId,
                    workerId,
                    warehouseId,
                    strategy,
                    cartId,
                    instructions
            );

            PickSession session = pickSessionService.createSession(command);

            logger.debug("Created pick session {} for task {} using strategy {}",
                    session.getSessionId(), taskId, strategy);

        } catch (Exception e) {
            logger.error("Error creating pick session for task {}", taskId, e);
        }
    }

    /**
     * Handle TaskAssignedEvent from task-execution-service
     * Notifies picker of new assignment
     */
    @KafkaListener(
            topics = "${paklog.kafka.topics.task-events:wes-task-events}",
            groupId = "${paklog.kafka.consumer.group-id:pick-execution-service}"
    )
    public void handleTaskAssigned(Map<String, Object> eventData) {
        try {
            String eventType = (String) eventData.get("type");

            if (!"TaskAssignedEvent".equals(eventType)) {
                return;
            }

            String taskType = (String) eventData.get("taskType");
            if (!"PICK".equals(taskType)) {
                return;
            }

            logger.info("Received TaskAssignedEvent for PICK task: {}", eventData);

            String taskId = (String) eventData.get("taskId");
            String assignedTo = (String) eventData.get("assignedTo");

            if (assignedTo == null || assignedTo.isBlank()) {
                logger.warn("Ignoring TaskAssignedEvent for task {} - missing assigned picker", taskId);
                return;
            }

            boolean sessionExists = pickSessionService.getActiveSessions().stream()
                    .anyMatch(session -> Objects.equals(session.getTaskId(), taskId));

            if (sessionExists) {
                logger.info("Picker {} assigned to pick session for task {}", assignedTo, taskId);
            } else {
                logger.warn("No active pick session found for task {} when assigning picker {}", taskId, assignedTo);
            }

        } catch (Exception e) {
            logger.error("Error handling TaskAssignedEvent", e);
        }
    }

    private PickStrategy parseStrategy(Object rawStrategy) {
        if (rawStrategy instanceof PickStrategy strategy) {
            return strategy;
        }

        if (rawStrategy instanceof String strategyName && !strategyName.isBlank()) {
            try {
                return PickStrategy.valueOf(strategyName.toUpperCase());
            } catch (IllegalArgumentException ex) {
                logger.warn("Unknown pick strategy '{}', falling back to SINGLE", strategyName);
            }
        }

        return PickStrategy.SINGLE;
    }

    @SuppressWarnings("unchecked")
    private List<PickInstruction> extractPickInstructions(Map<String, Object> context) {
        Object rawInstructions = context.get("instructions");
        if (!(rawInstructions instanceof Iterable<?> iterable)) {
            return List.of();
        }

        List<PickInstruction> instructions = new ArrayList<>();
        for (Object raw : iterable) {
            if (!(raw instanceof Map<?, ?> instructionMap)) {
                continue;
            }

            String instructionId = stringValue(instructionMap.get("instructionId"), null);
            String sku = stringValue(instructionMap.get("itemSku"), null);
            Number quantityValue = asNumber(instructionMap.get("expectedQuantity"));

            Location location = buildLocation(instructionMap.get("location"));

            if (instructionId == null || sku == null || quantityValue == null || location == null) {
                logger.warn("Skipping instruction due to missing required fields: {}", instructionMap);
                continue;
            }

            int expectedQuantity = quantityValue.intValue();
            if (expectedQuantity <= 0) {
                logger.warn("Skipping instruction {} due to invalid quantity {}", instructionId, expectedQuantity);
                continue;
            }

            String description = stringValue(instructionMap.get("itemDescription"), null);
            String orderId = stringValue(instructionMap.get("orderId"), null);
            Priority priority = parsePriority(instructionMap.get("priority"));

            PickInstruction instruction = new PickInstruction(
                    instructionId,
                    sku,
                    description,
                    expectedQuantity,
                    location,
                    orderId,
                    priority
            );

            instructions.add(instruction);
        }

        return instructions;
    }

    private Priority parsePriority(Object rawPriority) {
        if (rawPriority instanceof Priority priority) {
            return priority;
        }

        if (rawPriority instanceof String priorityName && !priorityName.isBlank()) {
            try {
                return Priority.valueOf(priorityName.toUpperCase());
            } catch (IllegalArgumentException ex) {
                logger.warn("Unknown priority '{}', defaulting to NORMAL", priorityName);
            }
        }

        return Priority.NORMAL;
    }

    @SuppressWarnings("unchecked")
    private Location buildLocation(Object rawLocation) {
        if (!(rawLocation instanceof Map<?, ?> locationMap)) {
            return null;
        }

        String aisle = stringValue(locationMap.get("aisle"), null);
        String bay = stringValue(locationMap.get("bay"), null);
        String level = stringValue(locationMap.get("level"), null);
        String position = stringValue(locationMap.get("position"), null);

        if (aisle == null || bay == null || level == null) {
            return null;
        }

        return new Location(aisle, bay, level, position);
    }

    private Number asNumber(Object value) {
        if (value instanceof Number number) {
            return number;
        }

        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ex) {
                logger.warn("Unable to parse numeric value '{}'", text);
            }
        }

        return null;
    }

    private String stringValue(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = Objects.toString(value, defaultValue);
        return text != null && !text.isBlank() ? text : defaultValue;
    }
}
