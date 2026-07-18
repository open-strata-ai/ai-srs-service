package cc.openstrata.srs.application.dto;

import java.util.List;
import java.util.Map;

/** Register/publish a Skill (SPECS §1.2 POST /skills, POST /skills/{name}/versions). */
public record RegisterSkillRequest(
    String name,
    String version,
    String type,
    Map<String, Object> schema,
    List<DepDto> deps,
    boolean enabled,
    String packageRef) {}
