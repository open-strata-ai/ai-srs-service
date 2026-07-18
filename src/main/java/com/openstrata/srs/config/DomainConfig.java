package com.openstrata.srs.config;

import com.openstrata.srs.domain.EngineType;
import com.openstrata.srs.domain.service.RuleEngineDispatch;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires domain services that need configuration. {@link RuleEngineDispatch} is built from the
 * enabled-engine feature flags (SPECS §3.1) rather than autowired, so it stays pure-domain.
 */
@Configuration
public class DomainConfig {

    @Bean
    public RuleEngineDispatch ruleEngineDispatch(OpenstrataProperties props) {
        Set<EngineType> enabled = EnumSet.noneOf(EngineType.class);
        if (props.getFeatures().getRuleEngine().getOpa().isEnabled()) enabled.add(EngineType.OPA);
        if (props.getFeatures().getRuleEngine().getDrools().isEnabled()) enabled.add(EngineType.DROOLS);
        return new RuleEngineDispatch(enabled);
    }
}
