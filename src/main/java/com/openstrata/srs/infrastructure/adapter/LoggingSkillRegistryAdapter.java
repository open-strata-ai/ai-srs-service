package com.openstrata.srs.infrastructure.adapter;

import com.openstrata.srs.domain.Skill;
import com.openstrata.srs.domain.port.SkillRegistryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Offline default {@link SkillRegistryPort}. Logs the broadcast; production posts the MCP Tool
 * Schema to ai-tool-registry / the Agent engine (ARCH §5.2).
 */
@Component
public class LoggingSkillRegistryAdapter implements SkillRegistryPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingSkillRegistryAdapter.class);

    @Override
    public void broadcast(Skill skill) {
        log.info("SkillPublished broadcast: {}@{} (tenant={})",
            skill.name(), skill.version(), skill.tenantId().value());
    }
}
