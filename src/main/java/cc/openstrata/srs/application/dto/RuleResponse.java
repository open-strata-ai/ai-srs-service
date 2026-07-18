package cc.openstrata.srs.application.dto;

/** Rule projection returned to clients. */
public record RuleResponse(
    String ruleId,
    String name,
    String version,
    String engine,
    String action,
    boolean enabled) {}
