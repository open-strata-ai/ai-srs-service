package cc.openstrata.srs.infrastructure.adapter;

import cc.openstrata.srs.domain.Rule;
import cc.openstrata.srs.domain.RuleDecision;
import cc.openstrata.srs.domain.port.RuleEvalPort;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Offline default {@link RuleEvalPort}. Maps the rule's declared action to a decision without
 * running a real policy engine; production delegates to local OPA / the Agent runtime / Drools.
 */
@Component
public class LocalRuleEvalAdapter implements RuleEvalPort {

    @Override
    public RuleDecision evaluate(Rule rule, Map<String, Object> input) {
        // Skeleton evaluation: honor the rule's configured action deterministically.
        return RuleDecision.of(rule.action(), "evaluated " + rule.name() + " via " + rule.engine());
    }
}
