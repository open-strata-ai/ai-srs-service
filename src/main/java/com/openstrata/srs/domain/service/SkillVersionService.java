package com.openstrata.srs.domain.service;

import com.openstrata.srs.domain.DomainException;
import com.openstrata.srs.domain.SemVer;
import com.openstrata.srs.domain.Skill;
import com.openstrata.srs.web.ErrorCode;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * ADR-2 / §7.2 — SemVer constraints and canary activation. Multiple versions of a Skill
 * co-exist under the same {@code name}; the {@code enabled=true} version is active. A version
 * that other Skills still depend on cannot be deleted.
 */
@Component
public class SkillVersionService {

    /** The active (enabled) version among co-existing versions, or throw {@code SKILL_NOT_FOUND}. */
    public Skill activeVersion(List<Skill> versions) {
        if (versions == null || versions.isEmpty()) {
            throw new DomainException(ErrorCode.SKILL_NOT_FOUND, "no versions available");
        }
        return versions.stream()
            .filter(Skill::enabled)
            .findFirst()
            // Fall back to the highest version if none is explicitly enabled.
            .orElseGet(() -> versions.stream()
                .max((a, b) -> a.version().compareTo(b.version()))
                .orElseThrow(() ->
                    new DomainException(ErrorCode.SKILL_NOT_FOUND, "no versions available")));
    }

    /** True when publishing {@code candidate} would be a new version (not a duplicate). */
    public void assertNewVersion(SemVer candidate, Collection<SemVer> existing) {
        if (existing != null && existing.contains(candidate)) {
            throw new DomainException(ErrorCode.VERSION_CONFLICT,
                "version already exists: " + candidate);
        }
    }

    /** A version may not be deleted while another Skill depends on it (§7.2). */
    public void assertDeletable(Skill target, Collection<Skill> allSkills) {
        for (Skill s : allSkills) {
            if (s.name().equals(target.name())) continue;
            boolean depends = s.deps().stream().anyMatch(d ->
                d.skill().equals(target.name()) && target.version().satisfies(d.versionRange()));
            if (depends) {
                throw new DomainException(ErrorCode.VERSION_CONFLICT,
                    "cannot delete " + target.name() + "@" + target.version()
                        + ": depended on by " + s.name());
            }
        }
    }
}
