package com.openstrata.srs.domain.port;

import com.openstrata.srs.domain.Rule;

/**
 * Pushes Rule changes / hits to governance consumers — ai-platform-api and ai-admin-service
 * advanced guardrails (ARCH §5.2, event {@code RuleChanged}).
 */
public interface PolicyConsumerPort {

    void ruleChanged(Rule rule);
}
