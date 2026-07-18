package cc.openstrata.srs.application.dto;

/** Define a Rule (SPECS §1.2 POST /rules). */
public record DefineRuleRequest(
    String name,
    String version,
    String engine,
    String policy,
    String action) {}
