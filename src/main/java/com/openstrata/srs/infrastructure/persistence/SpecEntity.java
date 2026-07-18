package com.openstrata.srs.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** JPA mapping for the {@code specs} table (SPECS §2.2). */
@Entity
@Table(name = "specs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "name", "kind"}))
public class SpecEntity {

    @Id
    @Column(name = "spec_id", length = 64)
    private String specId;

    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "kind", length = 16, nullable = false)
    private String kind;

    @Lob
    @Column(name = "input_schema")
    private String inputSchema;

    @Lob
    @Column(name = "output_schema")
    private String outputSchema;

    @Lob
    @Column(name = "examples")
    private String examples;

    protected SpecEntity() {}

    public SpecEntity(String specId, String tenantId, String name, String kind,
                      String inputSchema, String outputSchema, String examples) {
        this.specId = specId;
        this.tenantId = tenantId;
        this.name = name;
        this.kind = kind;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.examples = examples;
    }

    public String getSpecId() { return specId; }
    public String getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getKind() { return kind; }
    public String getInputSchema() { return inputSchema; }
    public String getOutputSchema() { return outputSchema; }
    public String getExamples() { return examples; }
}
