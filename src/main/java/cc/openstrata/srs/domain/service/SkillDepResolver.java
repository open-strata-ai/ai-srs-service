package cc.openstrata.srs.domain.service;

import cc.openstrata.srs.domain.DomainException;
import cc.openstrata.srs.domain.SemVer;
import cc.openstrata.srs.domain.SkillDep;
import cc.openstrata.srs.web.ErrorCode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * ADR-3 / §7.2 — resolves a Skill's dependency DAG at publish time. Detects cycles
 * ({@code SKILL_DEP_CYCLE}) and unsatisfiable version ranges ({@code SKILL_DEP_VERSION}),
 * and returns a topological load order.
 */
@Component
public class SkillDepResolver {

    /**
     * @param root      the skill being published
     * @param graph     skill name → declared dependencies (must include {@code root})
     * @param available skill name → published versions (for range validation)
     * @return topologically ordered dependency names (dependencies before dependents)
     */
    public List<String> resolve(String root,
                                Map<String, List<SkillDep>> graph,
                                Map<String, List<SemVer>> available) {
        List<String> order = new ArrayList<>();
        Set<String> visiting = new LinkedHashSet<>();
        Set<String> done = new LinkedHashSet<>();
        dfs(root, graph, available, visiting, done, order);
        return order;
    }

    private void dfs(String node,
                     Map<String, List<SkillDep>> graph,
                     Map<String, List<SemVer>> available,
                     Set<String> visiting,
                     Set<String> done,
                     List<String> order) {
        if (done.contains(node)) return;
        if (!visiting.add(node)) {
            throw new DomainException(ErrorCode.SKILL_DEP_CYCLE,
                "Skill dependency contains cycle: " + String.join(" → ", visiting) + " → " + node);
        }
        for (SkillDep dep : graph.getOrDefault(node, List.of())) {
            List<SemVer> versions = available.getOrDefault(dep.skill(), List.of());
            boolean satisfied = versions.stream().anyMatch(v -> v.satisfies(dep.versionRange()));
            if (versions.isEmpty() || !satisfied) {
                throw new DomainException(ErrorCode.SKILL_DEP_VERSION,
                    "No version of " + dep.skill() + " satisfies range " + dep.versionRange());
            }
            dfs(dep.skill(), graph, available, visiting, done, order);
        }
        visiting.remove(node);
        done.add(node);
        if (!order.contains(node)) order.add(node);
    }
}
