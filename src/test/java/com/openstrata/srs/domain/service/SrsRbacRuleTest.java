package com.openstrata.srs.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.openstrata.srs.config.TenantContext;
import com.openstrata.srs.domain.DomainException;
import com.openstrata.srs.web.ErrorCode;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SrsRbacRuleTest {

    private final SrsRbacRule rbac = new SrsRbacRule();

    @Test
    void platformAdminBypasses() {
        TenantContext.Tenant t = new TenantContext.Tenant("t", Set.of(), true);
        assertDoesNotThrow(() -> rbac.require(t, "developer"));
    }

    @Test
    void allowsWhenRolePresent() {
        TenantContext.Tenant t = new TenantContext.Tenant("t", Set.of("developer"), false);
        assertDoesNotThrow(() -> rbac.require(t, "developer"));
    }

    @Test
    void forbidsWhenRoleMissing() {
        TenantContext.Tenant t = new TenantContext.Tenant("t", Set.of("viewer"), false);
        DomainException ex = assertThrows(DomainException.class, () -> rbac.require(t, "admin"));
        assertEquals(ErrorCode.FORBIDDEN, ex.code());
    }
}
