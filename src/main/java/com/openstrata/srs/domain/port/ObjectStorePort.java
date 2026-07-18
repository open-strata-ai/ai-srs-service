package com.openstrata.srs.domain.port;

import java.util.Optional;

/**
 * Object store SPI (ARCH §5.2 — MinIO, optional). Large Skill packages are stored here and
 * referenced by {@code skills.package_ref}. Null-object adapter used when disabled (P10).
 */
public interface ObjectStorePort {

    boolean enabled();

    /** Store package bytes and return an object reference. */
    String put(String tenantId, String skillName, byte[] content);

    Optional<byte[]> get(String ref);
}
