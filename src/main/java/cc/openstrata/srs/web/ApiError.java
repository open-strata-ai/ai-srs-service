package cc.openstrata.srs.web;

import cc.openstrata.srs.domain.DomainException;
import java.util.UUID;

/** Error envelope returned to clients (SPECS §1.4). */
public record ApiError(String code, String message, String traceId, String doc) {

    public static ApiError of(ErrorCode code, String message) {
        String trace = UUID.randomUUID().toString().substring(0, 8);
        return new ApiError(code.name(), message, trace,
            "https://docs.openstrata.cc/errors/" + code.name());
    }

    public static ApiError of(DomainException e) {
        return of(e.code(), e.getMessage());
    }
}
