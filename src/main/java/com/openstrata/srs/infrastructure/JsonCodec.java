package com.openstrata.srs.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openstrata.srs.domain.DomainException;
import com.openstrata.srs.web.ErrorCode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Thin Jackson wrapper for (de)serializing JSONB-backed columns (schemas, deps, examples). */
@Component
public class JsonCodec {

    private final ObjectMapper mapper;

    public JsonCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String write(Object value) {
        if (value == null) return null;
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new DomainException(ErrorCode.BAD_REQUEST, "cannot serialize JSON: " + e.getMessage());
        }
    }

    public Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new DomainException(ErrorCode.BAD_REQUEST, "cannot parse JSON object: " + e.getMessage());
        }
    }

    public <T> List<T> readList(String json, Class<T> element) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json,
                mapper.getTypeFactory().constructCollectionType(List.class, element));
        } catch (Exception e) {
            throw new DomainException(ErrorCode.BAD_REQUEST, "cannot parse JSON array: " + e.getMessage());
        }
    }
}
