package com.paklog.wes.pick.application.service;

import com.paklog.wes.pick.application.command.ConfirmPickCommand;
import com.paklog.wes.pick.application.command.HandleShortPickCommand;
import com.paklog.wes.pick.application.command.StartPickSessionCommand;
import com.paklog.wes.pick.domain.aggregate.PickSession;
import com.paklog.wes.pick.domain.entity.PickInstruction;
import com.paklog.wes.pick.domain.repository.PickSessionRepository;
import com.paklog.wes.pick.domain.service.PathOptimizationService;
import com.paklog.wes.pick.domain.valueobject.Location;
import com.paklog.wes.pick.domain.valueobject.PickPath;
import com.paklog.wes.pick.domain.valueobject.SessionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Application service for managing pick sessions
 * Orchestrates domain logic and infrastructure
 */
@Service
public class PickSessionService {

    private static final Logger logger = LoggerFactory.getLogger(PickSessionService.class);

    private final PickSessionRepository sessionRepository;
    private final PathOptimizationService pathOptimizationService;

    public PickSessionService(
            PickSessionRepository sessionRepository,
            PathOptimizationService pathOptimizationService
    ) {
        this.sessionRepository = sessionRepository;
        this.pathOptimizationService = pathOptimizationService;
    }

    /**
     * Create and start a new pick session
     */
    @Transactional
    public PickSession createSession(StartPickSessionCommand command) {
        logger.info("Creating pick session for task: {}, worker: {}", command.taskId(), command.workerId());

        // Check if worker already has active session
        Optional<PickSession> existingSession = sessionRepository.findActiveSessionByWorkerId(command.workerId());
        if (existingSession.isPresent()) {
            throw new IllegalStateException(
                    "Worker " + command.workerId() + " already has an active session: " + existingSession.get().getSessionId()
            );
        }

        // Create session
        PickSession session = PickSession.create(
                command.taskId(),
                command.workerId(),
                command.warehouseId(),
                command.strategy(),
                command.cartId(),
                command.instructions()
        );

        // Optimize path
        Location startLocation = getWorkerStartLocation(command.workerId());
        PickPath optimizedPath = pathOptimizationService.optimizePath(command.instructions(), startLocation);

        logger.info("Path optimized: {} instructions, distance: {}, duration: {}",
                optimizedPath.getTotalPicks(), optimizedPath.totalDistance(), optimizedPath.estimatedDuration());

        // Start session with optimized path
        session.start(optimizedPath);

        // Save
        PickSession savedSession = sessionRepository.save(session);

        logger.info("Pick session created: {}", savedSession.getSessionId());

        return savedSession;
    }

    /**
     * Confirm a pick
     */
    @Transactional
    public PickSession confirmPick(ConfirmPickCommand command) {
        logger.debug("Confirming pick: session={}, instruction={}, quantity={}",
                command.sessionId(), command.instructionId(), command.quantity());

        PickSession session = findSessionById(command.sessionId());
        session.confirmPick(command.instructionId(), command.quantity());

        return sessionRepository.save(session);
    }

    /**
     * Handle short pick
     */
    @Transactional
    public PickSession handleShortPick(HandleShortPickCommand command) {
        logger.warn("Short pick: session={}, instruction={}, actual={}, reason={}",
                command.sessionId(), command.instructionId(), command.actualQuantity(), command.reason());

        PickSession session = findSessionById(command.sessionId());
        session.shortPick(command.instructionId(), command.actualQuantity(), command.reason());

        return sessionRepository.save(session);
    }

    /**
     * Pause session
     */
    @Transactional
    public PickSession pauseSession(String sessionId) {
        logger.info("Pausing session: {}", sessionId);

        PickSession session = findSessionById(sessionId);
        session.pause();

        return sessionRepository.save(session);
    }

    /**
     * Resume session
     */
    @Transactional
    public PickSession resumeSession(String sessionId) {
        logger.info("Resuming session: {}", sessionId);

        PickSession session = findSessionById(sessionId);
        session.resume();

        return sessionRepository.save(session);
    }

    /**
     * Complete session
     */
    @Transactional
    public PickSession completeSession(String sessionId) {
        logger.info("Completing session: {}", sessionId);

        PickSession session = findSessionById(sessionId);
        session.complete();

        PickSession completedSession = sessionRepository.save(session);

        logger.info("Session completed: {}, accuracy: {}%",
                sessionId, String.format("%.1f", completedSession.calculateAccuracy()));

        return completedSession;
    }

    /**
     * Cancel session
     */
    @Transactional
    public PickSession cancelSession(String sessionId, String reason) {
        logger.warn("Cancelling session: {}, reason: {}", sessionId, reason);

        PickSession session = findSessionById(sessionId);
        session.cancel(reason);

        return sessionRepository.save(session);
    }

    /**
     * Get session by ID
     */
    public PickSession getSession(String sessionId) {
        return findSessionById(sessionId);
    }

    /**
     * Get current instruction for session
     */
    public PickInstruction getCurrentInstruction(String sessionId) {
        PickSession session = findSessionById(sessionId);
        PickInstruction current = session.getCurrentInstruction();

        if (current == null) {
            throw new IllegalStateException("No current instruction for session: " + sessionId);
        }

        return current;
    }

    /**
     * Get active session for worker
     */
    public Optional<PickSession> getActiveSessionForWorker(String workerId) {
        return sessionRepository.findActiveSessionByWorkerId(workerId);
    }

    /**
     * Get session progress
     */
    public SessionProgress getSessionProgress(String sessionId) {
        PickSession session = findSessionById(sessionId);

        return new SessionProgress(
                session.getSessionId(),
                session.getStatus(),
                session.getProgress(),
                session.getCompletedInstructionCount(),
                session.getPickInstructions().size(),
                session.getShortPickCount(),
                session.calculateAccuracy(),
                session.getDuration()
        );
    }

    /**
     * Get all active sessions
     */
    public List<PickSession> getActiveSessions() {
        return sessionRepository.findByStatus(SessionStatus.IN_PROGRESS);
    }

    /**
     * Get sessions by warehouse
     */
    public List<PickSession> getSessionsByWarehouse(String warehouseId) {
        return sessionRepository.findByWarehouseId(warehouseId);
    }

    // Helper methods

    private PickSession findSessionById(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    }

    private Location getWorkerStartLocation(String workerId) {
        // TODO: Get actual worker location from worker service
        // For now, return default location
        return new Location("A", "01", "01", "01");
    }

    /**
     * Session progress information
     */
    public record SessionProgress(
            String sessionId,
            SessionStatus status,
            double progressPercentage,
            int completedInstructions,
            int totalInstructions,
            int shortPicks,
            double accuracy,
            java.time.Duration duration
    ) {}
}
