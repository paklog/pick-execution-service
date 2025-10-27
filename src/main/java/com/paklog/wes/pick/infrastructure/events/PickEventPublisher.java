package com.paklog.wes.pick.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Publisher for Pick Execution events using CloudEvents format
 */
@Service
public class PickEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PickEventPublisher.class);
    private static final String SOURCE = "paklog://pick-execution-service";

    private final KafkaTemplate<String, CloudEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PickEventPublisher(KafkaTemplate<String, CloudEvent> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String topic, String key, String eventType, Object eventData) {
        try {
            CloudEvent cloudEvent = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create(SOURCE))
                .withType(eventType)
                .withDataContentType("application/json")
                .withTime(OffsetDateTime.now())
                .withData(objectMapper.writeValueAsBytes(eventData))
                .build();

            kafkaTemplate.send(topic, key, cloudEvent);
            log.info("Event published: type={}, key={}, topic={}", eventType, key, topic);
        } catch (Exception e) {
            log.error("Failed to publish event: type={}, key={}", eventType, key, e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}
