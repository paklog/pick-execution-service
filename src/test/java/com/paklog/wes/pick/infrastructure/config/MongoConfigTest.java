package com.paklog.wes.pick.infrastructure.config;

import com.paklog.wes.pick.domain.aggregate.PickSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MongoConfig Tests")
class MongoConfigTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private IndexOperations indexOperations;

    @InjectMocks
    private MongoConfig mongoConfig;

    @Test
    @DisplayName("Should create all configured indexes at startup")
    void shouldCreateIndexes() {
        when(mongoTemplate.indexOps(PickSession.class)).thenReturn(indexOperations);

        mongoConfig.initIndexes();

        verify(mongoTemplate).indexOps(PickSession.class);
        verify(indexOperations, times(7)).ensureIndex(any(Index.class));
        verifyNoMoreInteractions(indexOperations);
    }
}
