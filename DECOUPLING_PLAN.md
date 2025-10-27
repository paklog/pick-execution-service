# Pick Execution Service - Decoupling Plan
## Eliminate Dependencies on Shared Modules

**Service:** pick-execution-service
**Complexity:** LOW (only domain primitives)
**Estimated Effort:** 3 hours
**Priority:** Phase 3 (after task-execution-service)

---

## Current Dependencies

### Shared Modules Used
- ✓ **paklog-domain** (v0.0.1-SNAPSHOT)
  - `com.paklog.domain.annotation.AggregateRoot`
  - `com.paklog.domain.shared.DomainEvent`
  - `com.paklog.domain.valueobject.Priority`

### Coupling Impact
- Cannot deploy independently
- Breaking changes in shared modules affect this service
- Build requires paklog-domain artifact
- Testing requires paklog-domain dependency

---

## Target Architecture

### Service-Owned Components
```
pick-execution-service/
├── src/main/java/com/paklog/pick/execution/
│   ├── domain/
│   │   ├── shared/
│   │   │   ├── AggregateRoot.java        # Copy from paklog-domain
│   │   │   └── DomainEvent.java          # Copy from paklog-domain
│   │   │
│   │   └── valueobject/
│   │       └── Priority.java             # Copy from paklog-domain
│   │
│   ├── events/                            # Publisher-owned schemas
│   │   ├── PickingStartedEvent.java
│   │   ├── PickingCompletedEvent.java
│   │   ├── PickingExceptionEvent.java
│   │   └── PickPathOptimizedEvent.java
│   │
│   ├── integration/
│   │   └── contracts/                     # Consumer contracts
│   │       ├── TaskCreatedContract.java   # From task-execution
│   │       └── TaskAssignedContract.java  # From task-execution
│   │
│   └── infrastructure/
│       └── events/
│           ├── PickEventPublisher.java
│           └── TaskEventConsumer.java
```

---

## CloudEvents Schema Definition

### Event Type Pattern
All events MUST follow: `com.paklog.wes.pick-execution.picking.<entity>.<action>`

### Events Published by Pick Execution Service

#### 1. Picking Started Event
**Type:** `com.paklog.wes.pick-execution.picking.pick-task.started.v1`
**Trigger:** Worker starts picking items
**Schema:**
```json
{
  "pick_task_id": "PICK-12345",
  "task_id": "TASK-001",
  "wave_id": "WAVE-001",
  "worker_id": "WORKER-456",
  "location_sequence": ["LOC-A1-01", "LOC-A1-05", "LOC-A1-12"],
  "total_items": 15,
  "started_at": "2025-10-26T10:00:00Z",
  "optimized_path_distance_meters": 85.5
}
```

#### 2. Picking Completed Event
**Type:** `com.paklog.wes.pick-execution.picking.pick-task.completed.v1`
**Trigger:** All items picked successfully
**Schema:**
```json
{
  "pick_task_id": "PICK-12345",
  "task_id": "TASK-001",
  "wave_id": "WAVE-001",
  "worker_id": "WORKER-456",
  "completed_at": "2025-10-26T10:15:00Z",
  "duration_seconds": 900,
  "items_picked": 15,
  "locations_visited": 8,
  "actual_distance_meters": 87.2,
  "pick_rate_items_per_hour": 60
}
```

#### 3. Picking Exception Event
**Type:** `com.paklog.wes.pick-execution.picking.pick-task.exception.v1`
**Trigger:** Exception occurs during picking (short pick, location empty, etc.)
**Schema:**
```json
{
  "pick_task_id": "PICK-12345",
  "task_id": "TASK-001",
  "order_id": "ORD-123",
  "sku_code": "SKU-789",
  "location_id": "LOC-A1-05",
  "exception_type": "SHORT_PICK|LOCATION_EMPTY|DAMAGED_ITEM|WRONG_ITEM",
  "requested_quantity": 5,
  "actual_quantity": 3,
  "reported_at": "2025-10-26T10:08:00Z",
  "reported_by": "WORKER-456"
}
```

#### 4. Pick Path Optimized Event
**Type:** `com.paklog.wes.pick-execution.picking.pick-path.optimized.v1`
**Trigger:** Pick path is optimized using TSP algorithm
**Schema:**
```json
{
  "pick_task_id": "PICK-12345",
  "wave_id": "WAVE-001",
  "original_distance_meters": 120.5,
  "optimized_distance_meters": 85.5,
  "distance_saved_meters": 35.0,
  "improvement_percentage": 29.05,
  "algorithm_used": "TSP_2OPT",
  "optimization_time_ms": 45,
  "optimized_at": "2025-10-26T09:59:55Z"
}
```

### Events Consumed by Pick Execution Service

#### Task Created Event (from task-execution-service)
**Type:** `com.paklog.wes.task-execution.task.task.created.v1`
**Purpose:** Creates pick task when task is created
**Contract:**
```json
{
  "task_id": "TASK-001",
  "wave_id": "WAVE-001",
  "order_id": "ORD-123",
  "task_type": "PICK",
  "priority": "HIGH",
  "zone_id": "ZONE-A1",
  "created_at": "2025-10-26T10:00:00Z"
}
```

#### Task Assigned Event (from task-execution-service)
**Type:** `com.paklog.wes.task-execution.task.task.assigned.v1`
**Purpose:** Assigns pick task to worker
**Contract:**
```json
{
  "task_id": "TASK-001",
  "worker_id": "WORKER-456",
  "assigned_at": "2025-10-26T10:01:00Z",
  "priority": "HIGH",
  "zone_id": "ZONE-A1"
}
```

---

## Step-by-Step Migration Tasks

### Phase 1: Preparation (15 minutes)

#### Task 1.1: Create Feature Branch
```bash
cd pick-execution-service
git checkout -b decouple/remove-shared-dependencies
```

#### Task 1.2: Run Baseline Tests
```bash
mvn clean test
# Document current test results and coverage
```

#### Task 1.3: Create Service-Internal Packages
```bash
mkdir -p src/main/java/com/paklog/pick/execution/domain/shared
mkdir -p src/main/java/com/paklog/pick/execution/domain/valueobject
mkdir -p src/main/java/com/paklog/pick/execution/events
mkdir -p src/main/java/com/paklog/pick/execution/integration/contracts
mkdir -p src/main/java/com/paklog/pick/execution/infrastructure/events
```

---

### Phase 2: Internalize Domain Primitives (1 hour)

#### Task 2.1: Copy AggregateRoot Annotation
```bash
# Copy file
cp ../../paklog-domain/src/main/java/com/paklog/domain/annotation/AggregateRoot.java \
   src/main/java/com/paklog/pick/execution/domain/shared/AggregateRoot.java
```

**Update package declaration:**
```java
// File: src/main/java/com/paklog/pick/execution/domain/shared/AggregateRoot.java
package com.paklog.pick.execution.domain.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for aggregate roots in Pick Execution bounded context
 * Copied from paklog-domain to eliminate compilation dependency
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AggregateRoot {
}
```

#### Task 2.2: Copy DomainEvent Interface
```bash
# Copy file
cp ../../paklog-domain/src/main/java/com/paklog/domain/shared/DomainEvent.java \
   src/main/java/com/paklog/pick/execution/domain/shared/DomainEvent.java
```

**Update package declaration:**
```java
// File: src/main/java/com/paklog/pick/execution/domain/shared/DomainEvent.java
package com.paklog.pick.execution.domain.shared;

import java.time.Instant;

/**
 * Base interface for all domain events in Pick Execution bounded context
 * Copied from paklog-domain to eliminate compilation dependency
 */
public interface DomainEvent {

    /**
     * When the event occurred
     */
    Instant occurredOn();

    /**
     * Type of the event
     */
    String eventType();
}
```

#### Task 2.3: Copy Priority Value Object
```bash
# Copy file
cp ../../paklog-domain/src/main/java/com/paklog/domain/valueobject/Priority.java \
   src/main/java/com/paklog/pick/execution/domain/valueobject/Priority.java
```

**Update package declaration:**
```java
// File: src/main/java/com/paklog/pick/execution/domain/valueobject/Priority.java
package com.paklog.pick.execution.domain.valueobject;

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
```

#### Task 2.4: Update All Imports
```bash
# Find and replace imports across the codebase
find src/main/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.annotation\.AggregateRoot/import com.paklog.pick.execution.domain.shared.AggregateRoot/g' {} +

find src/main/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.shared\.DomainEvent/import com.paklog.pick.execution.domain.shared.DomainEvent/g' {} +

find src/main/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.valueobject\.Priority/import com.paklog.pick.execution.domain.valueobject.Priority/g' {} +

# Also update test files
find src/test/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.annotation\.AggregateRoot/import com.paklog.pick.execution.domain.shared.AggregateRoot/g' {} +

find src/test/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.shared\.DomainEvent/import com.paklog.pick.execution.domain.shared.DomainEvent/g' {} +

find src/test/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.valueobject\.Priority/import com.paklog.pick.execution.domain.valueobject.Priority/g' {} +
```

#### Task 2.5: Remove paklog-domain Dependency
**Edit pom.xml:**
```xml
<!-- DELETE THIS DEPENDENCY -->
<!--
<dependency>
    <groupId>com.paklog.common</groupId>
    <artifactId>paklog-domain</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
-->
```

#### Task 2.6: Verify Compilation
```bash
mvn clean compile
# Should succeed without paklog-domain dependency
```

---

### Phase 3: Define Event Schemas (1.5 hours)

#### Task 3.1: Create PickingStartedEvent

**File:** `src/main/java/com/paklog/pick/execution/events/PickingStartedEvent.java`

```java
package com.paklog.pick.execution.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * Event published when worker starts picking items
 * CloudEvent Type: com.paklog.wes.pick-execution.picking.pick-task.started.v1
 */
public record PickingStartedEvent(
    @JsonProperty("pick_task_id") String pickTaskId,
    @JsonProperty("task_id") String taskId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("location_sequence") List<String> locationSequence,
    @JsonProperty("total_items") int totalItems,
    @JsonProperty("started_at") Instant startedAt,
    @JsonProperty("optimized_path_distance_meters") double optimizedPathDistanceMeters
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pick-execution.picking.pick-task.started.v1";
}
```

#### Task 3.2: Create PickingCompletedEvent

**File:** `src/main/java/com/paklog/pick/execution/events/PickingCompletedEvent.java`

```java
package com.paklog.pick.execution.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Event published when all items picked successfully
 * CloudEvent Type: com.paklog.wes.pick-execution.picking.pick-task.completed.v1
 */
public record PickingCompletedEvent(
    @JsonProperty("pick_task_id") String pickTaskId,
    @JsonProperty("task_id") String taskId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("completed_at") Instant completedAt,
    @JsonProperty("duration_seconds") long durationSeconds,
    @JsonProperty("items_picked") int itemsPicked,
    @JsonProperty("locations_visited") int locationsVisited,
    @JsonProperty("actual_distance_meters") double actualDistanceMeters,
    @JsonProperty("pick_rate_items_per_hour") int pickRateItemsPerHour
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pick-execution.picking.pick-task.completed.v1";
}
```

#### Task 3.3: Create PickingExceptionEvent

**File:** `src/main/java/com/paklog/pick/execution/events/PickingExceptionEvent.java`

```java
package com.paklog.pick.execution.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Event published when exception occurs during picking
 * CloudEvent Type: com.paklog.wes.pick-execution.picking.pick-task.exception.v1
 */
public record PickingExceptionEvent(
    @JsonProperty("pick_task_id") String pickTaskId,
    @JsonProperty("task_id") String taskId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("sku_code") String skuCode,
    @JsonProperty("location_id") String locationId,
    @JsonProperty("exception_type") String exceptionType,
    @JsonProperty("requested_quantity") int requestedQuantity,
    @JsonProperty("actual_quantity") int actualQuantity,
    @JsonProperty("reported_at") Instant reportedAt,
    @JsonProperty("reported_by") String reportedBy
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pick-execution.picking.pick-task.exception.v1";
}
```

#### Task 3.4: Create PickPathOptimizedEvent

**File:** `src/main/java/com/paklog/pick/execution/events/PickPathOptimizedEvent.java`

```java
package com.paklog.pick.execution.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Event published when pick path is optimized using TSP algorithm
 * CloudEvent Type: com.paklog.wes.pick-execution.picking.pick-path.optimized.v1
 */
public record PickPathOptimizedEvent(
    @JsonProperty("pick_task_id") String pickTaskId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("original_distance_meters") double originalDistanceMeters,
    @JsonProperty("optimized_distance_meters") double optimizedDistanceMeters,
    @JsonProperty("distance_saved_meters") double distanceSavedMeters,
    @JsonProperty("improvement_percentage") double improvementPercentage,
    @JsonProperty("algorithm_used") String algorithmUsed,
    @JsonProperty("optimization_time_ms") long optimizationTimeMs,
    @JsonProperty("optimized_at") Instant optimizedAt
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pick-execution.picking.pick-path.optimized.v1";
}
```

#### Task 3.5: Create Task Event Contracts (Anti-Corruption Layer)

**File:** `src/main/java/com/paklog/pick/execution/integration/contracts/TaskCreatedContract.java`

```java
package com.paklog.pick.execution.integration.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Contract for TaskCreatedEvent from task-execution-service
 * Anti-Corruption Layer for external events
 */
public record TaskCreatedContract(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("task_type") String taskType,
    @JsonProperty("priority") String priority,
    @JsonProperty("zone_id") String zoneId,
    @JsonProperty("created_at") Instant createdAt
) {
    public static final String EVENT_TYPE = "com.paklog.wes.task-execution.task.task.created.v1";

    /**
     * Check if this is a picking task
     */
    public boolean isPickTask() {
        return "PICK".equalsIgnoreCase(taskType);
    }
}
```

**File:** `src/main/java/com/paklog/pick/execution/integration/contracts/TaskAssignedContract.java`

```java
package com.paklog.pick.execution.integration.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Contract for TaskAssignedEvent from task-execution-service
 * Anti-Corruption Layer for external events
 */
public record TaskAssignedContract(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("assigned_at") Instant assignedAt,
    @JsonProperty("priority") String priority,
    @JsonProperty("zone_id") String zoneId
) {
    public static final String EVENT_TYPE = "com.paklog.wes.task-execution.task.task.assigned.v1";
}
```

#### Task 3.6: Create CloudEvents Publisher

**File:** `src/main/java/com/paklog/pick/execution/infrastructure/events/PickEventPublisher.java`

```java
package com.paklog.pick.execution.infrastructure.events;

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
```

#### Task 3.7: Create Task Event Consumer

**File:** `src/main/java/com/paklog/pick/execution/infrastructure/events/TaskEventConsumer.java`

```java
package com.paklog.pick.execution.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.pick.execution.application.usecases.CreatePickTaskUseCase;
import com.paklog.pick.execution.application.usecases.AssignPickTaskUseCase;
import com.paklog.pick.execution.domain.valueobject.Priority;
import com.paklog.pick.execution.integration.contracts.TaskAssignedContract;
import com.paklog.pick.execution.integration.contracts.TaskCreatedContract;
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

    private final CreatePickTaskUseCase createPickTaskUseCase;
    private final AssignPickTaskUseCase assignPickTaskUseCase;
    private final ObjectMapper objectMapper;

    public TaskEventConsumer(
            CreatePickTaskUseCase createPickTaskUseCase,
            AssignPickTaskUseCase assignPickTaskUseCase,
            ObjectMapper objectMapper) {
        this.createPickTaskUseCase = createPickTaskUseCase;
        this.assignPickTaskUseCase = assignPickTaskUseCase;
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
            throw e;
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
        createPickTaskUseCase.execute(
            contract.taskId(),
            contract.waveId(),
            contract.orderId(),
            priority,
            contract.zoneId()
        );
    }

    private void handleTaskAssigned(CloudEvent cloudEvent) throws Exception {
        TaskAssignedContract contract = objectMapper.readValue(
            cloudEvent.getData().toBytes(),
            TaskAssignedContract.class
        );

        log.info("Assigning pick task: taskId={}, workerId={}",
            contract.taskId(), contract.workerId());

        assignPickTaskUseCase.execute(
            contract.taskId(),
            contract.workerId()
        );
    }

    private Priority mapPriority(String externalPriority) {
        return Priority.fromString(externalPriority);
    }
}
```

---

### Phase 4: Testing & Validation (30 minutes)

#### Task 4.1: Run Unit Tests
```bash
mvn test
```

#### Task 4.2: Run Full Test Suite
```bash
mvn clean verify
```

#### Task 4.3: Build Service Independently
```bash
mvn clean package -DskipTests
```

#### Task 4.4: Run Service Locally
```bash
mvn spring-boot:run
```

---

## Validation Checklist

- [ ] No compilation errors after removing paklog-domain
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Code coverage maintained (≥80%)
- [ ] Service builds independently
- [ ] CloudEvents published with correct types
- [ ] Task events consumed successfully
- [ ] TSP path optimization working
- [ ] No references to com.paklog.domain.* packages

---

## Rollback Plan

```bash
git checkout main
git branch -D decouple/remove-shared-dependencies
```

---

## Success Criteria

- ✅ Zero dependencies on paklog-domain
- ✅ Service builds independently
- ✅ All tests passing
- ✅ Events published correctly
- ✅ Production deployment successful

---

**Estimated Total Time:** 3 hours
**Complexity:** LOW
**Risk Level:** LOW
