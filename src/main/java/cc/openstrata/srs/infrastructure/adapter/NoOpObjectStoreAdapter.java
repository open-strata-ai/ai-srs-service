package cc.openstrata.srs.infrastructure.adapter;

import cc.openstrata.srs.config.OpenstrataProperties;
import cc.openstrata.srs.domain.port.ObjectStorePort;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Null-object {@link ObjectStorePort} (P10). When {@code openstrata.spi.object-store.enabled}
 * is false this is a no-op; otherwise it keeps packages in memory for offline runs (production
 * swaps in MinIO).
 */
@Component
public class NoOpObjectStoreAdapter implements ObjectStorePort {

    private final OpenstrataProperties props;
    private final ConcurrentMap<String, byte[]> store = new ConcurrentHashMap<>();

    public NoOpObjectStoreAdapter(OpenstrataProperties props) {
        this.props = props;
    }

    @Override
    public boolean enabled() {
        return props.getSpi().getObjectStore().isEnabled();
    }

    @Override
    public String put(String tenantId, String skillName, byte[] content) {
        if (!enabled()) return null;
        String ref = "os://" + tenantId + "/" + skillName;
        store.put(ref, content);
        return ref;
    }

    @Override
    public Optional<byte[]> get(String ref) {
        return Optional.ofNullable(store.get(ref));
    }
}
