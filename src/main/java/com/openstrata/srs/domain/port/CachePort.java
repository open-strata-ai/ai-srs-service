package com.openstrata.srs.domain.port;

import java.util.Optional;

/**
 * Cache SPI (ARCH §5.2 — Redis default / Valkey alternative). Tenant-prefixed keys back the
 * high-frequency runtime {@code resolve} path (cache-hit &lt;10ms target).
 */
public interface CachePort {

    Optional<String> get(String key);

    void put(String key, String value);

    void evict(String key);
}
