---
layout: default
title: Home
---

# Pick Execution Service Documentation

Order picking execution and confirmation service with advanced TSP-based path optimization and intelligent batch picking strategies.

## Overview

The Pick Execution Service manages the complete picking workflow from session creation through pick confirmation. It implements sophisticated path optimization using Traveling Salesman Problem (TSP) algorithms, supports multiple picking strategies (single order, batch, zone, wave), and provides real-time pick confirmation with exception handling. The service uses Domain-Driven Design with event-driven architecture for optimal warehouse picking operations.

## Quick Links

### Getting Started
- [README](README.md) - Quick start guide and overview
- [Architecture Overview](architecture.md) - System architecture description

### Architecture & Design
- [Domain Model](DOMAIN-MODEL.md) - Complete domain model with class diagrams
- [Sequence Diagrams](SEQUENCE-DIAGRAMS.md) - Process flows and interactions
- [OpenAPI Specification](openapi.yaml) - REST API documentation
- [AsyncAPI Specification](asyncapi.yaml) - Event documentation

## Technology Stack

- **Java 21** - Programming language
- **Spring Boot 3.2** - Application framework
- **MongoDB** - Document database for session storage
- **Apache Kafka** - Event streaming platform
- **CloudEvents 2.5.0** - Event standard
- **Maven** - Build tool

## Key Features

- **TSP-Based Path Optimization** - 2-opt algorithm for optimal pick paths
- **Multiple Picking Strategies** - Single order, batch, zone, priority, wave
- **Dynamic Path Re-optimization** - Adjust paths on location failures
- **3D Location Tracking** - Precise warehouse coordinates
- **Pick Exception Handling** - Handle shorts, damages, empty locations
- **Batch Picking Optimization** - Multi-order batch creation and execution
- **Serpentine Path Support** - S-shaped aisle traversal patterns

## Domain Model

### Aggregates
- **PickSession** - Complete pick session lifecycle management

### Entities
- **PickInstruction** - Individual pick line items
- **PickPath** - Optimized picking path
- **PickNode** - Individual path waypoints

### Value Objects
- **Location** - Physical warehouse location
- **Coordinates** - 3D position (x, y, z)
- **PickItem** - Product details for picking
- **SessionMetrics** - Session performance metrics
- **PickResult** - Pick confirmation result

### Session Lifecycle

```
CREATED -> ASSIGNED -> IN_PROGRESS -> COMPLETED
                            \-> PAUSED -> IN_PROGRESS
                                   \-> CANCELLED
```

### Pick Status

```
PENDING -> IN_PROGRESS -> PICKED
                     \-> SKIPPED
                     \-> SHORT_PICKED
```

## Domain Events

### Published Events
- **PickSessionCreated** - New pick session created
- **PickSessionAssigned** - Session assigned to operator
- **PickSessionStarted** - Session execution started
- **PickConfirmed** - Item successfully picked
- **PickShorted** - Partial quantity picked
- **LocationSkipped** - Location skipped with reason
- **PathOptimized** - Path optimization completed
- **PathReoptimized** - Path dynamically re-optimized
- **PickSessionCompleted** - Session completed
- **PickSessionPaused** - Session paused
- **BatchCreated** - Batch pick session created

### Consumed Events
- **WaveReleased** - Create wave pick sessions
- **TaskAssigned** - Start assigned pick session
- **InventoryReserved** - Confirm inventory availability

## Architecture Patterns

- **Hexagonal Architecture** - Ports and adapters for clean separation
- **Domain-Driven Design** - Rich domain model with business logic
- **Event-Driven Architecture** - Asynchronous integration via events
- **Strategy Pattern** - Multiple path optimization strategies
- **State Pattern** - Session and pick status management

## API Endpoints

### Pick Session Management
- `POST /pick-sessions` - Create pick session
- `GET /pick-sessions/{sessionId}` - Get session details
- `PUT /pick-sessions/{sessionId}/assign` - Assign to operator
- `POST /pick-sessions/{sessionId}/start` - Start session
- `POST /pick-sessions/{sessionId}/complete` - Complete session
- `POST /pick-sessions/{sessionId}/cancel` - Cancel session
- `GET /pick-sessions` - List sessions with filtering

### Pick Operations
- `POST /pick-sessions/{sessionId}/picks/{instructionId}/confirm` - Confirm pick
- `POST /pick-sessions/{sessionId}/picks/{instructionId}/skip` - Skip pick
- `POST /pick-sessions/{sessionId}/pause` - Pause session
- `POST /pick-sessions/{sessionId}/resume` - Resume session
- `PUT /pick-sessions/{sessionId}/reoptimize` - Re-optimize path
- `GET /pick-sessions/{sessionId}/metrics` - Get session metrics

### Batch Picking
- `POST /batch-sessions` - Create batch pick session
- `GET /batch-sessions/{sessionId}/orders` - Get batch orders
- `POST /batch-sessions/{sessionId}/split` - Split batch

## Path Optimization

### TSP Algorithm Implementation

The service implements a 2-opt TSP optimization algorithm:

1. **Nearest Neighbor** - O(n²) initial solution
2. **2-opt Improvement** - O(n² × k) local search where k = iterations
3. **Max Iterations** - 100 (configurable)
4. **Improvement Threshold** - 1% minimum improvement

### Optimization Strategies

#### Shortest Distance
Minimizes total travel distance using TSP algorithm.

#### Zone Optimized
Groups picks by zone, optimizes zone sequence, then optimizes within each zone.

#### Priority Based
Prioritizes urgent picks first, then optimizes remaining picks.

#### Serpentine
S-shaped aisle pattern - alternating forward/backward traversal.

#### Batch Optimized
Optimizes multi-order batches considering all pick locations.

### Zone Optimization Algorithm

```
1. Group picks by zone
2. Calculate zone transition matrix
3. Find optimal zone sequence (TSP on zones)
4. Optimize within each zone
5. Combine into final path
```

### Serpentine Pattern Algorithm

```
1. Sort aisles by proximity
2. For each aisle:
   - If even: traverse forward
   - If odd: traverse backward
3. Minimize cross-aisle movement
4. Return S-shaped path
```

## Pick Session Types

### Single Order Pick
One operator picks one order with optimized path.

### Batch Pick
One operator picks multiple orders simultaneously (max 5 orders).

### Zone Pick
Picks constrained to single warehouse zone.

### Priority Pick
Urgent orders processed with priority path optimization.

### Wave Pick
Multiple orders from released wave with coordinated picking.

## Exception Handling

### Exception Types
- **LOCATION_EMPTY** - No inventory at location
- **INSUFFICIENT_QUANTITY** - Less than required quantity
- **WRONG_PRODUCT** - Incorrect product at location
- **DAMAGED_PRODUCT** - Product damage identified
- **LOCATION_BLOCKED** - Location inaccessible
- **SYSTEM_ERROR** - System or scanning error

### Exception Resolution
- Automatic path re-optimization on location failures
- Alternative location suggestions
- Short pick recording and reporting
- Photo capture for quality control

## Batch Picking

### Batch Creation Rules
- Orders must be compatible (size, zone)
- Maximum travel distance increase: 20%
- Same carrier preference
- Similar priority levels
- Max 5 orders per batch

### Batch Optimization
- Multi-order path consolidation
- Shared location visit optimization
- Order separation tracking
- Batch efficiency scoring

## Integration Points

### Consumes Events From
- Wave Planning (wave released)
- Task Execution (pick task assigned)
- Inventory (inventory status)

### Publishes Events To
- Task Execution (pick completed)
- Physical Tracking (inventory movements)
- Pack Ship (picks ready for packing)
- Order Management (picking progress)

## Performance Considerations

### Algorithm Performance
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

### Database Indexes
- MongoDB indexes on sessionId, operatorId, status, warehouse
- Compound index on warehouse + status + createdAt
- Geospatial index on location coordinates

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

## Metrics and KPIs

### Session Metrics
- Total picks vs. completed picks
- Pick accuracy percentage
- Average pick time per line
- Total distance traveled
- Picking efficiency score

### Operator Metrics
- Picks per hour
- Accuracy rate
- Average session completion time
- Zone coverage

## Getting Started

1. Review the [README](README.md) for quick start instructions
2. Understand the [Architecture](architecture.md) and design patterns
3. Explore the [Domain Model](DOMAIN-MODEL.md) to understand business concepts
4. Study the [Sequence Diagrams](SEQUENCE-DIAGRAMS.md) for process flows
5. Reference the [OpenAPI](openapi.yaml) and [AsyncAPI](asyncapi.yaml) specifications

## Configuration

Key configuration properties:
- `pick.optimization.algorithm` - TSP, zone, serpentine, priority
- `pick.optimization.max-iterations` - TSP algorithm iterations (default: 100)
- `pick.optimization.improvement-threshold` - Min improvement % (default: 1.0)
- `pick.batch.max-orders` - Maximum orders per batch (default: 5)
- `pick.batch.max-distance-increase` - Max distance increase % (default: 20)
- `pick.cache.location-distance-ttl` - Location distance cache TTL (default: 24h)
- `pick.cache.path-ttl` - Path calculation cache TTL (default: 1h)

## Contributing

For contribution guidelines, please refer to the main README in the project root.

## Support

- **GitHub Issues**: Report bugs or request features
- **Documentation**: Browse the guides in the navigation menu
- **Service Owner**: WMS Team
- **Slack**: #wms-pick-execution
