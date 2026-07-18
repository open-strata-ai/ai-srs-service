package com.openstrata.srs.domain.port;

import java.util.Set;

/**
 * Auth SPI (auth-contract §3, ARCH §5.2 — Keycloak default). Translates a bearer token into
 * tenant/roles. Domain defines the interface only; infrastructure supplies the adapter.
 */
public interface AuthPort {

    /** Resolved identity for a request. */
    record Identity(String tenantId, Set<String> roles) {}

    /** Validate/parse a token into an {@link Identity}, or {@code null} when unauthenticated. */
    Identity resolve(String bearerToken);
}
