package cc.openstrata.srs.infrastructure.adapter;

import cc.openstrata.srs.domain.port.AuthPort;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Offline default {@link AuthPort}. Decodes JWT claims without signature verification (the
 * gateway owns validation, auth-contract §2). Production swaps in a Keycloak adapter.
 */
@Component
public class LocalAuthAdapter implements AuthPort {

    @Override
    public Identity resolve(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) return null;
        try {
            String jwt = bearerToken.startsWith("Bearer ") ? bearerToken.substring(7) : bearerToken;
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;
            String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String tenantId = extract(json, "\"tenant_id\"");
            Set<String> roles = new HashSet<>();
            int idx = json.indexOf("\"roles\"");
            if (idx >= 0) {
                int a = json.indexOf('[', idx);
                int b = json.indexOf(']', a);
                if (a >= 0 && b > a) {
                    for (String piece : json.substring(a + 1, b).split(",")) {
                        String r = extract(piece, "\"");
                        if (r != null) roles.add(r);
                    }
                }
            }
            if (tenantId == null) return null;
            return new Identity(tenantId, roles);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String extract(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return null;
        int colon = json.indexOf(':', i);
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon);
        int q2 = json.indexOf('"', q1 + 1);
        if (q1 < 0 || q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }
}
