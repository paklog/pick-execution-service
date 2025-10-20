# Pick Execution Service

Mobile-first pick session management with Traveling Salesman Problem (TSP) path optimization for efficient order picking operations.

## Overview

The Pick Execution Service orchestrates the complete lifecycle of order picking sessions within warehouse operations. This bounded context receives pick tasks, creates optimized pick sessions with intelligent path routing using TSP algorithms, guides mobile pickers through optimized pick paths, handles short picks and exceptions, and provides real-time session tracking. The service integrates with mobile applications to deliver turn-by-turn picking guidance with barcode validation and quantity confirmation.

## Domain-Driven Design

### Bounded Context
**Pick Session Execution & Path Optimization** - Manages picking operations from session creation through completion with intelligent path optimization and mobile guidance.

### Core Domain Model

#### Aggregates
- **PickSession** - Root aggregate representing a picking session for a worker

#### Entities
- **PickInstruction** - Individual line-level pick within a session

#### Value Objects
- **SessionStatus** - Session lifecycle status (CREATED, IN_PROGRESS, PAUSED, COMPLETED, CANCELLED)
- **InstructionStatus** - Instruction status (PENDING, IN_PROGRESS, PICKED, SHORT_PICKED, SKIPPED)
- **PickStrategy** - Picking strategy type (DISCRETE, BATCH, WAVE, ZONE, CLUSTER)
- **PickPath** - Optimized sequence of locations with TSP routing
- **Location** - Physical warehouse location reference

#### Domain Events
- **PickSessionStartedEvent** - Pick session started by worker
- **PickConfirmedEvent** - Item successfully picked
- **ShortPickEvent** - Partial pick due to insufficient inventory
- **PickSessionPausedEvent** - Session temporarily paused
- **PickSessionResumedEvent** - Paused session resumed
- **PickSessionCompletedEvent** - All picks completed
- **PickSessionCancelledEvent** - Session cancelled

### Ubiquitous Language
- **Pick Session**: Complete picking workflow for assigned orders
- **Pick Instruction**: Single line item to be picked from a location
- **Pick Path**: Optimized route through warehouse locations
- **Short Pick**: Partial fulfillment due to inventory shortage
- **Pick Accuracy**: Percentage of correct picks vs total picks
- **Pick Cart**: Mobile container for collecting picked items
- **Traveling Salesman**: Path optimization algorithm for minimal travel
- **2-opt Improvement**: Local search optimization for path refinement

## Architecture & Patterns

### Hexagonal Architecture (Ports and Adapters)

```
src/main/java/com/paklog/wes/pick/
├── domain/                           # Core business logic
│   ├── aggregate/                   # Aggregates
│   │   └── PickSession.java         # Session aggregate root
│   ├── entity/                      # Entities
│   │   └── PickInstruction.java     # Pick line item
│   ├── valueobject/                 # Value objects
│   │   ├── SessionStatus.java
│   │   ├── InstructionStatus.java
│   │   ├── PickStrategy.java
│   │   ├── PickPath.java
│   │   └── Location.java
│   ├── repository/                  # Repository interfaces
│   │   └── PickSessionRepository.java
│   ├── service/                     # Domain services
│   │   ├── PathOptimizationService.java
│   │   └── PickAccuracyCalculator.java
│   └── event/                       # Domain events
├── application/                      # Use cases & orchestration
│   ├── service/                     # Application services
│   │   └── PickSessionService.java
│   ├── command/                     # Commands
│   │   ├── StartPickSessionCommand.java
│   │   ├── ConfirmPickCommand.java
│   │   └── HandleShortPickCommand.java
│   └── query/                       # Queries
└── adapter/                          # External adapters
    ├── rest/                        # REST controllers
    │   ├── PickSessionController.java
    │   └── MobilePickController.java
    ├── persistence/                 # MongoDB repositories
    ├── optimization/                # TSP algorithms
    │   ├── TravelingSalesmanSolver.java
    │   └── TwoOptOptimizer.java
    └── events/                      # Event publishers/consumers
```

### Design Patterns & Principles
- **Hexagonal Architecture** - Clean separation of domain and infrastructure
- **Domain-Driven Design** - Rich domain model with session lifecycle
- **Strategy Pattern** - Pluggable pick strategies and optimization algorithms
- **Traveling Salesman Problem** - NP-hard path optimization
- **2-opt Local Search** - Iterative path improvement algorithm
- **Event-Driven Architecture** - Real-time session event publishing
- **Repository Pattern** - Data access abstraction
- **SOLID Principles** - Maintainable and extensible code

## Technology Stack

### Core Framework
- **Java 21** - Programming language
- **Spring Boot 3.3.3** - Application framework
- **Maven** - Build and dependency management

### Data & Persistence
- **MongoDB** - Document database for session storage
- **Spring Data MongoDB** - Data access layer

### Messaging & Events
- **Apache Kafka** - Event streaming platform
- **Spring Kafka** - Kafka integration
- **CloudEvents 2.5.0** - Standardized event format

### API & Documentation
- **Spring Web MVC** - REST API framework
- **Bean Validation** - Input validation
- **OpenAPI/Swagger** - API documentation

### Observability
- **Spring Boot Actuator** - Health checks and metrics
- **Micrometer** - Metrics collection
- **Micrometer Tracing** - Distributed tracing
- **Loki Logback Appender** - Log aggregation

### Testing
- **JUnit 5** - Unit testing framework
- **Testcontainers** - Integration testing
- **Mockito** - Mocking framework
- **AssertJ** - Fluent assertions

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Local development environment

## Standards Applied

### Architectural Standards
- ✅ Hexagonal Architecture (Ports and Adapters)
- ✅ Domain-Driven Design tactical patterns
- ✅ Event-Driven Architecture
- ✅ Microservices architecture
- ✅ RESTful API design
- ✅ Algorithm-driven optimization

### Code Quality Standards
- ✅ SOLID principles
- ✅ Clean Code practices
- ✅ Comprehensive unit and integration testing
- ✅ Domain-driven design patterns
- ✅ Immutable value objects
- ✅ Rich domain models with business logic

### Event & Integration Standards
- ✅ CloudEvents specification v1.0
- ✅ Event-driven session coordination
- ✅ At-least-once delivery semantics
- ✅ Event versioning strategy
- ✅ Idempotent event handling

### Observability Standards
- ✅ Structured logging (JSON)
- ✅ Distributed tracing
- ✅ Health check endpoints
- ✅ Prometheus metrics
- ✅ Correlation ID propagation

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/paklog/pick-execution-service.git
   cd pick-execution-service
   ```

2. **Start infrastructure services**
   ```bash
   docker-compose up -d mongodb kafka
   ```

3. **Build and run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Verify the service is running**
   ```bash
   curl http://localhost:8083/actuator/health
   ```

### Using Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f pick-execution-service

# Stop all services
docker-compose down
```

## API Documentation

Once running, access the interactive API documentation:
- **Swagger UI**: http://localhost:8083/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8083/v3/api-docs

### Key Endpoints

- `POST /pick-sessions` - Create new pick session
- `GET /pick-sessions/{sessionId}` - Get session details
- `POST /pick-sessions/{sessionId}/start` - Start session with optimized path
- `POST /pick-sessions/{sessionId}/pick` - Confirm pick
- `POST /pick-sessions/{sessionId}/short-pick` - Handle short pick
- `POST /pick-sessions/{sessionId}/pause` - Pause session
- `POST /pick-sessions/{sessionId}/resume` - Resume session
- `POST /pick-sessions/{sessionId}/complete` - Complete session
- `POST /pick-sessions/{sessionId}/cancel` - Cancel session
- `GET /pick-sessions/{sessionId}/current-instruction` - Get current pick
- `GET /pick-sessions/{sessionId}/next-instruction` - Preview next pick
- `GET /mobile/pick-sessions/worker/{workerId}` - Get worker's active sessions

## Path Optimization Algorithms

### Traveling Salesman Problem (TSP) Solver

The service implements a sophisticated TSP solver for optimal pick path generation:

**Algorithm**: Nearest Neighbor with 2-opt Improvement

```
1. Start at picking staging location
2. Greedily visit nearest unvisited location
3. Return to staging when all locations visited
4. Apply 2-opt local search for improvement
5. Generate sequenced pick path
```

**Complexity**: O(n²) for construction, O(n²) iterations for 2-opt

**Features**:
- **Distance calculation**: Euclidean distance between warehouse coordinates
- **Zone constraints**: Respect warehouse zone boundaries
- **Aisle optimization**: Minimize cross-aisle travel
- **Vertical optimization**: Optimize vertical travel within aisles
- **Return path**: Efficient route back to pack station

### 2-opt Path Improvement

Iterative edge-swap optimization:

```java
while (improvement_found) {
    for each pair of edges (i,j) and (k,l) {
        if (swapping edges reduces total distance) {
            swap edges
            mark improvement found
        }
    }
}
```

**Result**: 10-30% reduction in pick path distance on average

### Pick Strategies

- **Discrete Picking**: One order at a time (simple path)
- **Batch Picking**: Multiple orders, single pass (complex path)
- **Wave Picking**: Time-based batches with priority
- **Zone Picking**: Zone-confined picking with handoffs
- **Cluster Picking**: Location-based grouping

## Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run tests with coverage
mvn clean verify jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Configuration

Key configuration properties:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/pick_execution
  kafka:
    bootstrap-servers: localhost:9092

pick-execution:
  path-optimization:
    algorithm: tsp-2opt
    max-iterations: 100
    improvement-threshold: 0.01
  picking:
    default-strategy: discrete
    barcode-validation: strict
    accuracy-tracking: enabled
```

## Event Integration

### Published Events
- `com.paklog.wes.pick.session.started.v1`
- `com.paklog.wes.pick.confirmed.v1`
- `com.paklog.wes.pick.short.v1`
- `com.paklog.wes.pick.session.paused.v1`
- `com.paklog.wes.pick.session.resumed.v1`
- `com.paklog.wes.pick.session.completed.v1`
- `com.paklog.wes.pick.session.cancelled.v1`

### Consumed Events
- `com.paklog.wes.task.assigned.v1` - Create pick session from task
- `com.paklog.inventory.reserved.v1` - Inventory availability confirmation
- `com.paklog.inventory.adjustment.v1` - Inventory discrepancy updates

### Event Format
All events follow the CloudEvents specification v1.0 and are published asynchronously via Kafka.

## Pick Session Lifecycle

```
CREATED → IN_PROGRESS → COMPLETED
    ↓          ↓
CANCELLED   PAUSED → RESUMED → IN_PROGRESS
```

### State Transitions
- **CREATED → IN_PROGRESS**: Worker starts session, path optimized
- **IN_PROGRESS → PAUSED**: Temporary break or interruption
- **PAUSED → RESUMED**: Worker returns to picking
- **IN_PROGRESS → COMPLETED**: All instructions picked
- **Any → CANCELLED**: Session cancelled (except COMPLETED)

### Pick Instruction States
- **PENDING**: Not yet started
- **IN_PROGRESS**: Currently being picked
- **PICKED**: Successfully completed
- **SHORT_PICKED**: Partially completed due to shortage
- **SKIPPED**: Deferred for later resolution

## Monitoring

- **Health**: http://localhost:8083/actuator/health
- **Metrics**: http://localhost:8083/actuator/metrics
- **Prometheus**: http://localhost:8083/actuator/prometheus
- **Info**: http://localhost:8083/actuator/info

### Key Metrics
- `pick.sessions.created.total` - Total sessions created
- `pick.sessions.completed.total` - Total sessions completed
- `pick.accuracy.percentage` - Overall pick accuracy
- `pick.path.distance.meters` - Average path distance
- `pick.path.optimization.time` - TSP computation time
- `pick.session.duration.seconds` - Session execution time
- `pick.short.rate` - Short pick occurrence rate

## Contributing

1. Follow hexagonal architecture principles
2. Implement domain logic in domain layer
3. Optimize pick paths using TSP algorithms
4. Maintain session lifecycle state transitions
5. Handle short picks gracefully
6. Write comprehensive tests including algorithm tests
7. Document domain concepts using ubiquitous language
8. Follow existing code style and conventions

## License

Copyright © 2024 Paklog. All rights reserved.
