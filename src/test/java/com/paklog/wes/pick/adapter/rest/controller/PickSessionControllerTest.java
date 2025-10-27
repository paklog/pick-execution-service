package com.paklog.wes.pick.adapter.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.wes.pick.domain.valueobject.Priority;
import com.paklog.wes.pick.adapter.rest.dto.*;
import com.paklog.wes.pick.application.command.ConfirmPickCommand;
import com.paklog.wes.pick.application.command.HandleShortPickCommand;
import com.paklog.wes.pick.application.command.StartPickSessionCommand;
import com.paklog.wes.pick.application.service.PickSessionService;
import com.paklog.wes.pick.domain.aggregate.PickSession;
import com.paklog.wes.pick.domain.entity.PickInstruction;
import com.paklog.wes.pick.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PickSessionController.class)
class PickSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PickSessionService pickSessionService;

    private PickSession session;

    @BeforeEach
    void setUp() {
        session = createSession();

        when(pickSessionService.createSession(any(StartPickSessionCommand.class))).thenReturn(session);
        when(pickSessionService.getSession(session.getSessionId())).thenReturn(session);
        when(pickSessionService.confirmPick(any(ConfirmPickCommand.class))).thenReturn(session);
        when(pickSessionService.handleShortPick(any(HandleShortPickCommand.class))).thenReturn(session);
        when(pickSessionService.pauseSession(session.getSessionId())).thenReturn(session);
        when(pickSessionService.resumeSession(session.getSessionId())).thenReturn(session);
        when(pickSessionService.completeSession(session.getSessionId())).thenReturn(session);
        when(pickSessionService.cancelSession(session.getSessionId(), "Cancelled"))
                .thenReturn(session);
        when(pickSessionService.getSessionProgress(session.getSessionId()))
                .thenReturn(new PickSessionService.SessionProgress(
                        session.getSessionId(),
                        session.getStatus(),
                        session.getProgress(),
                        session.getCompletedInstructionCount(),
                        session.getPickInstructions().size(),
                        session.getShortPickCount(),
                        session.calculateAccuracy(),
                        session.getDuration()
                ));
        when(pickSessionService.getActiveSessions()).thenReturn(List.of(session));
        when(pickSessionService.getCurrentInstruction(session.getSessionId()))
                .thenReturn(session.getCurrentInstruction());
    }

    @Test
    @DisplayName("Should create session via REST API")
    void shouldCreateSession() throws Exception {
        StartSessionRequest request = new StartSessionRequest(
                "TASK-1",
                "WORKER-1",
                "WH-1",
                PickStrategy.BATCH,
                "CART-1",
                List.of(
                        new PickInstructionDto(
                                "INST-1",
                                "SKU-1",
                                "Item 1",
                                5,
                                0,
                                new Location("A", "01", "01", "01"),
                                "ORDER-1",
                                InstructionStatus.PENDING,
                                0,
                                Priority.NORMAL
                        )
                )
        );

        mockMvc.perform(post("/api/v1/picks/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(session.getSessionId()))
                .andExpect(jsonPath("$.instructions", hasSize(2)));
    }

    @Test
    @DisplayName("Should fetch session details")
    void shouldGetSession() throws Exception {
        mockMvc.perform(get("/api/v1/picks/sessions/{id}", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerId").value("WORKER-1"));
    }

    @Test
    @DisplayName("Should confirm pick")
    void shouldConfirmPick() throws Exception {
        ConfirmPickRequest request = new ConfirmPickRequest("INST-1", 5);

        mockMvc.perform(post("/api/v1/picks/sessions/{id}/confirm", session.getSessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(session.getSessionId()));
    }

    @Test
    @DisplayName("Should retrieve session progress")
    void shouldGetProgress() throws Exception {
        mockMvc.perform(get("/api/v1/picks/sessions/{id}/progress", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(session.getSessionId()));
    }

    @Test
    @DisplayName("Should list active sessions")
    void shouldListActiveSessions() throws Exception {
        mockMvc.perform(get("/api/v1/picks/sessions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Should return current instruction")
    void shouldGetCurrentInstruction() throws Exception {
        mockMvc.perform(get("/api/v1/picks/sessions/{id}/current-instruction", session.getSessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructionId").value("INST-2"));
    }

    private PickSession createSession() {
        List<PickInstruction> instructions = List.of(
                new PickInstruction("INST-1", "SKU-1", "Item 1", 5, new Location("A", "01", "01", "01"), "ORDER-1", Priority.NORMAL),
                new PickInstruction("INST-2", "SKU-2", "Item 2", 5, new Location("A", "02", "01", "01"), "ORDER-1", Priority.NORMAL)
        );

        PickSession pickSession = PickSession.create(
                "TASK-1",
                "WORKER-1",
                "WH-1",
                PickStrategy.BATCH,
                "CART-1",
                instructions
        );

        PickPath path = new PickPath(
                List.of(
                        new PickPath.PathNode("INST-1", instructions.get(0).getLocation(), 0, 0.0),
                        new PickPath.PathNode("INST-2", instructions.get(1).getLocation(), 1, 10.0)
                ),
                10.0,
                Duration.ofMinutes(10),
                "TEST"
        );

        pickSession.start(path);
        pickSession.confirmPick("INST-1", 5);
        return pickSession;
    }
}
