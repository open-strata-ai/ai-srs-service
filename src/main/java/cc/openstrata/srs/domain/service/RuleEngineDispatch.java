package cc.openstrata.srs.domain.service;

import cc.openstrata.srs.domain.DomainException;
import cc.openstrata.srs.domain.EngineType;
import cc.openstrata.srs.domain.Rule;
import cc.openstrata.srs.domain.RuleDecision;
import cc.openstrata.srs.domain.port.RuleEvalPort;
import cc.openstrata.srs.web.ErrorCode;
import java.util.Map;
import java.util.Set;

/**
 * ADR-4 / §7.3 — routes Rule evaluation to the configured engine (OPA/Drools). Engines
 * coexist and are selected per-rule by the {@code engine} field; an unconfigured engine is
 * rejected with {@code RULE_ENGINE_UNSUPPORTED}. Registered as a bean by {@code DomainConfig}.
 */
public class RuleEngineDispatch {

    private final Set<EngineType> enabledEngines;

    public RuleEngineDispatch(Set<EngineType> enabledEngines) {
        this.enabledEngines = Set.copyOf(enabledEngines);
    }

    public boolean supports(EngineType engine) {
        return enabledEngines.contains(engine);
    }

    public void assertSupported(EngineType engine) {
        if (!supports(engine)) {
            throw new DomainException(ErrorCode.RULE_ENGINE_UNSUPPORTED,
                "Rule engine not configured: " + engine);
        }
    }

    /** Evaluate a rule via the given port, after verifying its engine is enabled. */
    public RuleDecision evaluate(Rule rule, Map<String, Object> input, RuleEvalPort port) {
        assertSupported(rule.engine());
        return port.evaluate(rule, input);
    }

    /** Dry-run: verify the engine and map the rule's action to a decision (no side effects). */
    public RuleDecision dryRun(Rule rule) {
        assertSupported(rule.engine());
        return RuleDecision.of(rule.action(), "dry-run: " + rule.name() + " → " + rule.action());
    }
}
