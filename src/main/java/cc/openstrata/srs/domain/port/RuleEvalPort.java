package cc.openstrata.srs.domain.port;

import cc.openstrata.srs.domain.Rule;
import cc.openstrata.srs.domain.RuleDecision;
import java.util.Map;

/**
 * Rule evaluation SPI (ARCH §5.2). Delegates policy evaluation to the concrete engine
 * (local OPA / Agent runtime / Drools). Domain routes here via {@code RuleEngineDispatch}.
 */
public interface RuleEvalPort {

    RuleDecision evaluate(Rule rule, Map<String, Object> input);
}
