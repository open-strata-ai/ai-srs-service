# ai-srs-service · AI Coding Rules & Skills (SKILLS)

> **Source**: Extracted from `design/DESIGN.md` §5 (Domain Rules), §11 (Integration Points), §12 (Security & Multi-tenancy). These rules guide AI-assisted development within this repo.

---

## RULE-01: Skill Semantic Versioning Enforcement

| Aspect | Detail |
| --- | --- |
| **Trigger** | Publishing or upgrading a Skill version. |
| **Constraint** | Skill versions MUST follow SemVer (`major.minor.patch`). Multiple versions of the same `name` co-exist; only `enabled=true` versions are exposed externally. Deleting a version that is still depended on is forbidden (must validate dependency graph first). |
| **Rationale** | Prevents breaking existing Agent workflows that depend on specific Skill versions. |

**Implementation pattern**:
```java
// Domain service: SkillVersionService
public void publish(Skill skill, String newVersion) {
    SemVer.parse(newVersion);  // validates semver format
    if (skill.isDependedOnByOthers()) {
        throw new SkillDepViolationException("Cannot delete; depended on by other skills");
    }
    // Canary release: previous versions remain; only enabled version is active
}
```

**Test checklist**:
- [ ] Invalid SemVer string → rejected.
- [ ] Delete depended-on version → `409 SKILL_DEP_VIOLATION`.
- [ ] Multiple versions with one `enabled=true` → correct routing.

---

## RULE-02: Skill Dependency DAG Resolution

| Aspect | Detail |
| --- | --- |
| **Trigger** | Publishing a Skill that declares `dependencies` or invoking `resolveDeps()`. |
| **Constraint** | The dependency graph MUST be cycle-free. All version ranges declared in dependencies MUST be satisfiable. Resolution failure blocks publish. |
| **Rationale** | Cyclic dependencies cause infinite loading loops at Agent runtime; unresolvable version ranges produce undefined behavior. |

**Implementation pattern**:
```java
// Domain service: SkillDepResolver
public DependencyGraph resolve(List<SkillDep> deps) {
    Graph graph = buildGraph(deps);
    if (graph.hasCycle()) {
        throw new SkillDepCycleException(graph.findCyclePath());
    }
    for (SkillDep dep : deps) {
        if (!versionExistsInRange(dep.getSkill(), dep.getVersionRange())) {
            throw new SkillDepVersionException(dep);
        }
    }
    return new DependencyGraph(graph.topologicalSort());
}
```

**Test checklist**:
- [ ] Three-skill cycle (A→B→C→A) → `422 SKILL_DEP_CYCLE`.
- [ ] Valid DAG → topological order returned.
- [ ] Version range `>=1.0.0 <2.0.0` but only `0.9.0` exists → `422 SKILL_DEP_VERSION`.

---

## RULE-03: Rule Engine Dispatch by Type

| Aspect | Detail |
| --- | --- |
| **Trigger** | Evaluating a Rule against Agent input. |
| **Constraint** | Route evaluation to the correct engine based on the Rule's `engine` field: `opa` → OPA (Rego) evaluation, `drools` → Drools rules engine. The `action` field determines consequence: `block` → reject execution, `warn` → log warning + continue, `log` → silent log only. |
| **Rationale** | Allows teams to choose the policy language that fits their use case; Rego for infrastructure rules, Drools for business logic rules. |

**Implementation pattern**:
```java
// Domain service: RuleEngineDispatch
public RuleResult evaluate(Rule rule, Map<String, Object> input) {
    RuleEngine engine = engineRegistry.get(rule.getEngine());  // opa or drools
    boolean matched = engine.evaluate(rule.getPolicy(), input);
    if (matched) {
        return switch (rule.getAction()) {
            case BLOCK -> RuleResult.block(rule.getRuleId());
            case WARN  -> RuleResult.warn(rule.getRuleId());
            case LOG   -> RuleResult.log(rule.getRuleId());
        };
    }
    return RuleResult.allow();
}
```

---

## RULE-04: Spec JSON Schema Validation

| Aspect | Detail |
| --- | --- |
| **Trigger** | Agent runtime calls `validateSpec()` with input or output data. |
| **Constraint** | Validate against the Spec's JSON Schema (`inputSchema` or `outputSchema` depending on `kind`). Validation failure throws `SpecValidationFailed` with detailed schema violation messages. |
| **Rationale** | Ensures Agent I/O conforms to declared contracts; prevents malformed data from propagating through agent pipelines. |

**Implementation pattern**:
```java
// Domain service: SpecValidator
public ValidationResult validate(Spec spec, JsonNode data) {
    JsonSchema schema = spec.getKind() == SpecKind.INPUT
        ? spec.getInputSchema() : spec.getOutputSchema();
    Set<ValidationMessage> errors = schema.validate(data);
    if (!errors.isEmpty()) {
        throw new SpecValidationFailedException(spec.getSpecId(), errors);
    }
    return ValidationResult.VALID;
}
```

---

## RULE-05: Skill/Rule RBAC Enforcement

| Aspect | Detail |
| --- | --- |
| **Trigger** | Any SRS CRUD operation. |
| **Constraint** | Skill and Rule operations require RBAC roles aligned with `Auth` SPI: `developer` can create, `admin` can enable Rules. Multi-tenant: `tenant_id` is STRICTLY enforced — no cross-tenant access. |
| **Rationale** | Prevents unauthorized modification of SRS that could affect all Agents in a tenant. |

**Implementation pattern**:
```java
@PreAuthorize("hasRole('developer') and #tenantId == authentication.tenantId")
public Skill createSkill(String tenantId, CreateSkillCommand cmd) { ... }

@PreAuthorize("hasRole('admin') and #tenantId == authentication.tenantId")
public Rule enableRule(String tenantId, String ruleId) { ... }
```

---

## RULE-06: Integration with Agent Engine (resolve flow)

| Aspect | Detail |
| --- | --- |
| **Trigger** | Agent engine calls `/api/v1/resolve?kind=skill&name=&ver=&tenant=`. |
| **Constraint** | Must validate Auth token → resolve Skill package from cache/DB → resolve DAG → return MCP-compatible Tool Schema. The response must be `AgentSpec`-compatible (§4.3.5). |
| **Rationale** | High-frequency call path; cache-hit path must be <10ms; DB fallback must be <100ms. |

**Checklist**:
- [ ] Auth token validation via `AuthPort`.
- [ ] Redis cache hit → <10ms response.
- [ ] Cache miss → DB query + DAG resolution → populate cache.
- [ ] Response schema matches `AgentSpec` Tool bindings.

---

## RULE-07: Advanced Guardrail Integration (PolicyConsumerPort)

| Aspect | Detail |
| --- | --- |
| **Trigger** | A Rule is evaluated and produces `BLOCK` or `WARN` action. |
| **Constraint** | Rule evaluation results MUST be reported to `ai-platform-api` / `ai-admin-service` via `PolicyConsumerPort` for centralized governance dashboard and audit (§14). |
| **Rationale** | Enables platform administrators to monitor rule effectiveness and adjust policies. |

**Implementation pattern**:
```java
// After rule evaluation:
if (result.isBlock() || result.isWarn()) {
    policyConsumerPort.reportRuleHit(tenantId, ruleId, result.getAction(), inputDigest);
}
```

---

## RULE-08: Tenant Data Isolation (SRS)

| Aspect | Detail |
| --- | --- |
| **Trigger** | Any SRS data access (read/write). |
| **Constraint** | All SRS tables use `tenant_id` column + RLS (§8.2). Object storage (MinIO) uses bucket-per-tenant for Skill packages. Cross-tenant SRS access is forbidden. |
| **Rationale** | SRS (especially Rules) often encode tenant-specific business logic; leakage across tenants is a security breach. |

**Implementation pattern**:
```java
// Repository queries must filter by tenant_id
@Query("SELECT s FROM SkillEntity s WHERE s.tenantId = :tenantId AND s.name = :name")
Optional<SkillEntity> findByName(@Param("tenantId") String tenantId, @Param("name") String name);
```

---

## RULE-09: Audit Trail for SRS Mutations

| Aspect | Detail |
| --- | --- |
| **Trigger** | Any SRS publish/enable/validate operation. |
| **Constraint** | SRS publish, enable, and validation events must write to `audit_log` (reusing control-plane audit semantics, §14.6 / §4.7.4). Advanced guardrail Rule hits must be reported to `ai-platform-api` for centralized audit. |
| **Rationale** | SRS changes directly impact Agent behavior; audit is required for compliance and debugging. |

---

## RULE-10: SPI Contract Enforcement (bump-spi-version)

| Aspect | Detail |
| --- | --- |
| **Trigger** | Upgrading dependencies (Keycloak, Redis/Valkey) or changing Port interfaces. |
| **Constraint** | Must update `bom.yaml` `interface_versions` and run contract tests: `AuthPort` against Keycloak, `CachePort` against Redis + Valkey. |
| **Rationale** | Multi-implementation Ports (Redis/Valkey) require verified compatibility. |

**Checklist**:
- [ ] Update `bom.yaml` version entries.
- [ ] Run `AuthPort` contract test.
- [ ] Run `CachePort` contract test (both Redis and Valkey).
- [ ] Run `SkillRegistryPort` contract test against `ai-tool-registry`.

---

> **References**: Full domain rules in `design/DESIGN.md` §5, §11, §12. Cross-reference `skills/SKILLS.md` in `ai-platform-api` for shared rules (bump-spi-version, tenant isolation).
