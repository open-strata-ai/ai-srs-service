package com.openstrata.srs.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/** JPA mapping for the {@code skill_tests} table (SPECS §2.2). */
@Entity
@Table(name = "skill_tests")
public class SkillTestEntity {

    @Id
    @Column(name = "test_id", length = 64)
    private String testId;

    @Column(name = "skill_id", length = 64, nullable = false)
    private String skillId;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Lob
    @Column(name = "report")
    private String report;

    protected SkillTestEntity() {}

    public SkillTestEntity(String testId, String skillId, String status, String report) {
        this.testId = testId;
        this.skillId = skillId;
        this.status = status;
        this.report = report;
    }

    public String getTestId() { return testId; }
    public String getSkillId() { return skillId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReport() { return report; }
    public void setReport(String report) { this.report = report; }
}
