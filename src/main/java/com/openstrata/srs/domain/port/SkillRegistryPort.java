package com.openstrata.srs.domain.port;

import com.openstrata.srs.domain.Skill;

/**
 * Broadcasts published Skills to the Agent engine / ai-tool-registry in MCP Tool Schema
 * format (ARCH §5.2, event {@code SkillPublished}).
 */
public interface SkillRegistryPort {

    void broadcast(Skill skill);
}
