package cc.openstrata.srs.application;

import cc.openstrata.srs.application.dto.CreateSpecRequest;
import cc.openstrata.srs.application.dto.SpecResponse;
import cc.openstrata.srs.application.dto.ValidationResponse;
import cc.openstrata.srs.config.TenantContext;
import cc.openstrata.srs.domain.DomainException;
import cc.openstrata.srs.domain.SpecKind;
import cc.openstrata.srs.domain.service.SpecValidator;
import cc.openstrata.srs.infrastructure.JsonCodec;
import cc.openstrata.srs.infrastructure.persistence.SpecEntity;
import cc.openstrata.srs.infrastructure.persistence.SpecRepository;
import cc.openstrata.srs.web.ErrorCode;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec write/validate use cases (ARCH §3.2). Create input/output Specs and validate Agent
 * runtime data against the stored JSON Schema via {@link SpecValidator}.
 */
@Service
public class SpecAppService {

    private final SpecRepository repo;
    private final SpecValidator validator;
    private final JsonCodec json;

    public SpecAppService(SpecRepository repo, SpecValidator validator, JsonCodec json) {
        this.repo = repo;
        this.validator = validator;
        this.json = json;
    }

    @Transactional
    public SpecResponse create(CreateSpecRequest req) {
        String tenant = tenant();
        SpecKind kind = parseKind(req.kind());
        SpecEntity entity = new SpecEntity(
            UUID.randomUUID().toString(), tenant, req.name(), kind.name(),
            json.write(req.inputSchema()), json.write(req.outputSchema()),
            json.write(req.examples()));
        repo.save(entity);
        return new SpecResponse(entity.getSpecId(), entity.getName(), entity.getKind());
    }

    /** Validate data against the Spec's schema for the given direction (§7.4). */
    @Transactional(readOnly = true)
    public ValidationResponse validate(String specId, Map<String, Object> data) {
        SpecEntity entity = repo.findByTenantIdAndSpecId(tenant(), specId)
            .orElseThrow(() -> new DomainException(ErrorCode.SPEC_NOT_FOUND, "spec not found: " + specId));
        String schemaJson = SpecKind.OUTPUT.name().equals(entity.getKind())
            ? entity.getOutputSchema() : entity.getInputSchema();
        Map<String, Object> schema = json.readMap(schemaJson);
        SpecValidator.Result r = validator.validate(schema, data);
        return new ValidationResponse(r.valid(), r.violations());
    }

    private String tenant() {
        String t = TenantContext.tenantId();
        if (t == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "no tenant context");
        }
        return t;
    }

    private SpecKind parseKind(String raw) {
        try {
            return SpecKind.valueOf(String.valueOf(raw).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException(ErrorCode.BAD_REQUEST, "unknown spec kind: " + raw);
        }
    }
}
