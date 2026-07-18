package com.openstrata.srs.domain;

/** A declared dependency on another Skill by name + version range (SPECS §2.2 deps JSONB). */
public record SkillDep(String skill, String versionRange) {
    public SkillDep {
        if (skill == null || skill.isBlank()) {
            throw new IllegalArgumentException("dependency skill name must not be blank");
        }
    }
}
