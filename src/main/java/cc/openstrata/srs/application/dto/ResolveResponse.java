package cc.openstrata.srs.application.dto;

/** Runtime SRS resolution payload (SPECS §1.2 GET /resolve). */
public record ResolveResponse(
    String kind,
    String name,
    String version,
    boolean cacheHit,
    String payload) {}
