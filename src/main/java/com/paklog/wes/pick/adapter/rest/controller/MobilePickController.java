package com.paklog.wes.pick.adapter.rest.controller;

import com.paklog.wes.pick.adapter.rest.dto.ConfirmPickRequest;
import com.paklog.wes.pick.adapter.rest.dto.PickInstructionDto;
import com.paklog.wes.pick.adapter.rest.dto.SessionResponse;
import com.paklog.wes.pick.adapter.rest.dto.ShortPickRequest;
import com.paklog.wes.pick.application.command.ConfirmPickCommand;
import com.paklog.wes.pick.application.command.HandleShortPickCommand;
import com.paklog.wes.pick.application.service.PickSessionService;
import com.paklog.wes.pick.domain.aggregate.PickSession;
import com.paklog.wes.pick.domain.entity.PickInstruction;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mobile-optimized REST controller for workers
 * Simplified API for warehouse floor operations
 */
@RestController
@RequestMapping("/api/v1/mobile/picks")
public class MobilePickController {

    private final PickSessionService pickSessionService;

    public MobilePickController(PickSessionService pickSessionService) {
        this.pickSessionService = pickSessionService;
    }

    /**
     * Get worker's active session
     */
    @GetMapping("/my-session")
    public ResponseEntity<SessionResponse> getMySession(@RequestHeader("X-Worker-Id") String workerId) {
        Optional<PickSession> session = pickSessionService.getActiveSessionForWorker(workerId);

        if (session.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toResponse(session.get()));
    }

    /**
     * Get current instruction to pick
     */
    @GetMapping("/current-instruction")
    public ResponseEntity<PickInstructionDto> getCurrentInstruction(@RequestHeader("X-Worker-Id") String workerId) {
        Optional<PickSession> session = pickSessionService.getActiveSessionForWorker(workerId);

        if (session.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PickInstruction instruction = session.get().getCurrentInstruction();
        if (instruction == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(toInstructionDto(instruction));
    }

    /**
     * Confirm current pick (simplified - no instruction ID needed)
     */
    @PostMapping("/confirm")
    public ResponseEntity<SessionResponse> confirmPick(
            @RequestHeader("X-Worker-Id") String workerId,
            @Valid @RequestBody ConfirmPickRequest request
    ) {
        Optional<PickSession> session = pickSessionService.getActiveSessionForWorker(workerId);

        if (session.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ConfirmPickCommand command = new ConfirmPickCommand(
                session.get().getSessionId(),
                request.instructionId(),
                request.quantity()
        );

        PickSession updatedSession = pickSessionService.confirmPick(command);
        return ResponseEntity.ok(toResponse(updatedSession));
    }

    /**
     * Report short pick
     */
    @PostMapping("/short-pick")
    public ResponseEntity<SessionResponse> shortPick(
            @RequestHeader("X-Worker-Id") String workerId,
            @Valid @RequestBody ShortPickRequest request
    ) {
        Optional<PickSession> session = pickSessionService.getActiveSessionForWorker(workerId);

        if (session.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        HandleShortPickCommand command = new HandleShortPickCommand(
                session.get().getSessionId(),
                request.instructionId(),
                request.actualQuantity(),
                request.reason()
        );

        PickSession updatedSession = pickSessionService.handleShortPick(command);
        return ResponseEntity.ok(toResponse(updatedSession));
    }

    /**
     * Get session progress
     */
    @GetMapping("/progress")
    public ResponseEntity<PickSessionService.SessionProgress> getProgress(@RequestHeader("X-Worker-Id") String workerId) {
        Optional<PickSession> session = pickSessionService.getActiveSessionForWorker(workerId);

        if (session.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PickSessionService.SessionProgress progress = pickSessionService.getSessionProgress(session.get().getSessionId());
        return ResponseEntity.ok(progress);
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
