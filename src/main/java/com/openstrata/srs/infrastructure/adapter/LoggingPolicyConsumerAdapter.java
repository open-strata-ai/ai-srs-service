package com.openstrata.srs.infrastructure.adapter;

import com.openstrata.srs.domain.Rule;
import com.openstrata.srs.domain.port.PolicyConsumerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Offline default {@link PolicyConsumerPort}. Logs Rule changes; production pushes to
 * ai-platform-api / ai-admin-service advanced guardrails (ARCH §5.2).
 */
@Component
public class LoggingPolicyConsumerAdapter implements PolicyConsumerPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingPolicyConsumerAdapter.class);

    @Override
    public void ruleChanged(Rule rule) {
        log.info("RuleChanged: {}@{} engine={} action={} enabled={}",
            rule.name(), rule.version(), rule.engine(), rule.action(), rule.enabled());
    }
}
