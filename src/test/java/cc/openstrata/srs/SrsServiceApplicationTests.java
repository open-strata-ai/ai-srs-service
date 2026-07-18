package cc.openstrata.srs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SrsServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context wires (H2 backend, in-memory adapters) offline.
    }
}
