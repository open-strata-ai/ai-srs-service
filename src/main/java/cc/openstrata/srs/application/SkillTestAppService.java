package cc.openstrata.srs.application;

import cc.openstrata.srs.application.dto.SkillTestResponse;
import cc.openstrata.srs.config.OpenstrataProperties;
import cc.openstrata.srs.config.TenantContext;
import cc.openstrata.srs.domain.DomainException;
import cc.openstrata.srs.infrastructure.persistence.SkillEntity;
import cc.openstrata.srs.infrastructure.persistence.SkillRepository;
import cc.openstrata.srs.infrastructure.persistence.SkillTestEntity;
import cc.openstrata.srs.infrastructure.persistence.SkillTestRepository;
import cc.openstrata.srs.web.ErrorCode;
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
