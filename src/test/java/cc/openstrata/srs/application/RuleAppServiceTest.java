package cc.openstrata.srs.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cc.openstrata.srs.application.dto.DecisionResponse;
import cc.openstrata.srs.application.dto.DefineRuleRequest;
import cc.openstrata.srs.application.dto.RuleResponse;
import cc.openstrata.srs.config.TenantContext;
import cc.openstrata.srs.domain.DomainException;
import cc.openstrata.srs.domain.EngineType;
import cc.openstrata.srs.domain.port.PolicyConsumerPort;
import cc.openstrata.srs.domain.service.RuleEngineDispatch;
import cc.openstrata.srs.infrastructure.persistence.RuleEntity;
import cc.openstrata.srs.infrastructure.persistence.RuleRepository;
import cc.openstrata.srs.web.ErrorCode;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RuleAppServiceTest {

    private final RuleRepository repo = mock(RuleRepository.class);
    private final PolicyConsumerPort policyConsumer = mock(PolicyConsumerPort.class);

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.Tenant("t1", Set.of("platform-admin"), true));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private RuleAppService service(EnumSet<EngineType> engines) {
        return new RuleAppService(repo, new RuleEngineDispatch(engines), policyConsumer);
    }

    @Test
    void definesRuleWithEnabledEngine() {
        RuleAppService svc = service(EnumSet.of(EngineType.OPA));
        RuleResponse r = svc.define(new DefineRuleRequest("no_pii", "1.0.0", "opa", "package srs", "block"));
        assertEquals("OPA", r.engine());
        assertEquals("BLOCK", r.action());
        assertTrue(r.enabled());
    }

    @Test
    void rejectsUnconfiguredEngine() {
        RuleAppService svc = service(EnumSet.of(EngineType.OPA));
        DomainException ex = assertThrows(DomainException.class,
            () -> svc.define(new DefineRuleRequest("r", "1.0.0", "drools", "rule x", "warn")));
        assertEquals(ErrorCode.RULE_ENGINE_UNSUPPORTED, ex.code());
    }

    @Test
    void dryRunReturnsDecision() {
        when(repo.findByTenantIdAndRuleId("t1", "r1")).thenReturn(Optional.of(
            new RuleEntity("r1", "t1", "warn_pii", "1.0.0", "OPA", "package srs", "WARN", true)));
        RuleAppService svc = service(EnumSet.of(EngineType.OPA));
        DecisionResponse d = svc.dryRun("r1");
        assertEquals("WARN", d.action());
        assertTrue(d.allowed());
    }
}
