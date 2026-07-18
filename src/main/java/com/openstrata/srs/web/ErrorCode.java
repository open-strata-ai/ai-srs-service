package com.openstrata.srs.web;

/**
 * Business error codes (SPECS §1.5). Each maps to a single HTTP status so the
 * {@link GlobalExceptionHandler} can render a consistent error envelope.
 */
public enum ErrorCode {
    SKILL_DEP_CYCLE(422),
    SKILL_DEP_VERSION(422),
    SKILL_NOT_FOUND(404),
    RULE_NOT_FOUND(404),
    SPEC_NOT_FOUND(404),
    VERSION_CONFLICT(409),
    SPEC_VALIDATION_FAILED(400),
    RULE_ENGINE_UNSUPPORTED(400),
    SRS_DISABLED(422),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    INTERNAL(500);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
