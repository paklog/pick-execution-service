package com.paklog.wes.pick.adapter.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.pick.adapter.rest.dto.ConfirmPickRequest;
import com.paklog.wes.pick.adapter.rest.dto.ShortPickRequest;
import com.paklog.wes.pick.application.command.ConfirmPickCommand;
import com.paklog.wes.pick.application.command.HandleShortPickCommand;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MobilePickController.class)
class MobilePickControllerTest {

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

        when(pickSessionService.getActiveSessionForWorker("WORKER-1"))
                .thenReturn(Optional.of(session));
        when(pickSessionService.confirmPick(any(ConfirmPickCommand.class)))
                .thenReturn(session);
        when(pickSessionService.handleShortPick(any(HandleShortPickCommand.class)))
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
    }

    @Test
    @DisplayName("Should fetch worker session")
    void shouldGetWorkerSession() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/picks/my-session")
                        .header("X-Worker-Id", "WORKER-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerId").value("WORKER-1"));
    }

    @Test
    @DisplayName("Should return 404 when worker has no session")
    void shouldReturnNotFoundForMissingSession() throws Exception {
        when(pickSessionService.getActiveSessionForWorker("WORKER-2")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/mobile/picks/my-session")
                        .header("X-Worker-Id", "WORKER-2"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return current instruction")
    void shouldGetCurrentInstruction() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/picks/current-instruction")
                        .header("X-Worker-Id", "WORKER-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructionId").value("INST-2"));
    }

    @Test
    @DisplayName("Should confirm pick for worker")
    void shouldConfirmPick() throws Exception {
        ConfirmPickRequest request = new ConfirmPickRequest("INST-1", 5);

        mockMvc.perform(post("/api/v1/mobile/picks/confirm")
                        .header("X-Worker-Id", "WORKER-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(session.getSessionId()));
    }

    @Test
    @DisplayName("Should handle short pick for worker")
    void shouldHandleShortPick() throws Exception {
        ShortPickRequest request = new ShortPickRequest("INST-1", 1, "Missing items");

        mockMvc.perform(post("/api/v1/mobile/picks/short-pick")
                        .header("X-Worker-Id", "WORKER-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortPicks").value(session.getShortPickCount()));
    }

    @Test
    @DisplayName("Should get session progress for worker")
    void shouldGetProgress() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/picks/progress")
                        .header("X-Worker-Id", "WORKER-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(session.getSessionId()));
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
        when(pickSessionService.getCurrentInstruction(eq(pickSession.getSessionId())))
                .thenReturn(pickSession.getCurrentInstruction());
        return pickSession;
    }
}
