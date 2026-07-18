package cc.openstrata.srs.application;

import cc.openstrata.srs.application.dto.ResolveResponse;
import cc.openstrata.srs.domain.DomainException;
import cc.openstrata.srs.domain.Skill;
import cc.openstrata.srs.domain.port.CachePort;
import cc.openstrata.srs.domain.service.SkillVersionService;
import cc.openstrata.srs.infrastructure.persistence.RuleEntity;
import cc.openstrata.srs.infrastructure.persistence.RuleRepository;
import cc.openstrata.srs.infrastructure.persistence.SkillEntity;
import cc.openstrata.srs.infrastructure.persistence.SkillRepository;
import cc.openstrata.srs.web.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CQRS read path (ARCH §3.2 / ADR-5). High-frequency Agent runtime resolution with a
 * tenant-prefixed cache in front of the DB fallback (cache-hit &lt;10ms target).
 */
@Service
public class SrsQueryService {

    private final SkillRepository skillRepo;
    private final RuleRepository ruleRepo;
    private final CachePort cache;
    private final SkillVersionService versionService;

    public SrsQueryService(SkillRepository skillRepo,
                           RuleRepository ruleRepo,
                           CachePort cache,
                           SkillVersionService versionService) {
        this.skillRepo = skillRepo;
        this.ruleRepo = ruleRepo;
        this.cache = cache;
        this.versionService = versionService;
    }

    @Transactional(readOnly = true)
    public ResolveResponse resolve(String kind, String name, String version, String tenant) {
        if (tenant == null || tenant.isBlank()) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "tenant required for resolve");
        }
        String key = tenant + ":" + kind + ":" + name + ":" + (version == null ? "active" : version);
        Optional<String> cached = cache.get(key);
        if (cached.isPresent()) {
            return new ResolveResponse(kind, name, version, true, cached.get());
        }

        String payload = switch (kind == null ? "" : kind.toLowerCase()) {
            case "skill" -> resolveSkill(tenant, name, version);
            case "rule" -> resolveRule(tenant, name);
            default -> throw new DomainException(ErrorCode.BAD_REQUEST, "unsupported kind: " + kind);
        };
        cache.put(key, payload);
        return new ResolveResponse(kind, name, version, false, payload);
    }

    private String resolveSkill(String tenant, String name, String version) {
        List<SkillEntity> versions = skillRepo.findByTenantIdAndName(tenant, name);
        if (versions.isEmpty()) {
            throw new DomainException(ErrorCode.SKILL_NOT_FOUND, "skill not found: " + name);
        }
        if (version != null) {
            return skillRepo.findByTenantIdAndNameAndVersion(tenant, name, version)
                .map(SkillEntity::getSchemaJson)
                .orElseThrow(() -> new DomainException(ErrorCode.SKILL_NOT_FOUND,
                    "skill not found: " + name + "@" + version));
        }
        Skill active = versionService.activeVersion(versions.stream().map(e ->
            new Skill(e.getSkillId(), new cc.openstrata.srs.domain.TenantId(e.getTenantId()),
                e.getName(), cc.openstrata.srs.domain.SemVer.parse(e.getVersion()),
                cc.openstrata.srs.domain.SkillType.valueOf(e.getType()), e.getSchemaJson(),
                List.of(), e.isEnabled(), e.getPackageRef())).collect(Collectors.toList()));
        return active.schemaJson();
    }

    private String resolveRule(String tenant, String name) {
        return ruleRepo.findByTenantId(tenant).stream()
            .filter(r -> r.getName().equals(name) && r.isEnabled())
            .map(RuleEntity::getPolicy)
            .findFirst()
            .orElseThrow(() -> new DomainException(ErrorCode.RULE_NOT_FOUND, "rule not found: " + name));
    }
}
