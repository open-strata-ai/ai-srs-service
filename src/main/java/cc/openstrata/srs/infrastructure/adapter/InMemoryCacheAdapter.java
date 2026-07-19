package cc.openstrata.srs.infrastructure.adapter;

import cc.openstrata.srs.domain.port.CachePort;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Offline default {@link CachePort}. Production swaps in a Redis/Valkey adapter (ARCH §5.4);
 * this in-memory map keeps {@code mvn test} network-free.
 */
@Component
@Profile("!prod")
public class InMemoryCacheAdapter implements CachePort {

    private final ConcurrentMap<String, String> store = new ConcurrentHashMap<>();

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public void put(String key, String value) {
        store.put(key, value);
    }

    @Override
    public void evict(String key) {
        store.remove(key);
    }
}
