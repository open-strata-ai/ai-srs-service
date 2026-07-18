package cc.openstrata.srs.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import cc.openstrata.srs.application.dto.CreateSpecRequest;
import cc.openstrata.srs.application.dto.SpecResponse;
import cc.openstrata.srs.application.dto.ValidationResponse;
import cc.openstrata.srs.config.TenantContext;
import cc.openstrata.srs.domain.service.SpecValidator;
import cc.openstrata.srs.infrastructure.JsonCodec;
import cc.openstrata.srs.infrastructure.persistence.SpecEntity;
import cc.openstrata.srs.infrastructure.persistence.SpecRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpecAppServiceTest {

    private final SpecRepository repo = mock(SpecRepository.class);
    private final SpecAppService svc =
        new SpecAppService(repo, new SpecValidator(), new JsonCodec(new ObjectMapper()));

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantContext.Tenant("t1", Set.of("platform-admin"), true));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createsSpec() {
        SpecResponse r = svc.create(new CreateSpecRequest(
            "search_io", "input",
            Map.of("required", List.of("query")), null, List.of()));
        assertEquals("search_io", r.name());
        assertEquals("INPUT", r.kind());
    }

    @Test
    void validatesAgainstStoredSchema() {
        String schemaJson = "{\"required\":[\"query\"],\"properties\":{\"query\":{\"type\":\"string\"}}}";
        when(repo.findByTenantIdAndSpecId("t1", "spec1")).thenReturn(Optional.of(
            new SpecEntity("spec1", "t1", "search_io", "INPUT", schemaJson, null, null)));

        ValidationResponse missing = svc.validate("spec1", Map.of());
        assertFalse(missing.valid());
        assertTrue(missing.violations().stream().anyMatch(v -> v.contains("query")));

        ValidationResponse ok = svc.validate("spec1", Map.of("query", "hello"));
        assertTrue(ok.valid());
    }
}
