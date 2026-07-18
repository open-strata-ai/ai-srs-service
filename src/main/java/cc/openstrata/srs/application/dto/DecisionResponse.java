package cc.openstrata.srs.application.dto;

/** Rule dry-run / evaluation outcome. */
public record DecisionResponse(String action, boolean allowed, String message) {}
