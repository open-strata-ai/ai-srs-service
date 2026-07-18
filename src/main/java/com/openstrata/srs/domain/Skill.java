package com.openstrata.srs.domain;

import java.util.List;

/**
 * Skill aggregate root (ARCH §4.2). Multiple versions co-exist under the same {@code name};
 * the version with {@code enabled=true} is the active one selected for canary rollout.
 */
public record Skill(
    String skillId,
    TenantId tenantId,
    String name,
    SemVer version,
    SkillType type,
    String schemaJson,
    List<SkillDep> deps,
    boolean enabled,
    String packageRef) {

    public Skill {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("skill name must not be blank");
        }
        deps = deps == null ? List.of() : List.copyOf(deps);
    }

    public Skill withEnabled(boolean value) {
        return new Skill(skillId, tenantId, name, version, type, schemaJson, deps, value, packageRef);
    }
}
