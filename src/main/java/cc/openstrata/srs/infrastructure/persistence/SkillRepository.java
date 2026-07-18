package cc.openstrata.srs.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<SkillEntity, String> {

    List<SkillEntity> findByTenantIdAndName(String tenantId, String name);

    Optional<SkillEntity> findByTenantIdAndNameAndVersion(String tenantId, String name, String version);

    List<SkillEntity> findByTenantId(String tenantId);
}
