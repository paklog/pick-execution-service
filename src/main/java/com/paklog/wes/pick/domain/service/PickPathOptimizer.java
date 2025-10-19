package com.paklog.wes.pick.domain.service;

import com.paklog.wes.pick.domain.aggregate.PickSession;
import com.paklog.wes.pick.domain.entity.PickInstruction;
import com.paklog.wes.pick.domain.valueobject.Location;
import com.paklog.wes.pick.domain.valueobject.PickPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Placeholder optimizer that delegates to {@link PathOptimizationService}.
 * The original implementation referenced unfinished domain types and prevented compilation.
 * This version keeps the service injectable while reusing the existing optimization logic.
 */
@Service
public class PickPathOptimizer {

    private static final Logger logger = LoggerFactory.getLogger(PickPathOptimizer.class);

    private final PathOptimizationService pathOptimizationService;

    public PickPathOptimizer(PathOptimizationService pathOptimizationService) {
        this.pathOptimizationService = pathOptimizationService;
    }

    /**
     * Optimize the path for a given session by delegating to {@link PathOptimizationService}.
     */
    public PickPath optimizePath(PickSession session, Location startLocation) {
        List<PickInstruction> instructions = session.getPickInstructions();
        logger.debug("Delegating optimization for session {} with {} instructions",
                session.getSessionId(), instructions.size());
        return pathOptimizationService.optimizePath(instructions, startLocation);
    }
}
