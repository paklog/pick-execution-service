package com.paklog.wes.pick.adapter.rest.controller;

import com.paklog.wes.pick.domain.valueobject.Priority;
import com.paklog.wes.pick.adapter.rest.dto.*;
import com.paklog.wes.pick.application.command.ConfirmPickCommand;
import com.paklog.wes.pick.application.command.HandleShortPickCommand;
import com.paklog.wes.pick.application.command.StartPickSessionCommand;
import com.paklog.wes.pick.application.service.PickSessionService;
import com.paklog.wes.pick.domain.aggregate.PickSession;
import com.paklog.wes.pick.domain.entity.PickInstruction;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for pick session management
 */
@RestController
@RequestMapping("/api/v1/picks")
public class PickSessionController {

    private final PickSessionService pickSessionService;

    public PickSessionController(PickSessionService pickSessionService) {
        this.pickSessionService = pickSessionService;
    }

    /**
     * Start a new pick session
     */
    @PostMapping("/sessions")
    public ResponseEntity<SessionResponse> startSession(@Valid @RequestBody StartSessionRequest request) {
        // Convert DTOs to domain entities
        List<PickInstruction> instructions = request.instructions().stream()
                .map(dto -> new PickInstruction(
                        dto.instructionId(),
                        dto.itemSku(),
                        dto.itemDescription(),
                        dto.expectedQuantity(),
                        dto.location(),
                        dto.orderId(),
                        dto.priority() != null ? dto.priority() : Priority.NORMAL
                ))
                .collect(Collectors.toList());

        StartPickSessionCommand command = new StartPickSessionCommand(
                request.taskId(),
                request.workerId(),
                request.warehouseId(),
                request.strategy(),
                request.cartId(),
                instructions
        );

        PickSession session = pickSessionService.createSession(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(session));
    }

    /**
     * Get session by ID
     */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<SessionResponse> getSession(@PathVariable String id) {
        PickSession session = pickSessionService.getSession(id);
        return ResponseEntity.ok(toResponse(session));
    }

    /**
     * Confirm a pick
     */
    @PostMapping("/sessions/{id}/confirm")
    public ResponseEntity<SessionResponse> confirmPick(
            @PathVariable String id,
            @Valid @RequestBody ConfirmPickRequest request
    ) {
        ConfirmPickCommand command = new ConfirmPickCommand(id, request.instructionId(), request.quantity());
        PickSession session = pickSessionService.confirmPick(command);
        return ResponseEntity.ok(toResponse(session));
    }

    /**
     * Handle short pick
     */
    @PostMapping("/sessions/{id}/short-pick")
    public ResponseEntity<SessionResponse> shortPick(
            @PathVariable String id,
            @Valid @RequestBody ShortPickRequest request
    ) {
        HandleShortPickCommand command = new HandleShortPickCommand(
                id,
                request.instructionId(),
                request.actualQuantity(),
                request.reason()
        );
        PickSession session = pickSessionService.handleShortPick(command);
        return ResponseEntity.ok(toResponse(session));
    }

    /**
     * Pause session
     */
    @PostMapping("/sessions/{id}/pause")
    public ResponseEntity<SessionResponse> pauseSession(@PathVariable String id) {
        PickSession session = pickSessionService.pauseSession(id);
        return ResponseEntity.ok(toResponse(session));
    }

    /**
     * Resume session
     */
    @PostMapping("/sessions/{id}/resume")
    public ResponseEntity<SessionResponse> resumeSession(@PathVariable String id) {
        PickSession session = pickSessionService.resumeSession(id);
        return ResponseEntity.ok(toResponse(session));
    }

    /**
     * Complete session
     */
    @PostMapping("/sessions/{id}/complete")
    public ResponseEntity<SessionResponse> completeSession(@PathVariable String id) {
        PickSession session = pickSessionService.completeSession(id);
        return ResponseEntity.ok(toResponse(session));
    }

    /**
     * Cancel session
     */
    @PostMapping("/sessions/{id}/cancel")
    public ResponseEntity<SessionResponse> cancelSession(
            @PathVariable String id,
            @RequestParam String reason
    ) {
        PickSession session = pickSessionService.cancelSession(id, reason);
        return ResponseEntity.ok(toResponse(session));
    }

    /**
     * Get session progress
     */
    @GetMapping("/sessions/{id}/progress")
    public ResponseEntity<PickSessionService.SessionProgress> getProgress(@PathVariable String id) {
        PickSessionService.SessionProgress progress = pickSessionService.getSessionProgress(id);
        return ResponseEntity.ok(progress);
    }

    /**
     * Get all active sessions
     */
    @GetMapping("/sessions/active")
    public ResponseEntity<List<SessionResponse>> getActiveSessions() {
        List<PickSession> sessions = pickSessionService.getActiveSessions();
        List<SessionResponse> responses = sessions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Get current instruction for session
     */
    @GetMapping("/sessions/{id}/current-instruction")
    public ResponseEntity<PickInstructionDto> getCurrentInstruction(@PathVariable String id) {
        PickInstruction instruction = pickSessionService.getCurrentInstruction(id);
        return ResponseEntity.ok(toInstructionDto(instruction));
    }

    // Helper methods

    private SessionResponse toResponse(PickSession session) {
        return new SessionResponse(
                session.getSessionId(),
                session.getTaskId(),
                session.getWorkerId(),
                session.getWarehouseId(),
                session.getStrategy(),
                session.getStatus(),
                session.getCartId(),
                session.getPickInstructions().stream()
                        .map(this::toInstructionDto)
                        .collect(Collectors.toList()),
                session.getCurrentInstructionIndex(),
                session.getCreatedAt(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getProgress(),
                session.calculateAccuracy(),
                session.getCompletedInstructionCount(),
                session.getPickInstructions().size(),
                session.getShortPickCount()
        );
    }

    private PickInstructionDto toInstructionDto(PickInstruction instruction) {
        return new PickInstructionDto(
                instruction.getInstructionId(),
                instruction.getItemSku(),
                instruction.getItemDescription(),
                instruction.getExpectedQuantity(),
                instruction.getPickedQuantity(),
                instruction.getLocation(),
                instruction.getOrderId(),
                instruction.getStatus(),
                instruction.getSequenceNumber(),
                instruction.getPriority()
        );
    }
}
