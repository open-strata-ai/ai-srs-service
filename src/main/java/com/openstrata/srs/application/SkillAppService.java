package com.openstrata.srs.application;

import com.openstrata.srs.application.dto.DepDto;
import com.openstrata.srs.application.dto.RegisterSkillRequest;
import com.openstrata.srs.application.dto.SkillResponse;
import com.openstrata.srs.config.TenantContext;
import com.openstrata.srs.domain.DomainException;
import com.openstrata.srs.domain.SemVer;
import com.openstrata.srs.domain.Skill;
import com.openstrata.srs.domain.SkillDep;
import com.openstrata.srs.domain.SkillType;
import com.openstrata.srs.domain.TenantId;
import com.openstrata.srs.domain.port.SkillRegistryPort;
import com.openstrata.srs.domain.service.SkillDepResolver;
import com.openstrata.srs.domain.service.SkillVersionService;
import com.openstrata.srs.infrastructure.JsonCodec;
import com.openstrata.srs.infrastructure.persistence.SkillEntity;
import com.openstrata.srs.infrastructure.persistence.SkillRepository;
import com.openstrata.srs.web.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Skill write/read use cases (ARCH §3.2). Register/publish multi-version Skills, resolve the
 * dependency DAG, and deprecate versions. Broadcasts {@code SkillPublished} via the registry
 * port.
 */
@Service
public class SkillAppService {

    private final SkillRepository repo;
    private final SkillVersionService versionService;
    private final SkillDepResolver depResolver;
    private final SkillRegistryPort registryPort;
    private final JsonCodec json;

    public SkillAppService(SkillRepository repo,
                           SkillVersionService versionService,
                           SkillDepResolver depResolver,
                           SkillRegistryPort registryPort,
                           JsonCodec json) {
        this.repo = repo;
        this.versionService = versionService;
        this.depResolver = depResolver;
        this.registryPort = registryPort;
        this.json = json;
    }

    @Transactional
    public SkillResponse register(RegisterSkillRequest req) {
        return create(req.name(), req);
    }

    @Transactional
    public SkillResponse publishVersion(String name, RegisterSkillRequest req) {
        return create(name, req);
    }

    private SkillResponse create(String name, RegisterSkillRequest req) {
        String tenant = tenant();
        SemVer version = SemVer.parse(req.version());
        List<SkillEntity> existing = repo.findByTenantIdAndName(tenant, name);
        versionService.assertNewVersion(version,
            existing.stream().map(e -> SemVer.parse(e.getVersion())).collect(Collectors.toList()));

        SkillType type = parseType(req.type());
        SkillEntity entity = new SkillEntity(
            UUID.randomUUID().toString(),
            tenant,
            name,
            version.toString(),
            type.name(),
            json.write(req.schema() == null ? Map.of() : req.schema()),
            json.write(req.deps() == null ? List.of() : req.deps()),
            req.enabled(),
            req.packageRef());
        repo.save(entity);

        registryPort.broadcast(toDomain(entity));
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public SkillResponse get(String name) {
        String tenant = tenant();
        List<SkillEntity> versions = repo.findByTenantIdAndName(tenant, name);
        if (versions.isEmpty()) {
            throw new DomainException(ErrorCode.SKILL_NOT_FOUND, "skill not found: " + name);
        }
        Skill active = versionService.activeVersion(
            versions.stream().map(this::toDomain).collect(Collectors.toList()));
        return toResponse(active);
    }

    @Transactional(readOnly = true)
    public SkillResponse getVersion(String name, String version) {
        String tenant = tenant();
        SkillEntity entity = repo.findByTenantIdAndNameAndVersion(tenant, name, version)
            .orElseThrow(() -> new DomainException(ErrorCode.SKILL_NOT_FOUND,
                "skill not found: " + name + "@" + version));
        return toResponse(entity);
    }

    @Transactional
    public SkillResponse deprecate(String name) {
        String tenant = tenant();
        List<SkillEntity> versions = repo.findByTenantIdAndName(tenant, name);
        if (versions.isEmpty()) {
            throw new DomainException(ErrorCode.SKILL_NOT_FOUND, "skill not found: " + name);
        }
        SkillEntity active = versions.stream().filter(SkillEntity::isEnabled).findFirst()
            .orElse(versions.get(0));
        active.setEnabled(false);
        repo.save(active);
        return toResponse(active);
    }

    /** Resolve the dependency DAG for a skill across the tenant's published skills (§7.2). */
    @Transactional(readOnly = true)
    public List<String> resolveDeps(String rootName) {
        String tenant = tenant();
        List<SkillEntity> all = repo.findByTenantId(tenant);
        if (all.stream().noneMatch(e -> e.getName().equals(rootName))) {
            throw new DomainException(ErrorCode.SKILL_NOT_FOUND, "skill not found: " + rootName);
        }
        Map<String, List<SkillDep>> graph = new java.util.HashMap<>();
        Map<String, List<SemVer>> available = new java.util.HashMap<>();
        for (SkillEntity e : all) {
            graph.computeIfAbsent(e.getName(), k -> new ArrayList<>()).addAll(depsOf(e));
            available.computeIfAbsent(e.getName(), k -> new ArrayList<>())
                .add(SemVer.parse(e.getVersion()));
        }
        return depResolver.resolve(rootName, graph, available);
    }

    private String tenant() {
        String t = TenantContext.tenantId();
        if (t == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "no tenant context");
        }
        return t;
    }

    private SkillType parseType(String raw) {
        if (raw == null) return SkillType.MCP_TOOL;
        try {
            return SkillType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException(ErrorCode.BAD_REQUEST, "unknown skill type: " + raw);
        }
    }

    private List<SkillDep> depsOf(SkillEntity e) {
        return json.readList(e.getDepsJson(), DepDto.class).stream()
            .map(d -> new SkillDep(d.skill(), d.versionRange()))
            .collect(Collectors.toList());
    }

    private Skill toDomain(SkillEntity e) {
        return new Skill(e.getSkillId(), new TenantId(e.getTenantId()), e.getName(),
            SemVer.parse(e.getVersion()), SkillType.valueOf(e.getType()),
            e.getSchemaJson(), depsOf(e), e.isEnabled(), e.getPackageRef());
    }

    private SkillResponse toResponse(SkillEntity e) {
        List<DepDto> deps = json.readList(e.getDepsJson(), DepDto.class);
        return new SkillResponse(e.getSkillId(), e.getName(), e.getVersion(), e.getType(),
            e.isEnabled(), deps, e.getPackageRef());
    }

    private SkillResponse toResponse(Skill s) {
        List<DepDto> deps = s.deps().stream()
            .map(d -> new DepDto(d.skill(), d.versionRange())).collect(Collectors.toList());
        return new SkillResponse(s.skillId(), s.name(), s.version().toString(),
            s.type().name(), s.enabled(), deps, s.packageRef());
    }
}
