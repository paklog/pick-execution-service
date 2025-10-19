package com.paklog.wes.pick.infrastructure.config;

import com.paklog.wes.pick.domain.aggregate.PickSession;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

/**
 * MongoDB configuration and index creation
 */
@Configuration
public class MongoConfig {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    private final MongoTemplate mongoTemplate;

    public MongoConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initIndexes() {
        logger.info("Creating MongoDB indexes for PickSession collection");

        IndexOperations indexOps = mongoTemplate.indexOps(PickSession.class);

        // Index for session ID (already created by MongoDB as _id)

        // Index for task ID
        indexOps.ensureIndex(new Index().on("taskId", Sort.Direction.ASC)
                .named("idx_task_id"));

        // Compound index for worker and status queries
        indexOps.ensureIndex(new Index()
                .on("workerId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("idx_worker_status"));

        // Index for warehouse and status queries
        indexOps.ensureIndex(new Index()
                .on("warehouseId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("idx_warehouse_status"));

        // Index for status only
        indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)
                .named("idx_status"));

        // Index for created timestamp (useful for reporting)
        indexOps.ensureIndex(new Index()
                .on("createdAt", Sort.Direction.DESC)
                .named("idx_created_at"));

        // Index for completed timestamp
        indexOps.ensureIndex(new Index()
                .on("completedAt", Sort.Direction.DESC)
                .named("idx_completed_at"));

        // Compound index for warehouse queries
        indexOps.ensureIndex(new Index()
                .on("warehouseId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("idx_warehouse_created"));

        logger.info("MongoDB indexes created successfully");
    }
}
