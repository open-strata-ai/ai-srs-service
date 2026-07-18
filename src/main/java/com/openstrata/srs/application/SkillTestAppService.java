package com.openstrata.srs.application;

import com.openstrata.srs.application.dto.SkillTestResponse;
import com.openstrata.srs.config.OpenstrataProperties;
import com.openstrata.srs.config.TenantContext;
import com.openstrata.srs.domain.DomainException;
import com.openstrata.srs.infrastructure.persistence.SkillEntity;
import com.openstrata.srs.infrastructure.persistence.SkillRepository;
import com.openstrata.srs.infrastructure.persistence.SkillTestEntity;
import com.openstrata.srs.infrastructure.persistence.SkillTestRepository;
import com.openstrata.srs.web.ErrorCode;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Skill testing use case (ARCH §3.2, event {@code SkillTestCompleted}). Records a test run for
 * the given Skill version. Real Promptfoo execution is a documented gap; the skeleton records
 * a COMPLETED result synchronously.
 */
@Service
public class SkillTestAppService {

    private final SkillTestRepository testRepo;
    private final SkillRepository skillRepo;
    private final OpenstrataProperties props;

    public SkillTestAppService(SkillTestRepository testRepo,
                               SkillRepository skillRepo,
                               OpenstrataProperties props) {
        this.testRepo = testRepo;
        this.skillRepo = skillRepo;
        this.props = props;
    }

    @Transactional
    public SkillTestResponse runTests(String name, String version) {
        if (!props.getFeatures().getSkillTest().isEnabled()) {
            throw new DomainException(ErrorCode.BAD_REQUEST, "skill testing is disabled");
        }
        String tenant = TenantContext.tenantId();
        SkillEntity skill = skillRepo.findByTenantIdAndNameAndVersion(tenant, name, version)
            .orElseThrow(() -> new DomainException(ErrorCode.SKILL_NOT_FOUND,
                "skill not found: " + name + "@" + version));

        SkillTestEntity test = new SkillTestEntity(
            UUID.randomUUID().toString(), skill.getSkillId(), "COMPLETED",
            "{\"passed\":0,\"failed\":0,\"note\":\"promptfoo execution not wired (skeleton)\"}");
        testRepo.save(test);
        return new SkillTestResponse(test.getTestId(), test.getStatus());
    }
}
