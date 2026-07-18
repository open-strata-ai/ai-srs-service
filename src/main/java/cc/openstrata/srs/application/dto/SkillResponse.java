package cc.openstrata.srs.application.dto;

import java.util.List;

/** Skill projection returned to clients. */
public record SkillResponse(
    String skillId,
    String name,
    String version,
    String type,
    boolean enabled,
    List<DepDto> deps,
    String packageRef) {}
