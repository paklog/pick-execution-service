package com.paklog.wes.pick.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.wes.pick.domain.valueobject.Priority;
import com.paklog.wes.pick.integration.contracts.TaskAssignedContract;
import com.paklog.wes.pick.integration.contracts.TaskCreatedContract;
import io.cloudevents.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumer for task events from task-execution-service
 * Implements Anti-Corruption Layer pattern
 */
@Service
public class TaskEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TaskEventConsumer.class);

    private final ObjectMapper objectMapper;

    public TaskEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "task-events", groupId = "pick-execution-service")
    public void handleTaskEvent(CloudEvent cloudEvent) {
        String eventType = cloudEvent.getType();
        log.info("Received event: type={}, id={}", eventType, cloudEvent.getId());

        try {
            if (TaskCreatedContract.EVENT_TYPE.equals(eventType)) {
                handleTaskCreated(cloudEvent);
            } else if (TaskAssignedContract.EVENT_TYPE.equals(eventType)) {
                handleTaskAssigned(cloudEvent);
            } else {
                log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to handle task event: type={}, id={}", eventType, cloudEvent.getId(), e);
            throw new RuntimeException("Failed to handle task event", e);
        }
    }

    private void handleTaskCreated(CloudEvent cloudEvent) throws Exception {
        TaskCreatedContract contract = objectMapper.readValue(
            cloudEvent.getData().toBytes(),
            TaskCreatedContract.class
        );

        // Only process PICK tasks
        if (!contract.isPickTask()) {
            log.debug("Ignoring non-PICK task: taskId={}, taskType={}",
                contract.taskId(), contract.taskType());
            return;
        }

        log.info("Creating pick task: taskId={}, orderId={}, priority={}",
            contract.taskId(), contract.orderId(), contract.priority());

        Priority priority = mapPriority(contract.priority());
        // TODO: Invoke use case to create pick task
        // createPickTaskUseCase.execute(contract.taskId(), contract.waveId(), contract.orderId(), priority, contract.zoneId());
    }

    private void handleTaskAssigned(CloudEvent cloudEvent) throws Exception {
        TaskAssignedContract contract = objectMapper.readValue(
            cloudEvent.getData().toBytes(),
            TaskAssignedContract.class
        );

        log.info("Assigning pick task: taskId={}, workerId={}",
            contract.taskId(), contract.workerId());

        // TODO: Invoke use case to assign pick task
        // assignPickTaskUseCase.execute(contract.taskId(), contract.workerId());
    }

    private Priority mapPriority(String externalPriority) {
        return Priority.fromString(externalPriority);
    }
}
