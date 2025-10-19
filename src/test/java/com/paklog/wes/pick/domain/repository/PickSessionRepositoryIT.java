package com.paklog.wes.pick.domain.repository;

import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.pick.domain.aggregate.PickSession;
import com.paklog.wes.pick.domain.entity.PickInstruction;
import com.paklog.wes.pick.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DisplayName("PickSessionRepository Integration Tests")
class PickSessionRepositoryIT {

    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getConnectionString);
    }

    @Autowired
    private PickSessionRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should persist session and fetch active worker session")
    void shouldPersistAndFetchActiveSession() {
        PickSession session = startSession("WORKER-1", PickStrategy.BATCH);
        repository.save(session);

        assertThat(repository.countActiveSessions()).isEqualTo(1);
        assertThat(repository.countActiveSessionsByWorkerId("WORKER-1")).isEqualTo(1);

        assertThat(repository.findActiveSessionByWorkerId("WORKER-1"))
                .isPresent()
                .get()
                .extracting(PickSession::getStatus)
                .isEqualTo(SessionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should query by warehouse and complete sessions")
    void shouldQueryByWarehouseAndStatus() {
        PickSession session = startSession("WORKER-2", PickStrategy.SINGLE);
        session.getPickInstructions().forEach(i -> session.confirmPick(i.getInstructionId(), i.getExpectedQuantity()));
        repository.save(session);

        assertThat(repository.findByStatus(SessionStatus.COMPLETED)).hasSize(1);
        assertThat(repository.findByWarehouseId("WH-1")).hasSize(1);
        assertThat(repository.findByWarehouseIdAndStatus("WH-1", SessionStatus.COMPLETED)).hasSize(1);
        assertThat(repository.findByCreatedAtAfter(session.getCreatedAt().minusMinutes(1))).hasSize(1);
        assertThat(repository.findByCompletedAtAfter(session.getCompletedAt().minusMinutes(1))).hasSize(1);
    }

    private PickSession startSession(String workerId, PickStrategy strategy) {
        List<PickInstruction> instructions = List.of(
                new PickInstruction("INST-1", "SKU-1", "Item 1", 5, new Location("A", "01", "01", "01"), "ORDER-1", Priority.NORMAL),
                new PickInstruction("INST-2", "SKU-2", "Item 2", 5, new Location("A", "02", "01", "01"), "ORDER-1", Priority.NORMAL)
        );

        PickSession session = PickSession.create(
                "TASK-" + workerId,
                workerId,
                "WH-1",
                strategy,
                "CART-1",
                instructions
        );

        PickPath path = new PickPath(
                List.of(
                        new PickPath.PathNode("INST-1", instructions.get(0).getLocation(), 0, 0.0),
                        new PickPath.PathNode("INST-2", instructions.get(1).getLocation(), 1, 10.0)
                ),
                10.0,
                Duration.ofMinutes(5),
                "TEST"
        );

        session.start(path);
        return session;
    }
}
