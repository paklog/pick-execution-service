# Pick Execution Service - Implementation Plan

**Service**: Pick Execution Service (WES)
**Port**: 8082
**Started**: 2025-10-18
**Status**: 🚧 In Progress

---

## Service Overview

The **Pick Execution Service** manages the execution of picking operations in the warehouse. It receives pick tasks from the Task Execution Service and guides workers through the picking process with optimized paths, real-time validation, and put wall management.

### Key Responsibilities
- Execute pick sessions with optimized paths
- Manage pick carts and containers
- Handle batch, zone, wave, and cluster picking strategies
- Real-time pick validation and short pick handling
- Put wall operations and order consolidation
- Pick accuracy tracking and performance metrics

---

## Architecture Pattern

Following **Hexagonal Architecture** (Ports & Adapters) with **Domain-Driven Design**:

```
┌─────────────────────────────────────────────────────────┐
│                     ADAPTERS (Input)                     │
│  REST API │ Mobile API │ Kafka Consumers │ WebSocket    │
├─────────────────────────────────────────────────────────┤
│                   APPLICATION LAYER                      │
│     Commands │ Queries │ Services │ Handlers            │
├─────────────────────────────────────────────────────────┤
│                     DOMAIN LAYER                         │
│  Aggregates │ Entities │ Value Objects │ Domain Events  │
│  PickSession │ PutWall │ PickStrategy │ PathOptimizer   │
├─────────────────────────────────────────────────────────┤
│                  ADAPTERS (Output)                       │
│  MongoDB │ Kafka Producers │ REST Clients               │
└─────────────────────────────────────────────────────────┘
```

---

## Domain Model

### Aggregates

#### 1. PickSession (Aggregate Root)
**Purpose**: Manages a complete picking session for a worker

**State**:
```java
- sessionId: String
- taskId: String (from Task Execution Service)
- workerId: String
- warehouseId: String
- strategy: PickStrategy (SINGLE, BATCH, ZONE, WAVE, CLUSTER)
- status: SessionStatus (STARTED, IN_PROGRESS, PAUSED, COMPLETED, CANCELLED)
- cartId: String
- pickInstructions: List<PickInstruction>
- optimizedPath: PickPath
- startedAt: LocalDateTime
- completedAt: LocalDateTime
- totalItems: int
- pickedItems: int
- shortPicks: int
- accuracy: double
```

**Invariants**:
- Session must have at least one pick instruction
- Cannot complete session with pending picks
- Cannot start session without valid cart
- Accuracy must be 0-100%

**Behavior**:
```java
+ startSession(String workerId, String cartId)
+ confirmPick(String instructionId, int quantity, String location)
+ handleShortPick(String instructionId, int pickedQuantity, String reason)
+ pauseSession()
+ resumeSession()
+ completeSession()
+ cancelSession(String reason)
+ getCurrentInstruction(): PickInstruction
+ getNextInstruction(): PickInstruction
+ calculateProgress(): ProgressInfo
```

---

#### 2. PutWall (Aggregate Root)
**Purpose**: Manages put wall operations for order consolidation

**State**:
```java
- putWallId: String
- warehouseId: String
- zone: String
- slots: List<PutWallSlot>
- totalSlots: int
- occupiedSlots: int
- status: PutWallStatus (ACTIVE, FULL, MAINTENANCE, INACTIVE)
- configuration: PutWallConfiguration
```

**Behavior**:
```java
+ assignOrderToSlot(String orderId): PutWallSlot
+ putItemToSlot(String slotId, String itemId, int quantity)
+ completeOrder(String orderId)
+ clearSlot(String slotId)
+ getAvailableSlots(): List<PutWallSlot>
+ isOrderComplete(String orderId): boolean
```

---

### Entities

#### 1. PickInstruction
**Purpose**: Individual item to be picked

**State**:
```java
- instructionId: String
- itemSku: String
- itemDescription: String
- quantity: int
- pickedQuantity: int
- location: Location
- orderId: String (for put wall)
- status: InstructionStatus (PENDING, PICKED, SHORT_PICKED, SKIPPED)
- sequenceNumber: int (for optimized path)
- priority: Priority
- weight: double
- dimensions: Dimensions
- specialHandling: List<String>
```

---

#### 2. PutWallSlot
**Purpose**: Single slot in put wall

**State**:
```java
- slotId: String
- slotNumber: int
- orderId: String
- status: SlotStatus (EMPTY, ASSIGNED, FILLING, COMPLETE)
- items: List<SlotItem>
- expectedItems: int
- actualItems: int
- lightIndicator: LightStatus
```

---

### Value Objects

#### 1. PickStrategy
```java
enum PickStrategy {
    SINGLE,    // One order at a time
    BATCH,     // Multiple orders, same SKU
    ZONE,      // All items in a zone
    WAVE,      // Wave-based picking
    CLUSTER    // Multiple orders to cart
}
```

#### 2. SessionStatus
```java
enum SessionStatus {
    STARTED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    CANCELLED,
    FAILED
}
```

#### 3. PickPath
```java
record PickPath(
    List<PathNode> nodes,
    double totalDistance,
    Duration estimatedDuration,
    String algorithm
) {
    // Optimized sequence of locations to visit
}
```

#### 4. Location (Reuse from paklog-domain)
```java
record Location(
    String aisle,
    String bay,
    String level,
    String position
) {
    public double distanceFrom(Location other);
    public String toDisplayString();
}
```

---

### Domain Events

```java
- PickSessionStartedEvent(sessionId, workerId, taskId, warehouseId, strategy)
- PickConfirmedEvent(sessionId, instructionId, quantity, location, timestamp)
- ShortPickEvent(sessionId, instructionId, expectedQty, actualQty, reason)
- PickSessionPausedEvent(sessionId, reason, timestamp)
- PickSessionCompletedEvent(sessionId, workerId, totalPicks, accuracy, duration)
- PutWallOrderCompleteEvent(putWallId, orderId, slotId, itemCount)
```

---

## Application Layer

### Commands

```java
// Pick Session Commands
+ StartPickSessionCommand(taskId, workerId, cartId, strategy)
+ ConfirmPickCommand(sessionId, instructionId, quantity, location)
+ HandleShortPickCommand(sessionId, instructionId, actualQuantity, reason)
+ PauseSessionCommand(sessionId, reason)
+ ResumeSessionCommand(sessionId)
+ CompleteSessionCommand(sessionId)

// Put Wall Commands
+ AssignOrderToSlotCommand(putWallId, orderId)
+ PutItemCommand(putWallId, slotId, itemId, quantity)
+ CompleteOrderCommand(putWallId, orderId)
```

### Services

```java
+ PickSessionService
  - createSession(command): PickSession
  - confirmPick(command): PickSession
  - getCurrentInstruction(sessionId): PickInstruction
  - getActiveSessionForWorker(workerId): PickSession

+ PathOptimizationService
  - optimizePath(instructions, currentLocation): PickPath
  - calculateDistance(from, to): double
  - estimateDuration(path): Duration

+ PutWallService
  - assignOrderToSlot(command): PutWallSlot
  - putItem(command): PutWallSlot
  - getOrderStatus(putWallId, orderId): OrderStatus
```

---

## REST API Endpoints

### Pick Management API (`/api/v1/picks`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/sessions` | Start pick session |
| GET | `/sessions/{id}` | Get session details |
| POST | `/sessions/{id}/confirm` | Confirm pick |
| POST | `/sessions/{id}/short-pick` | Handle short pick |
| POST | `/sessions/{id}/pause` | Pause session |
| POST | `/sessions/{id}/resume` | Resume session |
| POST | `/sessions/{id}/complete` | Complete session |
| GET | `/sessions/active` | Get active sessions |
| GET | `/sessions/{id}/progress` | Get session progress |

### Mobile Pick API (`/api/v1/mobile/picks`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/my-session` | Get worker's active session |
| GET | `/current-instruction` | Get next pick instruction |
| POST | `/confirm` | Confirm pick (simplified) |
| POST | `/short-pick` | Report short pick |
| POST | `/skip` | Skip instruction |
| GET | `/progress` | Get session progress |

### Put Wall API (`/api/v1/putwalls`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/{id}/assign` | Assign order to slot |
| POST | `/{id}/put` | Put item in slot |
| POST | `/{id}/complete` | Complete order |
| GET | `/{id}/status` | Get put wall status |
| GET | `/{id}/slots` | Get available slots |

---

## Integration with Task Execution Service

### Event Consumption

**TaskAssignedEvent** → Start Pick Session
```json
{
  "taskId": "TASK-001",
  "type": "PICK",
  "workerId": "WORKER-001",
  "warehouseId": "WH-001",
  "context": {
    "waveId": "WAVE-001",
    "orderId": "ORDER-001",
    "instructions": [...]
  }
}
```

**Flow**:
1. Consume `TaskAssignedEvent` from task-execution-service
2. Extract pick instructions from task context
3. Optimize pick path
4. Create `PickSession`
5. Notify worker via WebSocket

---

## Path Optimization Algorithms

### 1. S-Shape Algorithm (Default)
- Traverse aisles in S-shape pattern
- Minimize backtracking
- Simple and fast

### 2. Nearest Neighbor (TSP Heuristic)
- Always pick nearest unvisited location
- Good for sparse picks
- O(n²) complexity

### 3. 2-Opt Optimization
- Improve path by swapping edges
- Better quality paths
- Higher computation cost

**Selection Logic**:
- < 10 picks: Nearest Neighbor
- 10-50 picks: S-Shape
- > 50 picks: S-Shape with zone optimization

---

## Implementation Phases

### Phase 1: Core Domain Model ✅ (Day 1-2)
- [x] PickSession aggregate
- [x] PickInstruction entity
- [x] Value objects (SessionStatus, PickStrategy, PickPath)
- [x] Domain events
- [x] Unit tests

### Phase 2: Path Optimization 🚧 (Day 2-3)
- [ ] PathOptimizationService
- [ ] S-Shape algorithm
- [ ] Nearest neighbor algorithm
- [ ] Distance calculations
- [ ] Unit tests

### Phase 3: Application Services (Day 3-4)
- [ ] PickSessionService
- [ ] Command handlers
- [ ] Query handlers
- [ ] Event publishers

### Phase 4: REST APIs (Day 4-5)
- [ ] PickSessionController
- [ ] MobilePickController
- [ ] DTOs
- [ ] Exception handling

### Phase 5: Task Integration (Day 5-6)
- [ ] TaskAssignedEventHandler
- [ ] REST client for Task Service
- [ ] Event mapping

### Phase 6: Put Wall (Day 6-7)
- [ ] PutWall aggregate
- [ ] PutWallService
- [ ] PutWallController
- [ ] Slot management

### Phase 7: WebSocket & Real-time (Day 7-8)
- [ ] WebSocket configuration
- [ ] Real-time progress updates
- [ ] Worker notifications

### Phase 8: Configuration & Deployment (Day 8-9)
- [ ] application.yml
- [ ] MongoDB indexes
- [ ] Kafka topics
- [ ] Docker configuration

### Phase 9: Testing (Day 9-10)
- [ ] Integration tests
- [ ] End-to-end tests
- [ ] Performance tests

---

## MongoDB Collections

### pick_sessions
```javascript
{
  _id: "SESSION-001",
  taskId: "TASK-001",
  workerId: "WORKER-001",
  warehouseId: "WH-001",
  strategy: "BATCH",
  status: "IN_PROGRESS",
  cartId: "CART-123",
  pickInstructions: [...],
  optimizedPath: {...},
  startedAt: ISODate("2025-10-18T10:00:00Z"),
  metrics: {
    totalItems: 25,
    pickedItems: 15,
    shortPicks: 2,
    accuracy: 92.0
  }
}
```

### Indexes
```javascript
db.pick_sessions.createIndex({ "workerId": 1, "status": 1 })
db.pick_sessions.createIndex({ "taskId": 1 })
db.pick_sessions.createIndex({ "status": 1, "startedAt": -1 })
db.pick_sessions.createIndex({ "warehouseId": 1, "status": 1 })
```

---

## Kafka Topics

### Consumed
- `wes-task-events` - Task assigned/completed events

### Published
- `wes-pick-events` - Pick session events
- `wes-putwall-events` - Put wall events

---

## Performance Targets

| Metric | Target |
|--------|--------|
| Session creation | < 200ms |
| Path optimization (50 picks) | < 500ms |
| Pick confirmation | < 100ms |
| API response time (p95) | < 300ms |
| WebSocket latency | < 50ms |
| Concurrent sessions | 500+ |

---

## Success Criteria

- [x] Domain model complete with tests
- [ ] Path optimization working
- [ ] REST APIs implemented
- [ ] Mobile API functional
- [ ] Task service integration working
- [ ] Put wall operations functional
- [ ] WebSocket real-time updates
- [ ] All unit tests passing (target: 50+ tests)
- [ ] Build successful
- [ ] Documentation complete

---

**Next Step**: Start implementing Phase 1 - Core Domain Model
