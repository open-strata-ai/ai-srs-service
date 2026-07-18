package com.openstrata.srs.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.openstrata.srs.domain.DomainException;
import com.openstrata.srs.domain.SemVer;
import com.openstrata.srs.domain.Skill;
import com.openstrata.srs.domain.SkillDep;
import com.openstrata.srs.domain.SkillType;
import com.openstrata.srs.domain.TenantId;
import com.openstrata.srs.web.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;

class SkillVersionServiceTest {

    private final SkillVersionService svc = new SkillVersionService();

    private Skill skill(String name, String version, boolean enabled, List<SkillDep> deps) {
        return new Skill("id-" + name + version, new TenantId("t"), name,
            SemVer.parse(version), SkillType.MCP_TOOL, "{}", deps, enabled, null);
    }

    @Test
    void picksEnabledVersion() {
        Skill v1 = skill("s", "1.0.0", false, List.of());
        Skill v2 = skill("s", "2.0.0", true, List.of());
        assertEquals(v2, svc.activeVersion(List.of(v1, v2)));
    }

    @Test
    void fallsBackToHighestWhenNoneEnabled() {
        Skill v1 = skill("s", "1.0.0", false, List.of());
        Skill v2 = skill("s", "1.3.0", false, List.of());
        assertEquals(v2, svc.activeVersion(List.of(v1, v2)));
    }

    @Test
    void rejectsDuplicateVersion() {
        DomainException ex = assertThrows(DomainException.class, () ->
            svc.assertNewVersion(SemVer.parse("1.0.0"),
                List.of(SemVer.parse("1.0.0"), SemVer.parse("1.1.0"))));
        assertEquals(ErrorCode.VERSION_CONFLICT, ex.code());
    }

    @Test
    void blocksDeleteOfDependedVersion() {
        Skill target = skill("auth", "1.2.0", true, List.of());
        Skill dependent = skill("order", "1.0.0", true, List.of(new SkillDep("auth", "^1.0.0")));
        DomainException ex = assertThrows(DomainException.class,
            () -> svc.assertDeletable(target, List.of(target, dependent)));
        assertEquals(ErrorCode.VERSION_CONFLICT, ex.code());
    }
}
