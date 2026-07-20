package cc.openstrata.srs.web;

import cc.openstrata.srs.application.RuleAppService;
import cc.openstrata.srs.application.SkillAppService;
import cc.openstrata.srs.application.SkillTestAppService;
import cc.openstrata.srs.application.SpecAppService;
import cc.openstrata.srs.application.SrsQueryService;
import cc.openstrata.srs.application.dto.CreateSpecRequest;
import cc.openstrata.srs.application.dto.DecisionResponse;
import cc.openstrata.srs.application.dto.DefineRuleRequest;
import cc.openstrata.srs.application.dto.RegisterSkillRequest;
import cc.openstrata.srs.application.dto.ResolveResponse;
import cc.openstrata.srs.application.dto.RuleResponse;
import cc.openstrata.srs.application.dto.SkillResponse;
import cc.openstrata.srs.application.dto.SkillTestResponse;
import cc.openstrata.srs.application.dto.SpecResponse;
import cc.openstrata.srs.application.dto.ValidationResponse;
import cc.openstrata.srs.config.OpenstrataProperties;
import cc.openstrata.srs.domain.DomainException;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** SRS REST API (SPECS §1.2). All endpoints are gated on {@code srs.enabled}. */
@RestController
@RequestMapping("/api/v1")
public class SrsController {

    private final OpenstrataProperties props;
    private final SkillAppService skills;
    private final RuleAppService rules;
    private final SpecAppService specs;
    private final SrsQueryService query;
    private final SkillTestAppService tests;

    public SrsController(OpenstrataProperties props,
                         SkillAppService skills,
                         RuleAppService rules,
                         SpecAppService specs,
                         SrsQueryService query,
                         SkillTestAppService tests) {
        this.props = props;
        this.skills = skills;
        this.rules = rules;
        this.specs = specs;
        this.query = query;
        this.tests = tests;
    }

    /** SRS is optional (SPECS §3.2): reject when the profile has it disabled. */
    private void requireSrs() {
        if (!props.getFeatures().getSrs().isEnabled()) {
            throw new DomainException(ErrorCode.SRS_DISABLED,
                "SRS service is disabled for this profile (starter)");
        }
    }

    // ---- Skills ----

    @PostMapping("/skills")
    public SkillResponse register(@RequestBody RegisterSkillRequest req) {
        requireSrs();
        return skills.register(req);
    }

    @PostMapping("/skills/{name}/versions")
    public SkillResponse publishVersion(@PathVariable String name,
                                        @RequestBody RegisterSkillRequest req) {
        requireSrs();
        return skills.publishVersion(name, req);
    }

    @GetMapping("/skills/{name}")
    public SkillResponse getSkill(@PathVariable String name) {
        requireSrs();
        return skills.get(name);
    }

    /** Tenant-scoped skill name listing (used by ai-platform-api SrsPort). */
    @GetMapping("/skills")
    public List<String> listSkills(@RequestParam String tenant) {
        requireSrs();
        return skills.listNames(tenant);
    }

    @GetMapping("/skills/{name}/versions/{ver}")
    public SkillResponse getSkillVersion(@PathVariable String name, @PathVariable String ver) {
        requireSrs();
        return skills.getVersion(name, ver);
    }

    @PostMapping("/skills/{name}:deprecate")
    public SkillResponse deprecate(@PathVariable String name) {
        requireSrs();
        return skills.deprecate(name);
    }

    @PostMapping("/skills:resolve-deps")
    public Map<String, Object> resolveDeps(@RequestBody Map<String, String> body) {
        requireSrs();
        List<String> order = skills.resolveDeps(body.get("skill"));
        return Map.of("order", order);
    }

    @PostMapping("/skills/{name}/versions/{ver}:test")
    public SkillTestResponse test(@PathVariable String name, @PathVariable String ver) {
        requireSrs();
        return tests.runTests(name, ver);
    }

    // ---- Rules ----

    @GetMapping("/rules")
    public List<RuleResponse> listRules() {
        requireSrs();
        return rules.list();
    }

    @PostMapping("/rules")
    public RuleResponse defineRule(@RequestBody DefineRuleRequest req) {
        requireSrs();
        return rules.define(req);
    }

    @PostMapping("/rules/{id}:dry-run")
    public DecisionResponse dryRun(@PathVariable String id) {
        requireSrs();
        return rules.dryRun(id);
    }

    @PatchMapping("/rules/{id}")
    public RuleResponse patchRule(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        requireSrs();
        return rules.setEnabled(id, Boolean.TRUE.equals(body.get("enabled")));
    }

    // ---- Specs ----

    @PostMapping("/specs")
    public SpecResponse createSpec(@RequestBody CreateSpecRequest req) {
        requireSrs();
        return specs.create(req);
    }

    @PostMapping("/specs/{id}:validate")
    public ValidationResponse validateSpec(@PathVariable String id,
                                           @RequestBody Map<String, Object> data) {
        requireSrs();
        return specs.validate(id, data);
    }

    // ---- Runtime resolution ----

    @GetMapping("/resolve")
    public ResolveResponse resolve(@RequestParam String kind,
                                   @RequestParam String name,
                                   @RequestParam(required = false) String ver,
                                   @RequestParam String tenant) {
        requireSrs();
        return query.resolve(kind, name, ver, tenant);
    }
}
