package cc.openstrata.srs.application;

import cc.openstrata.srs.application.dto.DecisionResponse;
import cc.openstrata.srs.application.dto.DefineRuleRequest;
import cc.openstrata.srs.application.dto.RuleResponse;
import cc.openstrata.srs.config.TenantContext;
import cc.openstrata.srs.domain.DomainException;
import cc.openstrata.srs.domain.EngineType;
import cc.openstrata.srs.domain.Rule;
import cc.openstrata.srs.domain.RuleAction;
import cc.openstrata.srs.domain.RuleDecision;
import cc.openstrata.srs.domain.SemVer;
import cc.openstrata.srs.domain.TenantId;
import cc.openstrata.srs.domain.port.PolicyConsumerPort;
import cc.openstrata.srs.domain.service.RuleEngineDispatch;
import cc.openstrata.srs.infrastructure.persistence.RuleEntity;
import cc.openstrata.srs.infrastructure.persistence.RuleRepository;
import cc.openstrata.srs.web.ErrorCode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rule write/read use cases (ARCH §3.2). Define rules, dry-run against the configured engine,
 * enable/disable, and list. Pushes {@code RuleChanged} via the policy consumer port.
 */
@Service
public class RuleAppService {

    private final RuleRepository repo;
    private final RuleEngineDispatch dispatch;
    private final PolicyConsumerPort policyConsumer;

    public RuleAppService(RuleRepository repo,
                          RuleEngineDispatch dispatch,
                          PolicyConsumerPort policyConsumer) {
        this.repo = repo;
        this.dispatch = dispatch;
        this.policyConsumer = policyConsumer;
    }

    @Transactional
    public RuleResponse define(DefineRuleRequest req) {
        String tenant = tenant();
        EngineType engine = parseEngine(req.engine());
        dispatch.assertSupported(engine);
        RuleAction action = parseAction(req.action());
        RuleEntity entity = new RuleEntity(
            UUID.randomUUID().toString(), tenant, req.name(),
            SemVer.parse(req.version()).toString(), engine.name(),
            req.policy(), action.name(), true);
        repo.save(entity);
        policyConsumer.ruleChanged(toDomain(entity));
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public DecisionResponse dryRun(String ruleId) {
        Rule rule = toDomain(load(ruleId));
        RuleDecision d = dispatch.dryRun(rule);
        return new DecisionResponse(d.action().name(), d.allowed(), d.message());
    }

    @Transactional
    public RuleResponse setEnabled(String ruleId, boolean enabled) {
        RuleEntity entity = load(ruleId);
        entity.setEnabled(enabled);
        repo.save(entity);
        policyConsumer.ruleChanged(toDomain(entity));
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<RuleResponse> list() {
        return repo.findByTenantId(tenant()).stream()
            .map(this::toResponse).collect(Collectors.toList());
    }

    private RuleEntity load(String ruleId) {
        return repo.findByTenantIdAndRuleId(tenant(), ruleId)
            .orElseThrow(() -> new DomainException(ErrorCode.RULE_NOT_FOUND, "rule not found: " + ruleId));
    }

    private String tenant() {
        String t = TenantContext.tenantId();
        if (t == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "no tenant context");
        }
        return t;
    }

    private EngineType parseEngine(String raw) {
        try {
            return EngineType.valueOf(String.valueOf(raw).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException(ErrorCode.RULE_ENGINE_UNSUPPORTED, "unknown engine: " + raw);
        }
    }

    private RuleAction parseAction(String raw) {
        try {
            return RuleAction.valueOf(String.valueOf(raw).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException(ErrorCode.BAD_REQUEST, "unknown action: " + raw);
        }
    }

    private Rule toDomain(RuleEntity e) {
        return new Rule(e.getRuleId(), new TenantId(e.getTenantId()), e.getName(),
            SemVer.parse(e.getVersion()), EngineType.valueOf(e.getEngine()),
            e.getPolicy(), RuleAction.valueOf(e.getAction()), e.isEnabled());
    }

    private RuleResponse toResponse(RuleEntity e) {
        return new RuleResponse(e.getRuleId(), e.getName(), e.getVersion(),
            e.getEngine(), e.getAction(), e.isEnabled());
    }
}
