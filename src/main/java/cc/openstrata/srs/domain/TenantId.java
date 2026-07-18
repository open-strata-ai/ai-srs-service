package cc.openstrata.srs.domain;

/** Multi-tenant isolation key (SPECS §2.4); enforced in all queries. */
public record TenantId(String value) {
    public TenantId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
    }
}
