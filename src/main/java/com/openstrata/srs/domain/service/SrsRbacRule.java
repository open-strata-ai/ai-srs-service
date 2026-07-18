package com.openstrata.srs.domain.service;

import com.openstrata.srs.config.TenantContext;
import com.openstrata.srs.domain.DomainException;
import com.openstrata.srs.web.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * §7.2 / §4.7.3 — Skill/Rule-level RBAC aligned with the Auth SPI. {@code platform-admin}
 * bypasses role checks; otherwise the required role must be present. {@code tenant_id} is
 * mandatory in multi-tenant mode.
 */
@Component
public class SrsRbacRule {

    public void require(TenantContext.Tenant tenant, String role) {
        if (tenant == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "no tenant context");
        }
        if (tenant.platformAdmin()) return;
        if (tenant.roles() == null || !tenant.roles().contains(role)) {
            throw new DomainException(ErrorCode.FORBIDDEN, "requires role: " + role);
        }
    }
}
