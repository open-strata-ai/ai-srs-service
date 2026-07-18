package com.openstrata.srs.application.dto;

import java.util.List;

/** Spec runtime validation result (SPECS §1.2 POST /specs/{id}:validate). */
public record ValidationResponse(boolean valid, List<String> violations) {}
