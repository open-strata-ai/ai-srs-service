package com.openstrata.srs.domain;

import java.util.List;

/**
 * Spec aggregate root (ARCH §4.2). Holds input/output JSON Schemas and few-shot examples;
 * single version per {@code name × kind}.
 */
public record Spec(
    String specId,
    TenantId tenantId,
    String name,
    SpecKind kind,
    String inputSchemaJson,
    String outputSchemaJson,
    List<Example> examples) {

    public Spec {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("spec name must not be blank");
        }
        examples = examples == null ? List.of() : List.copyOf(examples);
    }
}
