package com.paklog.wes.pick.domain.repository;

import com.paklog.wes.pick.domain.aggregate.PickSession;
import com.paklog.wes.pick.domain.valueobject.SessionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PickSession aggregate
 */
@Repository
public interface PickSessionRepository extends MongoRepository<PickSession, String> {

    /**
     * Find session by task ID
     */
    Optional<PickSession> findByTaskId(String taskId);

    /**
     * Find active session for worker
     */
    @Query("{ 'workerId': ?0, 'status': { $in: ['IN_PROGRESS', 'PAUSED'] } }")
    Optional<PickSession> findActiveSessionByWorkerId(String workerId);

    /**
     * Find all sessions for worker
     */
    List<PickSession> findByWorkerId(String workerId);

    /**
     * Find sessions by status
     */
    List<PickSession> findByStatus(SessionStatus status);

    /**
     * Find sessions by warehouse and status
     */
    List<PickSession> findByWarehouseIdAndStatus(String warehouseId, SessionStatus status);

    /**
     * Find sessions in warehouse
     */
    List<PickSession> findByWarehouseId(String warehouseId);

    /**
     * Find sessions created after date
     */
    List<PickSession> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find sessions completed after date
     */
    List<PickSession> findByCompletedAtAfter(LocalDateTime date);

    /**
     * Find sessions by worker and status
     */
    List<PickSession> findByWorkerIdAndStatus(String workerId, SessionStatus status);

    /**
     * Count active sessions
     */
    @Query(value = "{ 'status': { $in: ['IN_PROGRESS', 'PAUSED'] } }", count = true)
    long countActiveSessions();

    /**
     * Count active sessions for worker
     */
    @Query(value = "{ 'workerId': ?0, 'status': { $in: ['IN_PROGRESS', 'PAUSED'] } }", count = true)
    long countActiveSessionsByWorkerId(String workerId);
}
