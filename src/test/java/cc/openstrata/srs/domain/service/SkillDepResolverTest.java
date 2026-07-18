package cc.openstrata.srs.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cc.openstrata.srs.domain.DomainException;
import cc.openstrata.srs.domain.SemVer;
import cc.openstrata.srs.domain.SkillDep;
import cc.openstrata.srs.web.ErrorCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SkillDepResolverTest {

    private final SkillDepResolver resolver = new SkillDepResolver();

    @Test
    void resolvesTopologicalOrder() {
        Map<String, List<SkillDep>> graph = Map.of(
            "a", List.of(new SkillDep("b", "^1.0.0")),
            "b", List.of(new SkillDep("c", "^1.0.0")),
            "c", List.of());
        Map<String, List<SemVer>> available = Map.of(
            "a", List.of(SemVer.parse("1.0.0")),
            "b", List.of(SemVer.parse("1.2.0")),
            "c", List.of(SemVer.parse("1.5.0")));

        List<String> order = resolver.resolve("a", graph, available);
        assertTrue(order.indexOf("c") < order.indexOf("b"));
        assertTrue(order.indexOf("b") < order.indexOf("a"));
    }

    @Test
    void detectsCycle() {
        Map<String, List<SkillDep>> graph = Map.of(
            "a", List.of(new SkillDep("b", "*")),
            "b", List.of(new SkillDep("a", "*")));
        Map<String, List<SemVer>> available = Map.of(
            "a", List.of(SemVer.parse("1.0.0")),
            "b", List.of(SemVer.parse("1.0.0")));

        DomainException ex = assertThrows(DomainException.class,
            () -> resolver.resolve("a", graph, available));
        assertEquals(ErrorCode.SKILL_DEP_CYCLE, ex.code());
    }

    @Test
    void detectsUnsatisfiableVersion() {
        Map<String, List<SkillDep>> graph = Map.of(
            "a", List.of(new SkillDep("b", "^2.0.0")));
        Map<String, List<SemVer>> available = Map.of(
            "a", List.of(SemVer.parse("1.0.0")),
            "b", List.of(SemVer.parse("1.4.0")));

        DomainException ex = assertThrows(DomainException.class,
            () -> resolver.resolve("a", graph, available));
        assertEquals(ErrorCode.SKILL_DEP_VERSION, ex.code());
    }
}
