# Pick Execution Service (WES)

Pick operations and put wall management service.

## Responsibilities

- Pick list execution (not planning)
- Pick path optimization algorithms
- Mobile picking application backend
- Put wall operations
- Batch picking coordination
- Pick accuracy tracking
- Short pick handling

## Architecture

```
domain/
├── aggregate/      # PickSession, PutWall
├── entity/         # PickInstruction, PutWallSlot
├── valueobject/    # PickStrategy, PickPath, SessionStatus
├── service/        # PickSessionService, PathOptimizationService
├── repository/     # PickSessionRepository
└── event/          # PickStartedEvent, PickCompletedEvent

application/
├── command/        # StartPickSessionCommand, ConfirmPickCommand
├── query/          # GetPickInstructionsQuery, GetPutWallStatusQuery
└── handler/        # TaskAssignedHandler

adapter/
├── rest/           # Pick management controllers
├── mobile/         # Mobile API for pickers
└── persistence/    # MongoDB repositories

infrastructure/
├── config/         # Spring configurations
├── messaging/      # Kafka publishers/consumers
├── events/         # Event publishing infrastructure
└── optimization/   # Path optimization algorithms (TSP, 2-opt)
```

## Tech Stack

- Java 21
- Spring Boot 3.2.0
- MongoDB (pick session data)
- Apache Kafka (event-driven integration)
- WebSocket (real-time updates)
- CloudEvents
- OpenAPI/Swagger

## Running the Service

```bash
mvn spring-boot:run
```

## API Documentation

Available at: http://localhost:8083/swagger-ui.html

## Events Published

- `PickSessionStartedEvent` - When picking begins
- `PickConfirmedEvent` - When an item is picked
- `ShortPickEvent` - When a short pick occurs
- `PickSessionCompletedEvent` - When all picks are done
- `PutWallOrderCompleteEvent` - When put wall order is complete

## Events Consumed

- `TaskAssignedEvent` - From Task Execution Service
- `InventoryReservedEvent` - From Inventory Service
