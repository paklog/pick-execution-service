package com.paklog.wes.pick.infrastructure.events;

import com.paklog.wes.pick.domain.valueobject.Priority;
import com.paklog.wes.pick.application.command.StartPickSessionCommand;
import com.paklog.wes.pick.application.service.PickSessionService;
import com.paklog.wes.pick.domain.aggregate.PickSession;
import com.paklog.wes.pick.domain.entity.PickInstruction;
import com.paklog.wes.pick.domain.valueobject.Location;
import com.paklog.wes.pick.domain.valueobject.PickStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskEventHandler Tests")
class TaskEventHandlerTest {

    @Mock
    private PickSessionService pickSessionService;

    @InjectMocks
    private TaskEventHandler handler;

    @Captor
    private ArgumentCaptor<StartPickSessionCommand> commandCaptor;

    @Test
    @DisplayName("Should create pick session when TaskCreatedEvent contains valid instructions")
    void shouldCreateSessionForValidTaskCreatedEvent() {
        Map<String, Object> instruction = validInstructionPayload(3);
        instruction.put("priority", "HIGH");

        Map<String, Object> context = new HashMap<>();
        context.put("strategy", "batch");
        context.put("workerId", "WORKER-123");
        context.put("cartId", "CART-9");
        context.put("instructions", List.of(instruction));

        Map<String, Object> event = taskCreatedEvent(context);

        when(pickSessionService.createSession(any()))
                .thenReturn(stubSession("TASK-1", "WORKER-123", PickStrategy.BATCH));

        handler.handleTaskCreated(event);

        verify(pickSessionService).createSession(commandCaptor.capture());
        StartPickSessionCommand command = commandCaptor.getValue();

        assertThat(command.workerId()).isEqualTo("WORKER-123");
        assertThat(command.cartId()).isEqualTo("CART-9");
        assertThat(command.strategy()).isEqualTo(PickStrategy.BATCH);
        assertThat(command.instructions()).hasSize(1);
        assertThat(command.instructions().get(0).getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("Should ignore TaskCreatedEvent when instructions are missing")
    void shouldIgnoreTaskCreatedEventWithoutInstructions() {
        Map<String, Object> context = new HashMap<>();
        context.put("strategy", "single");
        context.put("workerId", "WORKER-1");
        context.put("cartId", "CART-1");
        context.put("instructions", List.of());

        handler.handleTaskCreated(taskCreatedEvent(context));

        verify(pickSessionService, never()).createSession(any());
    }

    @Test
    @DisplayName("Should fall back to defaults when strategy, cart or priority are invalid")
    void shouldFallbackToDefaultsForInvalidData() {
        Map<String, Object> instruction = validInstructionPayload("5");
        instruction.put("priority", "UNKNOWN");

        Map<String, Object> context = new HashMap<>();
        context.put("strategy", "invalid");
        context.put("workerId", "UNASSIGNED");
        context.put("cartId", " ");
        context.put("instructions", List.of(instruction));

        Map<String, Object> event = taskCreatedEvent(context);
        event.put("referenceId", "WAVE-99");

        when(pickSessionService.createSession(any()))
                .thenReturn(stubSession("TASK-1", "UNASSIGNED", PickStrategy.SINGLE));

        handler.handleTaskCreated(event);

        verify(pickSessionService).createSession(commandCaptor.capture());
        StartPickSessionCommand command = commandCaptor.getValue();

        assertThat(command.strategy()).isEqualTo(PickStrategy.SINGLE);
        assertThat(command.cartId()).isEqualTo("WAVE-99");

        PickInstruction instructionFromCommand = command.instructions().get(0);
        assertThat(instructionFromCommand.getExpectedQuantity()).isEqualTo(5);
        assertThat(instructionFromCommand.getPriority()).isEqualTo(Priority.NORMAL);
    }

    @Test
    @DisplayName("Should ignore TaskCreatedEvent when context payload missing")
    void shouldIgnoreTaskCreatedEventWithoutContext() {
        handler.handleTaskCreated(taskCreatedEvent(null));

        verify(pickSessionService, never()).createSession(any());
    }

    @Test
    @DisplayName("Should skip instructions that lack a valid location")
    void shouldSkipInstructionWithInvalidLocation() {
        Map<String, Object> instruction = new HashMap<>();
        instruction.put("instructionId", "INST-2");
        instruction.put("itemSku", "SKU-2");
        instruction.put("expectedQuantity", 2);
        instruction.put("location", Map.of("aisle", "A")); // Missing bay/level

        Map<String, Object> context = new HashMap<>();
        context.put("strategy", "single");
        context.put("workerId", "WORKER-1");
        context.put("cartId", "CART-1");
        context.put("instructions", List.of(instruction));

        handler.handleTaskCreated(taskCreatedEvent(context));

        verify(pickSessionService, never()).createSession(any());
    }

    @Test
    @DisplayName("Should ignore TaskCreatedEvent for non-pick tasks")
    void shouldIgnoreNonPickTaskCreatedEvent() {
        Map<String, Object> event = taskCreatedEvent(validInstructionContext());
        event.put("taskType", "PACK");

        handler.handleTaskCreated(event);

        verifyNoInteractions(pickSessionService);
    }

    @Test
    @DisplayName("Should check active sessions when TaskAssignedEvent received")
    void shouldConsultActiveSessionsOnTaskAssignedEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "TaskAssignedEvent");
        event.put("taskType", "PICK");
        event.put("taskId", "TASK-ASSIGNED");
        event.put("assignedTo", "WORKER-99");

        when(pickSessionService.getActiveSessions())
                .thenReturn(List.of(stubSession("TASK-ASSIGNED", "WORKER-99", PickStrategy.SINGLE)));

        handler.handleTaskAssigned(event);

        verify(pickSessionService).getActiveSessions();
        verifyNoMoreInteractions(pickSessionService);
    }

    @Test
    @DisplayName("Should ignore TaskAssignedEvent when picker missing")
    void shouldIgnoreTaskAssignedEventWithoutPicker() {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "TaskAssignedEvent");
        event.put("taskType", "PICK");
        event.put("taskId", "TASK-ASSIGNED");

        handler.handleTaskAssigned(event);

        verify(pickSessionService, never()).getActiveSessions();
    }

    private Map<String, Object> taskCreatedEvent(Map<String, Object> context) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "TaskCreatedEvent");
        event.put("taskType", "PICK");
        event.put("taskId", "TASK-1");
        event.put("warehouseId", "WH-1");
        event.put("referenceId", "WAVE-1");
        if (context != null) {
            event.put("context", context);
        }
        return event;
    }

    private Map<String, Object> validInstructionContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("strategy", "batch");
        context.put("workerId", "WORKER-123");
        context.put("cartId", "CART-9");
        context.put("instructions", List.of(validInstructionPayload(2)));
        return context;
    }

    private Map<String, Object> validInstructionPayload(Object expectedQuantity) {
        Map<String, Object> location = new HashMap<>();
        location.put("aisle", "A");
        location.put("bay", "01");
        location.put("level", "01");
        location.put("position", "01");

        Map<String, Object> instruction = new HashMap<>();
        instruction.put("instructionId", "INST-1");
        instruction.put("itemSku", "SKU-1");
        instruction.put("itemDescription", "Widget");
        instruction.put("expectedQuantity", expectedQuantity);
        instruction.put("orderId", "ORDER-1");
        instruction.put("location", location);
        return instruction;
    }

    private PickSession stubSession(String taskId, String workerId, PickStrategy strategy) {
        List<PickInstruction> instructions = List.of(
                new PickInstruction(
                        "STUB-1",
                        "STUB-SKU",
                        "Stub Item",
                        1,
                        new Location("A", "01", "01", "01"),
                        "ORDER-STUB",
                        Priority.NORMAL
                )
        );

        return PickSession.create(
                taskId,
                workerId,
                "WH-1",
                strategy,
                "CART-STUB",
                instructions
        );
    }
}
