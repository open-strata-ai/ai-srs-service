package cc.openstrata.srs.domain;

/**
 * Rule aggregate root (ARCH §4.2). One version active per {@code name}; the {@code engine}
 * field routes evaluation to the matching engine adapter (OPA/Drools).
 */
public record Rule(
    String ruleId,
    TenantId tenantId,
    String name,
    SemVer version,
    EngineType engine,
    String policy,
    RuleAction action,
    boolean enabled) {

    public Rule {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("rule name must not be blank");
        }
    }

    public Rule withEnabled(boolean value) {
        return new Rule(ruleId, tenantId, name, version, engine, policy, action, value);
    }
}
