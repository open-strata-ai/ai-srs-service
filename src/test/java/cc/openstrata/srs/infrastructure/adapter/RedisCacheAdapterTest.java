package cc.openstrata.srs.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisCacheAdapterTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> ops;
    private RedisCacheAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        adapter = new RedisCacheAdapter(redis);
    }

    @Test
    void putStoresWithPrefix() {
        adapter.put("resolve:cfg", "result-data");
        verify(ops).set("srs:resolve:cfg", "result-data", Duration.ofMinutes(5));
    }

    @Test
    void getReturnsValueWhenPresent() {
        when(ops.get("srs:resolve:cfg")).thenReturn("result-data");
        Optional<String> result = adapter.get("resolve:cfg");
        assertTrue(result.isPresent());
        assertEquals("result-data", result.get());
    }

    @Test
    void getReturnsEmptyWhenMissing() {
        when(ops.get("srs:missing")).thenReturn(null);
        assertTrue(adapter.get("missing").isEmpty());
    }

    @Test
    void evictDeletesKey() {
        adapter.evict("temp-key");
        verify(redis).delete("srs:temp-key");
    }

    @Test
    void getReturnsEmptyForNullValue() {
        when(ops.get("srs:null-key")).thenReturn(null);
        assertTrue(adapter.get("null-key").isEmpty());
    }
}
