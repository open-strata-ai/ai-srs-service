package cc.openstrata.srs.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** JPA mapping for the {@code skills} table (SPECS §2.2). JSON columns stored as text. */
@Entity
@Table(name = "skills",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "name", "version"}))
public class SkillEntity {

    @Id
    @Column(name = "skill_id", length = 64)
    private String skillId;

    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "version", length = 32, nullable = false)
    private String version;

    @Column(name = "type", length = 32, nullable = false)
    private String type;

    @Lob
    @Column(name = "schema_json", nullable = false)
    private String schemaJson;

    @Lob
    @Column(name = "deps_json")
    private String depsJson;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "package_ref", length = 256)
    private String packageRef;

    protected SkillEntity() {}

    public SkillEntity(String skillId, String tenantId, String name, String version, String type,
                       String schemaJson, String depsJson, boolean enabled, String packageRef) {
        this.skillId = skillId;
        this.tenantId = tenantId;
        this.name = name;
        this.version = version;
        this.type = type;
        this.schemaJson = schemaJson;
        this.depsJson = depsJson;
        this.enabled = enabled;
        this.packageRef = packageRef;
    }

    public String getSkillId() { return skillId; }
    public String getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getType() { return type; }
    public String getSchemaJson() { return schemaJson; }
    public String getDepsJson() { return depsJson; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getPackageRef() { return packageRef; }
}
