package cc.openstrata.srs.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** JPA mapping for the {@code rules} table (SPECS §2.2). */
@Entity
@Table(name = "rules",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "name", "version"}))
public class RuleEntity {

    @Id
    @Column(name = "rule_id", length = 64)
    private String ruleId;

    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "version", length = 32, nullable = false)
    private String version;

    @Column(name = "engine", length = 16, nullable = false)
    private String engine;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "policy", nullable = false)
    private String policy;

    @Column(name = "action", length = 8, nullable = false)
    private String action;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    protected RuleEntity() {}

    public RuleEntity(String ruleId, String tenantId, String name, String version, String engine,
                      String policy, String action, boolean enabled) {
        this.ruleId = ruleId;
        this.tenantId = tenantId;
        this.name = name;
        this.version = version;
        this.engine = engine;
        this.policy = policy;
        this.action = action;
        this.enabled = enabled;
    }

    public String getRuleId() { return ruleId; }
    public String getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getEngine() { return engine; }
    public String getPolicy() { return policy; }
    public String getAction() { return action; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
