package com.openstrata.srs.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openstrata.srs.domain.DomainException;
import com.openstrata.srs.domain.EngineType;
import com.openstrata.srs.domain.Rule;
import com.openstrata.srs.domain.RuleAction;
import com.openstrata.srs.domain.RuleDecision;
import com.openstrata.srs.domain.SemVer;
import com.openstrata.srs.domain.TenantId;
import com.openstrata.srs.web.ErrorCode;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuleEngineDispatchTest {

    private Rule rule(EngineType engine, RuleAction action) {
        return new Rule("r1", new TenantId("t"), "no_pii", SemVer.parse("1.0.0"),
            engine, "package srs", action, true);
    }

    @Test
    void routesToEnabledEngine() {
        RuleEngineDispatch dispatch = new RuleEngineDispatch(EnumSet.of(EngineType.OPA));
        assertTrue(dispatch.supports(EngineType.OPA));
        assertFalse(dispatch.supports(EngineType.DROOLS));

        RuleDecision d = dispatch.evaluate(rule(EngineType.OPA, RuleAction.BLOCK), Map.of(),
            (r, in) -> RuleDecision.of(r.action(), "ok"));
        assertEquals(RuleAction.BLOCK, d.action());
        assertFalse(d.allowed());
    }

    @Test
    void rejectsUnconfiguredEngine() {
        RuleEngineDispatch dispatch = new RuleEngineDispatch(EnumSet.of(EngineType.OPA));
        DomainException ex = assertThrows(DomainException.class,
            () -> dispatch.evaluate(rule(EngineType.DROOLS, RuleAction.WARN), Map.of(),
                (r, in) -> RuleDecision.allow()));
        assertEquals(ErrorCode.RULE_ENGINE_UNSUPPORTED, ex.code());
    }

    @Test
    void dryRunMapsAction() {
        RuleEngineDispatch dispatch = new RuleEngineDispatch(EnumSet.of(EngineType.OPA, EngineType.DROOLS));
        RuleDecision d = dispatch.dryRun(rule(EngineType.DROOLS, RuleAction.WARN));
        assertEquals(RuleAction.WARN, d.action());
        assertTrue(d.allowed());
    }
}
