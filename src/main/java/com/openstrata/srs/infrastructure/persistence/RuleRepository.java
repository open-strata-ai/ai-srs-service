package com.openstrata.srs.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository extends JpaRepository<RuleEntity, String> {

    List<RuleEntity> findByTenantId(String tenantId);

    Optional<RuleEntity> findByTenantIdAndRuleId(String tenantId, String ruleId);
}
