package com.openstrata.srs.domain.service;

import com.openstrata.srs.domain.DomainException;
import com.openstrata.srs.web.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * §7.4 — lightweight JSON-Schema (draft-07 subset) validator for Agent input/output. Checks
 * {@code required} presence and top-level property {@code type}. Failure raises
 * {@code SPEC_VALIDATION_FAILED} with detailed messages. (Full schema support is a documented
 * gap — production swaps in networknt/json-schema-validator.)
 */
@Component
public class SpecValidator {

    public record Result(boolean valid, List<String> violations) {}

    @SuppressWarnings("unchecked")
    public Result validate(Map<String, Object> schema, Map<String, Object> data) {
        List<String> violations = new ArrayList<>();
        if (schema == null) {
            return new Result(true, List.of());
        }
        Object required = schema.get("required");
        if (required instanceof List<?> req) {
            for (Object field : req) {
                if (data == null || !data.containsKey(String.valueOf(field))) {
                    violations.add("missing required field: " + field);
                }
            }
        }
        Object props = schema.get("properties");
        if (props instanceof Map<?, ?> properties && data != null) {
            for (Map.Entry<?, ?> e : properties.entrySet()) {
                String field = String.valueOf(e.getKey());
                if (!data.containsKey(field)) continue;
                if (e.getValue() instanceof Map<?, ?> spec) {
                    Object type = spec.get("type");
                    Object value = data.get(field);
                    if (type != null && value != null && !matchesType(String.valueOf(type), value)) {
                        violations.add("field " + field + " expected " + type
                            + " but got " + value.getClass().getSimpleName());
                    }
                }
            }
        }
        return new Result(violations.isEmpty(), violations);
    }

    /** Validate and throw on any violation. */
    public void validateOrThrow(Map<String, Object> schema, Map<String, Object> data) {
        Result r = validate(schema, data);
        if (!r.valid()) {
            throw new DomainException(ErrorCode.SPEC_VALIDATION_FAILED,
                String.join("; ", r.violations()));
        }
    }

    private boolean matchesType(String type, Object value) {
        return switch (type) {
            case "string" -> value instanceof String;
            case "integer" -> value instanceof Integer || value instanceof Long;
            case "number" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "object" -> value instanceof Map;
            case "array" -> value instanceof List;
            default -> true;
        };
    }
}
