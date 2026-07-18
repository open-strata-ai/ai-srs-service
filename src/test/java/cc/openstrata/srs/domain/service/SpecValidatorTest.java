package cc.openstrata.srs.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cc.openstrata.srs.domain.DomainException;
import cc.openstrata.srs.web.ErrorCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SpecValidatorTest {

    private final SpecValidator validator = new SpecValidator();

    private Map<String, Object> schema() {
        return Map.of(
            "required", List.of("query"),
            "properties", Map.of(
                "query", Map.of("type", "string"),
                "limit", Map.of("type", "integer")));
    }

    @Test
    void acceptsValidData() {
        SpecValidator.Result r = validator.validate(schema(), Map.of("query", "hi", "limit", 5));
        assertTrue(r.valid());
        assertTrue(r.violations().isEmpty());
    }

    @Test
    void rejectsMissingRequired() {
        SpecValidator.Result r = validator.validate(schema(), Map.of("limit", 5));
        assertFalse(r.valid());
        DomainException ex = assertThrows(DomainException.class,
            () -> validator.validateOrThrow(schema(), Map.of("limit", 5)));
        assertEquals(ErrorCode.SPEC_VALIDATION_FAILED, ex.code());
    }

    @Test
    void rejectsWrongType() {
        SpecValidator.Result r = validator.validate(schema(), Map.of("query", 42));
        assertFalse(r.valid());
    }
}
