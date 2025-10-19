# Pick Execution Service - Domain Model

## Overview

The Pick Execution Service domain model implements sophisticated warehouse picking operations using Domain-Driven Design. The model centers around the PickSession Aggregate which manages the entire pick workflow, with advanced path optimization through the TSP Solver and Path Optimizer.

## Class Diagram

```mermaid
classDiagram
    class PickSession {
        +String sessionId
        +String warehouseId
        +SessionType type
        +SessionStatus status
        +String operatorId
        +List~String~ orderIds
        +List~PickInstruction~ instructions
        +PickPath optimizedPath
        +LocalDateTime startTime
        +LocalDateTime endTime
        +SessionMetrics metrics
        +Map~String Object~ metadata
        +createSession()
        +assignOperator(operatorId)
        +startPicking()
        +confirmPick(instructionId, quantity)
        +skipLocation(reason)
        +pauseSession()
        +resumeSession()
        +completeSession()
        +optimizePath()
    }

    class SessionType {
        <<enumeration>>
        SINGLE_ORDER
        BATCH_PICK
        ZONE_PICK
        PRIORITY_PICK
        WAVE_PICK
    }

    class SessionStatus {
        <<enumeration>>
        CREATED
        ASSIGNED
        IN_PROGRESS
        PAUSED
        COMPLETED
        CANCELLED
    }

    class PickInstruction {
        +String instructionId
        +String orderId
        +String productId
        +String sku
        +int requiredQuantity
        +int pickedQuantity
        +Location pickLocation
        +String zone
        +PickStatus status
        +LocalDateTime pickTime
        +confirmPick(quantity)
        +skipPick(reason)
    }

    class PickStatus {
        <<enumeration>>
        PENDING
        IN_PROGRESS
        PICKED
        SKIPPED
        SHORT_PICKED
    }

    class Location {
        +String locationId
        +String zone
        +String aisle
        +String bay
        +String level
        +String position
        +Coordinates coordinates
        +LocationType type
        +boolean isActive
    }

    class Coordinates {
        +double x
        +double y
        +double z
        +double distanceTo(other)
        +double manhattanDistance(other)
        +double euclideanDistance(other)
    }

    class PickPath {
        +String pathId
        +List~PickNode~ nodes
        +double totalDistance
        +long estimatedTime
        +PathStrategy strategy
        +LocalDateTime calculatedAt
        +getNextNode()
        +getPreviousNode()
        +getCurrentProgress()
    }

    class PickNode {
        +int sequence
        +Location location
        +List~PickInstruction~ instructions
        +double distanceFromPrevious
        +long estimatedDuration
        +boolean visited
    }

    class PathStrategy {
        <<enumeration>>
        SHORTEST_DISTANCE
        ZONE_OPTIMIZED
        PRIORITY_BASED
        SERPENTINE
        BATCH_OPTIMIZED
    }

    class TSPSolver {
        +int maxIterations
        +double improvementThreshold
        +PickPath solveTSP(locations, startPoint)
        +PickPath solveTSPWithZones(locationsByZone, start)
        +List~PickLocation~ nearestNeighbor(locations, start)
        +List~PickLocation~ improve2Opt(path, start)
        +double calculateTotalDistance(path)
    }

    class PickPathOptimizer {
        -PathOptimizationService pathService
        +PickPath optimizePath(session, startLocation)
        +PickPath optimizeBatchPicking(sessions, maxConcurrent)
        +PickPath optimizeSerpentine(locations)
        +PickPath reoptimizePath(currentPath, failed, currentPos)
    }

    class PathOptimizationService {
        +PickPath optimizePath(instructions, startLocation)
        +PickPath optimizeByZone(instructions, zones)
        +PickPath optimizeByPriority(instructions)
        +Map~String List~ clusterByDensity(locations)
    }

    class SessionMetrics {
        +int totalPicks
        +int completedPicks
        +int skippedPicks
        +double pickAccuracy
        +long totalTime
        +double averagePickTime
        +double totalDistance
        +double efficiency
        +calculateMetrics()
        +updateProgress()
    }

    class PickException {
        +String exceptionId
        +String sessionId
        +String instructionId
        +ExceptionType type
        +String reason
        +LocalDateTime occurredAt
        +String reportedBy
        +Resolution resolution
    }

    class ExceptionType {
        <<enumeration>>
        LOCATION_EMPTY
        INSUFFICIENT_QUANTITY
        WRONG_PRODUCT
        DAMAGED_PRODUCT
        LOCATION_BLOCKED
        SYSTEM_ERROR
    }

    class BatchPickStrategy {
        +int maxOrdersPerBatch
        +double maxDistance
        +boolean allowMixedZones
        +List~PickSession~ createBatches(orders)
        +boolean canCombine(order1, order2)
        +double calculateSavings(batch)
    }

    class ZoneRouter {
        +List~Zone~ zones
        +ZoneSequence determineSequence(picks)
        +double calculateTransitionCost(zone1, zone2)
        +List~PickInstruction~ optimizeWithinZone(picks)
    }

    class DensityClusterer {
        +double maxClusterRadius
        +int minClusterSize
        +List~Cluster~ createClusters(locations)
        +Cluster findNearestCluster(location)
        +void mergeCloseClusters(clusters)
    }

    PickSession "1" *-- "1..*" PickInstruction : contains
    PickSession "1" --> "1" SessionStatus : has
    PickSession "1" --> "1" SessionType : has
    PickSession "1" --> "0..1" PickPath : follows
    PickInstruction "1" --> "1" Location : picks from
    PickInstruction "1" --> "1" PickStatus : has
    Location "1" --> "1" Coordinates : has
    PickPath "1" *-- "1..*" PickNode : contains
    PickPath "1" --> "1" PathStrategy : uses
    PickNode "1" --> "1" Location : visits
    PickNode "1" *-- "1..*" PickInstruction : executes
    PickSession "1" --> "1" SessionMetrics : tracks
    PickSession "0..*" -- "0..*" PickException : may have
    TSPSolver ..> PickPath : generates
    PickPathOptimizer --> PathOptimizationService : uses
    PickPathOptimizer ..> PickPath : creates
    PathOptimizationService ..> ZoneRouter : uses
    PathOptimizationService ..> DensityClusterer : uses
    BatchPickStrategy ..> PickSession : creates
```

## Entity Relationships

```mermaid
erDiagram
    PICK_SESSION ||--o{ PICK_INSTRUCTION : contains
    PICK_SESSION ||--o| PICK_PATH : follows
    PICK_SESSION }o--|| OPERATOR : assigned_to
    PICK_SESSION }o--o{ ORDER : picks_for
    PICK_INSTRUCTION }o--|| LOCATION : picks_from
    PICK_INSTRUCTION }o--|| PRODUCT : picks
    PICK_PATH ||--o{ PICK_NODE : contains
    PICK_NODE ||--|| LOCATION : visits
    PICK_NODE ||--o{ PICK_INSTRUCTION : executes
    LOCATION }o--|| ZONE : belongs_to
    PICK_SESSION ||--o{ PICK_EXCEPTION : may_have

    PICK_SESSION {
        string session_id PK
        string warehouse_id FK
        string type
        string status
        string operator_id FK
        json order_ids
        timestamp start_time
        timestamp end_time
        json metrics
    }

    PICK_INSTRUCTION {
        string instruction_id PK
        string session_id FK
        string order_id FK
        string product_id FK
        int required_quantity
        int picked_quantity
        string location_id FK
        string status
        timestamp pick_time
    }

    PICK_PATH {
        string path_id PK
        string session_id FK
        json nodes
        double total_distance
        long estimated_time
        string strategy
        timestamp calculated_at
    }

    PICK_NODE {
        string node_id PK
        string path_id FK
        int sequence
        string location_id FK
        json instruction_ids
        double distance_from_previous
        boolean visited
    }

    LOCATION {
        string location_id PK
        string zone FK
        string aisle
        string bay
        string level
        double x_coord
        double y_coord
        double z_coord
    }
```

## Value Objects

### PickItem
```java
public class PickItem {
    private String productId;
    private String sku;
    private String description;
    private int quantity;
    private String unitOfMeasure;
    private double weight;
    private double volume;
    private boolean fragile;
    private Map<String, Object> attributes;
}
```

### PickResult
```java
public class PickResult {
    private String instructionId;
    private int pickedQuantity;
    private int shortQuantity;
    private String shortReason;
    private byte[] photo;
    private LocalDateTime timestamp;
    private Coordinates pickLocation;
}
```

### PathSegment
```java
public class PathSegment {
    private Location from;
    private Location to;
    private double distance;
    private long estimatedTime;
    private String travelMethod; // WALK, FORKLIFT, etc
}
```

### OptimizationCriteria
```java
public class OptimizationCriteria {
    private boolean minimizeDistance;
    private boolean minimizeZoneChanges;
    private boolean prioritizeUrgent;
    private boolean groupBySKU;
    private Map<String, Double> weights;
}
```

## Domain Events

```mermaid
classDiagram
    class PickEvent {
        <<abstract>>
        +String sessionId
        +String warehouseId
        +Instant timestamp
        +String operatorId
    }

    class PickSessionCreated {
        +SessionType type
        +List~String~ orderIds
        +int totalPicks
    }

    class PickSessionAssigned {
        +String operatorId
        +String operatorName
        +LocalDateTime assignedAt
    }

    class PickSessionStarted {
        +Location startLocation
        +PickPath optimizedPath
        +LocalDateTime startTime
    }

    class PickConfirmed {
        +String instructionId
        +int pickedQuantity
        +Location pickLocation
        +LocalDateTime pickTime
    }

    class PickShorted {
        +String instructionId
        +int requiredQuantity
        +int actualQuantity
        +String shortReason
    }

    class LocationSkipped {
        +String instructionId
        +Location location
        +String skipReason
        +Location nextLocation
    }

    class PathOptimized {
        +double originalDistance
        +double optimizedDistance
        +double improvement
        +PathStrategy strategy
    }

    class PathReoptimized {
        +String reason
        +Location failedLocation
        +PickPath newPath
    }

    class PickSessionCompleted {
        +LocalDateTime completionTime
        +SessionMetrics finalMetrics
        +int totalPicks
        +int successfulPicks
    }

    class PickSessionPaused {
        +String reason
        +Location currentLocation
        +int remainingPicks
    }

    class BatchCreated {
        +List~String~ sessionIds
        +int totalOrders
        +double estimatedSavings
    }

    PickEvent <|-- PickSessionCreated
    PickEvent <|-- PickSessionAssigned
    PickEvent <|-- PickSessionStarted
    PickEvent <|-- PickConfirmed
    PickEvent <|-- PickShorted
    PickEvent <|-- LocationSkipped
    PickEvent <|-- PathOptimized
    PickEvent <|-- PathReoptimized
    PickEvent <|-- PickSessionCompleted
    PickEvent <|-- PickSessionPaused
    PickEvent <|-- BatchCreated
```

## Aggregates and Boundaries

### PickSession Aggregate
- **Root**: PickSession
- **Entities**: PickInstruction, PickPath, PickNode
- **Value Objects**: Location, Coordinates, PickItem, SessionMetrics
- **Invariants**:
  - A session can only be in one status at a time
  - All picks must be confirmed or skipped before completion
  - Path must be optimized before starting
  - Cannot modify completed sessions

### Path Optimization Boundary
- TSPSolver operates independently
- PathOptimizer coordinates optimization strategies
- Results are immutable once calculated
- Re-optimization creates new path

## Domain Services

### TSPSolver
Core path optimization algorithms:
- `solveTSP()` - Basic TSP with start point
- `solveTSPWithZones()` - Multi-zone optimization
- `nearestNeighbor()` - Greedy initial solution
- `improve2Opt()` - Local search improvement
- `calculateTotalDistance()` - Path distance calculation

### PickPathOptimizer
High-level optimization orchestration:
- `optimizePath()` - Standard path optimization
- `optimizeBatchPicking()` - Multi-order batch optimization
- `optimizeSerpentine()` - S-shaped aisle pattern
- `reoptimizePath()` - Dynamic re-routing

### PathOptimizationService
Path optimization strategies:
- `optimizeByZone()` - Zone-based grouping
- `optimizeByPriority()` - Urgent picks first
- `clusterByDensity()` - Spatial clustering

### BatchPickingService
Batch creation and management:
- `createBatches()` - Group compatible orders
- `evaluateBatchEfficiency()` - Calculate savings
- `splitLargeBatches()` - Manage batch size

### ExceptionHandlingService
Pick exception management:
- `handleLocationEmpty()` - Empty location logic
- `handleInsufficient()` - Short pick handling
- `triggerReoptimization()` - Path adjustment

## Repository Interfaces

```java
public interface PickSessionRepository {
    PickSession findById(String sessionId);
    List<PickSession> findByOperator(String operatorId);
    List<PickSession> findByStatus(SessionStatus status);
    List<PickSession> findByWarehouse(String warehouseId);
    PickSession save(PickSession session);
    void delete(String sessionId);
}

public interface PickInstructionRepository {
    List<PickInstruction> findBySession(String sessionId);
    List<PickInstruction> findByOrder(String orderId);
    PickInstruction findById(String instructionId);
    void saveAll(List<PickInstruction> instructions);
}

public interface LocationRepository {
    Location findById(String locationId);
    List<Location> findByZone(String zone);
    List<Location> findByAisle(String aisle);
    double getDistance(String from, String to);
}
```

## Business Rules

1. **Session Creation Rules**
   - Minimum 1 pick per session
   - Maximum 50 picks for single operator
   - Batch picks limited to 5 orders
   - Zone picks stay within zone boundaries

2. **Path Optimization Rules**
   - Always optimize before session start
   - Re-optimize on location failures
   - Respect zone transition costs
   - Prioritize urgent picks

3. **Pick Confirmation Rules**
   - Must scan location barcode
   - Quantity must match or report short
   - Photo required for exceptions
   - Cannot pick more than required

4. **Batch Picking Rules**
   - Orders must be compatible (size, zone)
   - Maximum travel distance increase: 20%
   - Same carrier preference
   - Similar priority levels

## Performance Optimizations

### TSP Algorithm Performance
- **Nearest Neighbor**: O(n²) complexity
- **2-opt Improvement**: O(n² × k) where k = iterations
- **Max iterations**: 100 (configurable)
- **Improvement threshold**: 1% minimum

### Caching Strategy
- Location distances cached for 24 hours
- Zone transition costs cached
- Operator capabilities cached
- Path calculations cached for 1 hour

### Batch Processing
- Process pick confirmations in batches
- Bulk path calculations for efficiency
- Aggregate metrics updates
- Async event publishing

## Algorithm Details

### TSP 2-opt Algorithm
```
1. Start with initial solution (Nearest Neighbor)
2. For each pair of edges (i, i+1) and (j, j+1):
   - Calculate cost of swapping to (i, j) and (i+1, j+1)
   - If improvement found, perform swap
3. Repeat until no improvement or max iterations
4. Return optimized path
```

### Zone Optimization
```
1. Group picks by zone
2. Calculate zone transition matrix
3. Find optimal zone sequence (TSP on zones)
4. Optimize within each zone
5. Combine into final path
```

### Serpentine Pattern
```
1. Sort aisles by proximity
2. For each aisle:
   - If even: traverse forward
   - If odd: traverse backward
3. Minimize cross-aisle movement
4. Return S-shaped path
```