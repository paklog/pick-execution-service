package com.paklog.wes.pick.application.service;

import com.paklog.wes.pick.domain.valueobject.Priority;
import com.paklog.wes.pick.application.command.ConfirmPickCommand;
import com.paklog.wes.pick.application.command.HandleShortPickCommand;
import com.paklog.wes.pick.application.command.StartPickSessionCommand;
import com.paklog.wes.pick.domain.aggregate.PickSession;
import com.paklog.wes.pick.domain.entity.PickInstruction;
import com.paklog.wes.pick.domain.repository.PickSessionRepository;
import com.paklog.wes.pick.domain.service.PathOptimizationService;
import com.paklog.wes.pick.domain.valueobject.InstructionStatus;
import com.paklog.wes.pick.domain.valueobject.Location;
import com.paklog.wes.pick.domain.valueobject.PickPath;
import com.paklog.wes.pick.domain.valueobject.PickStrategy;
import com.paklog.wes.pick.domain.valueobject.SessionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PickSessionService Tests")
@ExtendWith(MockitoExtension.class)
class PickSessionServiceTest {

    @Mock
    private PickSessionRepository repository;

    @Mock
    private PathOptimizationService pathOptimizationService;

    @InjectMocks
    private PickSessionService service;

    private List<PickInstruction> instructions;
    private PickPath optimizedPath;

    @BeforeEach
    void setUp() {
        instructions = List.of(
                new PickInstruction("INST-1", "SKU-1", "Item 1", 5, location("02"), "ORDER-1", Priority.NORMAL),
                new PickInstruction("INST-2", "SKU-2", "Item 2", 5, location("03"), "ORDER-1", Priority.NORMAL)
        );

        optimizedPath = new PickPath(
                List.of(
                        new PickPath.PathNode("INST-1", location("02"), 0, 0.0),
                        new PickPath.PathNode("INST-2", location("03"), 1, 10.0)
                ),
                10.0,
                Duration.ofMinutes(5),
                "NEAREST_NEIGHBOR"
        );

        lenient().when(pathOptimizationService.optimizePath(any(), any())).thenReturn(optimizedPath);
        lenient().when(repository.save(any(PickSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should create and start session")
    void shouldCreateSession() {
        StartPickSessionCommand command = new StartPickSessionCommand(
                "TASK-1",
                "WORKER-1",
                "WH-1",
                PickStrategy.BATCH,
                "CART-1",
                instructions
        );

        lenient().when(repository.findActiveSessionByWorkerId("WORKER-1")).thenReturn(Optional.empty());

        PickSession session = service.createSession(command);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(session.getOptimizedPath()).isEqualTo(optimizedPath);

        ArgumentCaptor<PickSession> captor = ArgumentCaptor.forClass(PickSession.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPickInstructions()).hasSize(2);
    }

    @Test
    @DisplayName("Should prevent creating session when worker already active")
    void shouldPreventDuplicateActiveSession() {
        PickSession existing = PickSession.create(
                "TASK-EXISTING",
                "WORKER-1",
                "WH-1",
                PickStrategy.SINGLE,
                "CART-1",
                instructions
        );
        when(repository.findActiveSessionByWorkerId("WORKER-1")).thenReturn(Optional.of(existing));

        StartPickSessionCommand command = new StartPickSessionCommand(
                "TASK-2",
                "WORKER-1",
                "WH-1",
                PickStrategy.BATCH,
                "CART-2",
                instructions
        );

        assertThatThrownBy(() -> service.createSession(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has an active session");
    }

    @Test
    @DisplayName("Should confirm pick via service layer")
    void shouldConfirmPick() {
        PickSession session = startSession();
        when(repository.findById(session.getSessionId())).thenReturn(Optional.of(session));

        ConfirmPickCommand command = new ConfirmPickCommand(session.getSessionId(), "INST-1", 5);
        PickSession updated = service.confirmPick(command);

        assertThat(updated.getCompletedInstructionCount()).isEqualTo(1);
        verify(repository, times(2)).save(any(PickSession.class)); // once when creating, once when confirming
    }

    @Test
    @DisplayName("Should handle short pick via service layer")
    void shouldHandleShortPick() {
        PickSession session = startSession();
        when(repository.findById(session.getSessionId())).thenReturn(Optional.of(session));

        HandleShortPickCommand command = new HandleShortPickCommand(session.getSessionId(), "INST-1", 1, "Shortage");
        PickSession updated = service.handleShortPick(command);

        assertThat(updated.getShortPickCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should pause and resume session")
    void shouldPauseAndResume() {
        PickSession session = startSession();
        when(repository.findById(session.getSessionId())).thenReturn(Optional.of(session));

        PickSession paused = service.pauseSession(session.getSessionId());
        assertThat(paused.getStatus()).isEqualTo(SessionStatus.PAUSED);

        PickSession resumed = service.resumeSession(session.getSessionId());
        assertThat(resumed.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should compute session progress projection")
    void shouldReturnSessionProgress() {
        PickSession session = startSession();
        when(repository.findById(session.getSessionId())).thenReturn(Optional.of(session));

        PickSessionService.SessionProgress progress = service.getSessionProgress(session.getSessionId());

        assertThat(progress.sessionId()).isEqualTo(session.getSessionId());
        assertThat(progress.totalInstructions()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should list active sessions and by warehouse")
    void shouldQuerySessions() {
        PickSession session = startSession();
        when(repository.findByStatus(SessionStatus.IN_PROGRESS)).thenReturn(List.of(session));
        when(repository.findByWarehouseId("WH-1")).thenReturn(List.of(session));

        assertThat(service.getActiveSessions()).hasSize(1);
        assertThat(service.getSessionsByWarehouse("WH-1")).hasSize(1);
    }

    @Test
    @DisplayName("Should complete and persist session when all instructions picked")
    void shouldCompleteSession() {
        PickSession session = startSession();
        session.getPickInstructions().forEach(instruction -> {
            instruction.setPickedQuantity(instruction.getExpectedQuantity());
            instruction.setStatus(InstructionStatus.PICKED);
        });

        when(repository.findById(session.getSessionId())).thenReturn(Optional.of(session));

        PickSession completed = service.completeSession(session.getSessionId());

        assertThat(completed.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
        verify(repository, atLeastOnce()).save(session);
    }

    @Test
    @DisplayName("Should cancel session and store reason")
    void shouldCancelSession() {
        PickSession session = startSession();
        when(repository.findById(session.getSessionId())).thenReturn(Optional.of(session));

        PickSession cancelled = service.cancelSession(session.getSessionId(), "No longer needed");

        assertThat(cancelled.getStatus()).isEqualTo(SessionStatus.CANCELLED);
        assertThat(cancelled.getCancellationReason()).isEqualTo("No longer needed");
        verify(repository, atLeastOnce()).save(session);
    }

    @Test
    @DisplayName("Should expose current instruction")
    void shouldReturnCurrentInstruction() {
        PickSession session = startSession();
        when(repository.findById(session.getSessionId())).thenReturn(Optional.of(session));

        PickInstruction current = service.getCurrentInstruction(session.getSessionId());

        assertThat(current.getInstructionId()).isEqualTo("INST-1");
    }

    @Test
    @DisplayName("Should raise error when session has no current instruction")
    void shouldFailWhenCurrentInstructionMissing() {
        PickSession session = startSession();
        session.getPickInstructions().forEach(instruction ->
                session.confirmPick(instruction.getInstructionId(), instruction.getExpectedQuantity())
        );

        when(repository.findById(session.getSessionId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.getCurrentInstruction(session.getSessionId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No current instruction");
    }

    @Test
    @DisplayName("Should fetch session by id")
    void shouldReturnSessionById() {
        PickSession session = startSession();
        when(repository.findById(session.getSessionId())).thenReturn(Optional.of(session));

        assertThat(service.getSession(session.getSessionId())).isEqualTo(session);
    }

    @Test
    @DisplayName("Should find active session for worker")
    void shouldReturnActiveSessionForWorker() {
        PickSession session = startSession();
        when(repository.findActiveSessionByWorkerId("WORKER-1")).thenReturn(Optional.of(session));

        assertThat(service.getActiveSessionForWorker("WORKER-1")).contains(session);
    }

    private PickSession startSession() {
        when(repository.findActiveSessionByWorkerId("WORKER-1")).thenReturn(Optional.empty());
        StartPickSessionCommand command = new StartPickSessionCommand(
                "TASK-1",
                "WORKER-1",
                "WH-1",
                PickStrategy.BATCH,
                "CART-1",
                instructions
        );
        PickSession session = service.createSession(command);
        return session;
    }

    private Location location(String bay) {
        return new Location("A", bay, "01", "01");
    }
}
