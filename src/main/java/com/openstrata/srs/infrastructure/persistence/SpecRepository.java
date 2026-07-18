package com.openstrata.srs.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecRepository extends JpaRepository<SpecEntity, String> {

    Optional<SpecEntity> findByTenantIdAndSpecId(String tenantId, String specId);

    Optional<SpecEntity> findByTenantIdAndNameAndKind(String tenantId, String name, String kind);
}
