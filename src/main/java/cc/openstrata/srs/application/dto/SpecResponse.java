package cc.openstrata.srs.application.dto;

/** Spec projection returned to clients. */
public record SpecResponse(String specId, String name, String kind) {}
