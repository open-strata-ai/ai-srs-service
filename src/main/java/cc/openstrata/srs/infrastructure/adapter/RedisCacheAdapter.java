package cc.openstrata.srs.infrastructure.adapter;

import cc.openstrata.srs.domain.port.CachePort;
import java.time.Duration;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Redis-backed CachePort for prod profile (SPRING_PROFILES_ACTIVE=prod). */
@Component
@Profile("prod")
public class RedisCacheAdapter implements CachePort {

    private final StringRedisTemplate redis;
    private static final Duration TTL = Duration.ofMinutes(5);

    public RedisCacheAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(redis.opsForValue().get(prefix(key)));
    }

    @Override
    public void put(String key, String value) {
        redis.opsForValue().set(prefix(key), value, TTL);
    }

    @Override
    public void evict(String key) {
        redis.delete(prefix(key));
    }

    private static String prefix(String key) {
        return "srs:" + key;
    }
}
