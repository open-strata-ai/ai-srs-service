# ai-srs-service · Architecture Decision Document (ARCH)

> **Source**: Extracted from `design/DESIGN.md` §1, §2, §3, §6. Full design doc is the authority; this distillate captures architectural decisions, constraints, and SPI boundaries for implementers.

---

## 1. Service Identity

| Attribute | Value |
| --- | --- |
| Domain | agent-infra |
| Language / Framework | Java · Spring Boot 3.x (Jakarta Persistence) |
| Optional | Yes — optional, lit from standard profile onwards |
| Default Port | 8083 |
| Platform Version | v1.4.0 |
| Deployment | 2 replicas, `ai-system` namespace, 500m CPU / 1Gi request |
| Database | PostgreSQL@16.0 (core base), schema `srs` |

---

## 2. Bounded Context

`ai-srs-service` is OpenStrata's **Skills / Rules / Specs (SRS) Unified Management Platform** (Architecture Doc §7). It is responsible for SRS content **storage**, **version management**, **retrieval**, and **runtime validation**. It serves as the "policy & contract foundation" for Agent runtime: Agent engines load Skills, validate against Rules before execution, and verify input/output against Specs.

### 2.1 Profile-Gated Behavior

| Profile | srs.enabled | Description |
| --- | --- | --- |
| starter | false | Lightweight users can use pure YAML-declared SRS (§7 v2.1); no service deployment |
| standard | true | Basic SRS (Skills + Specs + OPA Rules); service deployed |
| advanced | true | + Advanced guardrails (Rules) linked to admin-service |
| full | true | Full SRS + Dify low-code reference can reuse Specs; Drools engine available |

### 2.2 Upstream Consumers

| Consumer | Call Pattern | Auth Type | Role |
| --- | --- | --- | --- |
| Agent Engine (AgentRuntime) | REST/gRPC `SrsResolver` | Bearer JWT + X-Tenant-Id | Load/validate Skills, Rules, Specs at runtime |
| `ai-portal-frontend` | REST `/api/v1/skills` | Bearer JWT + X-Tenant-Id | SRS management UI (CRUD) |
| `ai-platform-api` | REST | Bearer JWT | Approval/policy reference |
| `ai-admin-service` | REST | Bearer JWT | Advanced guardrail delivery (§14) |

### 2.3 Downstream Dependencies

| Dependency | Type | Purpose |
| --- | --- | --- |
| PostgreSQL | base persistence (core) | SRS content + version JSONB storage; `tenant_id` column isolation + RLS |
| Redis / Valkey | cache (core/optional) | SRS retrieval results, version cache (tenant key prefix) |
| MinIO (optional) | object storage | Skill packages, large example files (bucket per tenant) |
| `ai-tool-registry` (Go) | internal | Skill ↔ MCP Tool Schema mapping |
| Agent Engine | internal | Skill/Rule evaluation at runtime |
| `ai-platform-api` (Java) | internal | Policy reference, approval rules |
| `ai-admin-service` (Java) | internal | Advanced guardrail policy delivery |
| `ai-eval-service` (Python) | internal | Skill testing (Promptfoo) |

### 2.4 Boundary Rules
- **Inbound**: Authenticated via `Auth` SPI (Keycloak, §4.7.3). `tenant_id` enforced in multi-tenant mode.
- **Out-of-scope (NEVER in this service)**: Agent execution, model inference, runtime orchestration, data-plane operations.
- **Outbound**: Provides Skills/Rules/Specs to Agent engines via REST/SPI. All external calls through Port interfaces.
- **Optionality**: Single-tenant small teams can use pure YAML/code-declared SRS (§7 v2.1) without deploying this service.

---

## 3. Responsibility Matrix

### 3.1 Core Capabilities

| Capability | Description | Arch § | Critical Rules |
| --- | --- | --- | --- |
| Skill Registration | Declare tool name, description, parameter schema, return schema (MCP Tool Schema format). | §4.3.2 / §7.2 | Must validate MCP Tool Schema compliance |
| Skill Version Management | Semantic versioning (`major.minor.patch`); canary rollout by `enabled` flag. Multiple versions co-exist. | §7.2 | Cannot delete version depended on by other skills |
| Skill Dependency Management | DAG dependency resolution; cycle detection; version-range validation. | §7.2 | Block publish on cycle or unresolvable version |
| Skill Testing | Promptfoo automated test case execution and reporting. | §7.2 | Async execution; report stored in `skill_tests` table |
| Rule Definition/Engine | YAML DSL / OPA (Rego) / Drools policy definitions. Multi-engine coexistence. | §7.3 / §4.7.4 | Engine field routes to correct evaluator |
| Rule Version & Dry Run | Dry-run evaluation before activation; blocking vs warning vs log-only actions. | §7.3 | Dry run does NOT affect production agents |
| Spec Management | Input/output JSON Schema definitions; few-shot examples. | §7.4 | Schema must be valid JSON Schema draft-07+ |
| Spec Runtime Validation | Automatic input/output validation at Agent runtime. | §7.4 | Failure throws `SpecValidationFailed` with detailed messages |
| Retrieval & Loading | Agent runtime retrieval by `tenant_id`/name/version; Redis-cached. | §7.1 | Cache-hit <10ms; DB fallback <100ms |
| Permission Control | Skill/Rule-level RBAC aligned with `Auth` SPI roles. | §7.2 / §4.7.3 | `tenant_id` STRICTLY enforced in multi-tenant |

### 3.2 Application Layer Use Cases (from §4)

CQRS: Write uses Command services (strong consistency, version locking). Read uses Query services (Projections, Redis-cached).

| Use Case | App Service | TX Type | Domain Event |
| --- | --- | --- | --- |
| Register/publish Skill | `SkillAppService.register/publish()` | Write | `SkillPublished` |
| Skill version upgrade/canary | `SkillAppService.upgrade/gray()` | Write | `SkillPublished` |
| Resolve Skill dependencies | `SkillAppService.resolveDeps()` | Read | `SkillDepGraphResolved` |
| Define/dry-run Rule | `RuleAppService.define/dryRun()` | Write/Read | `RuleChanged` |
| Enable/disable Rule | `RuleAppService.enable()` | Write | `RuleChanged` |
| Create/validate Spec | `SpecAppService.create/validate()` | Write/Read | `SpecValidated` |
| Runtime SRS retrieval | `SrsQueryService.resolve()` | Read | — |
| Load Skill package | `SrsQueryService.loadSkillPackage()` | Read | — |
| Skill test (Promptfoo) | `SkillTestAppService.runTests()` | Write (async) | `SkillTestCompleted` |

---

## 4. Domain Model

### 4.1 Architecture Style

DDD four-layer architecture. Domain layer defines Port interfaces only; Infrastructure layer implements Adapters + ACL. CQRS separates write consistency (version-locked) from read speed (Redis-cached Projections).

### 4.2 Aggregate Design

| Aggregate Root | Consistency Boundary | Version Strategy | Key Invariants |
| --- | --- | --- | --- |
| `Skill` | `ToolSchema` + `SkillDep` list. Multiple versions co-exist under same `name`. | SemVer, multiple versions; `enabled` flag selects active. | Cannot delete version with active dependents; DAG must be cycle-free. |
| `Rule` | `RuleAction` + policy expression. One version active per `name`. | SemVer; `enabled` flag for activation. | Engine type must match a registered engine adapter. |
| `Spec` | `Example` list. Input/output schemas. | Single version per `name×kind` combination. | JSON Schema must validate against draft-07+ spec. |

### 4.3 Entities (Identity-Based)

| Entity | Identity Field | Key Attributes |
| --- | --- | --- |
| `Skill` | `SkillId` | name, version (SemVer), type, schema (JSONB), deps (JSONB), enabled, package_ref |
| `Rule` | `RuleId` | name, version, engine (opa/drools), policy (text), action (block/warn/log), enabled |
| `Spec` | `SpecId` | name, kind (input/output), inputSchema, outputSchema, examples (JSONB) |
| `SkillDep` | (embedded in Skill) | skill name, versionRange string |
| `Example` | (embedded in Spec) | input, expectedOutput |

### 4.4 Value Objects (Immutable)

| VO | Type | Constraints |
| --- | --- | --- |
| `SemVer` | String | `major.minor.patch` format; comparable |
| `TenantId` | String | Multi-tenant isolation key; enforced in all queries |
| `SkillType` | Enum | `mcp_tool`, `http_tool`, `builtin`, ... |
| `EngineType` | Enum | `opa` (Rego), `drools` |
| `RuleAction` | Enum | `block` (reject execution), `warn` (log + continue), `log` (silent log) |
| `JsonSchema` | Object | Valid JSON Schema draft-07+ object |
| `RateLimit` | Object | `{maxRequests, windowSeconds}` per skill |
| `SpecKind` | Enum | `input`, `output` |

### 4.5 Domain Events

| Event | Trigger | Consumer SPI | Side Effects |
| --- | --- | --- | --- |
| `SkillPublished` | Skill registered or new version published | `SkillRegistryPort` | Broadcast to Agent engine for runtime loading |
| `RuleChanged` | Rule created, enabled, or disabled | `PolicyConsumerPort` | Push to `ai-platform-api` / `ai-admin-service` advanced guardrails |
| `SpecValidated` | Input/output validation completed | (internal) | Result event for traceability and test reporting |
| `SkillDepGraphResolved` | DAG resolution completed successfully | (internal) | Dependencies loadable; cache populated |
| `SkillTestCompleted` | Promptfoo test run finished | (internal) | Test report written to `skill_tests` table |

### 4.6 Domain Services (Pure Logic)

| Domain Service | Responsibility | Key Rule |
| --- | --- | --- |
| `SkillVersionService` | Enforce SemVer constraints; manage version activation | `enabled=true` version is the active one; cannot delete depended versions |
| `SkillDepResolver` | Build DAG from dependency list; detect cycles and version-range conflicts | Block publish on cycle or unmatchable version range |
| `RuleEngineDispatch` | Route rule evaluation to correct engine by `engine` field | OPA for Rego policies; Drools for Java rules; multi-engine coexistence |
| `SpecValidator` | JSON Schema validation of Agent input/output data | Fail with `SpecValidationFailed` + detailed violation messages |
| `SrsRbacRule` | Skill/Rule-level RBAC enforcement | Align with `Auth` SPI roles; `tenant_id` mandatory |

---

## 5. SPI Ports & Adapters

### 5.1 Architecture Principle

Domain layer defines Port interfaces only. Infrastructure layer implements Adapters with ACL translation. Multiple implementations of same Port coexist; switching requires zero domain changes.

### 5.2 Port Inventory

| Port (domain interface) | SPI Port (bom.yaml) | Default Adapter | ACL Responsibility |
| --- | --- | --- | --- |
| `AuthPort` | `Auth` (§4.7.3) | **Keycloak@25.0.0** ✅ | token/claims → `TenantContext`/`Role` |
| `CachePort` | `Cache` (§4.3.4) | **Redis@7.4.0** ✅ / **Valkey@7.2.0** (optional) | SRS retrieval/version cache with tenant key prefix |
| `ObjectStorePort` | — (base object store) | MinIO (optional) | External object storage ⇄ internal `SkillPackage` |
| `SkillRegistryPort` | — (§4.3.2) | REST → `ai-tool-registry` / Agent engine | Internal `Skill` + `ToolSchema` ⇄ MCP Tool Schema format |
| `RuleEvalPort` | — (§7.3) | REST → Agent runtime / local OPA engine | Internal `Rule` policy ⇄ Rego/Drools evaluation |
| `PolicyConsumerPort` | — (§14 guardrails) | REST → `ai-platform-api` / `ai-admin-service` | Internal `Rule` + evaluation result ⇄ governance DTO |

### 5.3 Port Interaction Patterns

**Cached Resolution** (high-frequency, Agent runtime):
```
Agent → resolve(kind=skill, name, ver, tenant)
  → AuthPort.validate(token)
  → CachePort.get(key) [cache hit: <10ms return]
  → [cache miss: DB query + dep resolution + CachePort.set(key)] [<100ms]
  → return SkillPackage (MCP Tool Schema format)
```

**Event Broadcast** (Skill/Rule changes):
```
POST /skills + dependencies
  → SkillDepResolver.resolve(deps) [validate DAG]
  → persist Skill aggregate
  → SkillPublished event → SkillRegistryPort.broadcast(MCP Schema)
  → PolicyConsumerPort.notify (if affects guardrails)
```

**Engine Dispatch** (Rule evaluation at Agent runtime):
```
Agent → checkRules(input)
  → RuleEngineDispatch.evaluate(rule, input)
  → RuleEvalPort.evaluate(engine=opa/drools, policy, input)
  → return allow / block / warn
  → PolicyConsumerPort.reportRuleHit (if block/warn)
```

### 5.4 Multi-Implementation Strategy

| Scenario | Port | Strategy |
| --- | --- | --- |
| Primary + Alternative (P10) | `CachePort` | Redis (default) + Valkey (optional OSI). Both Spring beans; selected by `openstrata.spi.cache.provider`. |
| Multi-Engine (P11) | `RuleEngineDispatch` | OPA (Rego) + Drools coexist. Routed by Rule's `engine` field. Both engines registered as Spring beans. |
| Null-Object Skip (P10) | `ObjectStorePort` | When `objectStore.enabled=false`, NoOp adapter returns empty. |

### 5.5 External Dependencies (bom.yaml alignment)

| Integration Point | Type | Version | License | Scope | Port |
| --- | --- | --- | --- | --- | --- |
| Keycloak | External OSS | 25.0.0 | Apache-2.0 | core | AuthPort |
| Redis | External OSS | 7.4.0 | BSD-3 | core | CachePort |
| Valkey | External OSS | 7.2.0 | BSD-3 | optional | CachePort |
| PostgreSQL | External OSS | 16.0 | PostgreSQL | core base | — (direct JPA) |
| MinIO | External OSS | — | AGPL-3 | optional | ObjectStorePort |
| ai-tool-registry | Internal (Go) | v1.4.0 | internal | core | SkillRegistryPort |
| Agent Engine | Internal | v1.4.0 | internal | core | RuleEvalPort, resolve endpoint |
| ai-platform-api | Internal (Java) | v1.4.0 | internal | optional | PolicyConsumerPort |
| ai-admin-service | Internal (Java) | v1.4.0 | internal | optional | PolicyConsumerPort |
| ai-eval-service | Internal (Python) | v1.4.0 | internal | standard+ | Skill testing |

---

## 6. Key Architectural Decisions

| # | Decision | Rationale | Impact |
| --- | --- | --- | --- |
| ADR-1 | `Skill`, `Rule`, `Spec` as independent aggregates | Each has distinct lifecycle, versioning cadence, and publishing strategy. Skill publishing doesn't lock Rule table. | Can update Skills without affecting Rules; but cross-aggregate consistency is eventually consistent. |
| ADR-2 | Multi-version coexistence with `enabled` flag for canary | Allows gradual rollout: publish v1.1 (disabled) → test → enable → disable v1.0. No downtime. | Need cleanup strategy for deprecated versions; storage growth over time. |
| ADR-3 | DAG dependency resolution at publish time | Cycle detection and version-range validation run at publish, not at Agent load time. Faster runtime, slower publish. | Publish may fail with `SKILL_DEP_CYCLE` or `SKILL_DEP_VERSION`; publish flow must handle these errors gracefully. |
| ADR-4 | Multi-engine Rule dispatch (OPA/Drools) | Rego (OPA) for infrastructure/declarative rules; Drools for Java-ecosystem business rules. Both coexist by `engine` field. | Two engine implementations to maintain; engine selection is per-rule, not per-service. |
| ADR-5 | CQRS with Redis caching for read path | Agent runtime resolution (`resolve`) is high-frequency; Redis cache with tenant-prefixed keys provides <10ms cache-hit latency. | Must handle cache invalidation on SRS publish; stale cache could serve outdated Skills/Rules. |
| ADR-6 | Optional MinIO object store for large Skill packages | Small SRS stored in PostgreSQL JSONB; large packages (binaries, model references) go to MinIO with bucket-per-tenant isolation. | Two storage backends; `package_ref` column bridges JSONB metadata to object store location. |
| ADR-7 | `resolve` endpoint returns `AgentSpec`-compatible format | Ensures Agent Engine can consume SRS packages without translation. Response conforms to `AgentSpec` Tool binding schema (§4.3.5). | Must keep `resolve` response schema in sync with AgentSpec spec evolution. |

---

## 7. Service Boundary Diagram

```text
┌──────────────────────────────────────────────────────────────┐
│                    Upstream Consumers                         │
│  Agent Engine (AgentRuntime)  │  ai-portal-frontend           │
│  ai-platform-api              │  ai-admin-service             │
└────────────────────────────┬─────────────────────────────────┘
                             │ Auth'd, X-Tenant-Id header
┌────────────────────────────▼─────────────────────────────────┐
│                   ai-srs-service (8083)                        │
│  (optional: standard/advanced/full profiles)                   │
│                                                               │
│  ┌────────────────── Application Layer ──────────────────┐   │
│  │ SkillAppService       RuleAppService                    │   │
│  │ SpecAppService        SkillTestAppService (async)       │   │
│  │ SrsQueryService (CQRS Read, Redis-cached)               │   │
│  │ DTO↔Domain mapping, @Transactional for writes           │   │
│  └────────────────────────────────────────────────────────┘   │
│  ┌────────────────── Domain Layer (3) ────────────────────┐   │
│  │ Aggregates: Skill (root), Rule (root), Spec (root)     │   │
│  │ Entities: SkillDep, Example                             │   │
│  │ Value Objects: SemVer, SkillType, EngineType,           │   │
│  │   RuleAction, JsonSchema, RateLimit, SpecKind           │   │
│  │ Domain Services: SkillVersionService, SkillDepResolver  │   │
│  │   RuleEngineDispatch, SpecValidator, SrsRbacRule        │   │
│  │ Domain Events: SkillPublished, RuleChanged, ...         │   │
│  │                                                         │   │
│  │ Port Interfaces (pure Java, 6 ports):                   │   │
│  │ AuthPort │ CachePort │ ObjectStorePort                  │   │
│  │ SkillRegistryPort │ RuleEvalPort │ PolicyConsumerPort   │   │
│  └────────────────────────────────────────────────────────┘   │
│  ┌──────────────── Infrastructure Layer (4) ──────────────┐   │
│  │ Adapters:                                               │   │
│  │ KeycloakAdapter │ RedisAdapter/ValkeyAdapter            │   │
│  │ MinIOAdapter (optional) │ SkillRegistryAdapter          │   │
│  │ RuleEvalAdapter │ PolicyConsumerAdapter                 │   │
│  │ JPA: SkillEntity/RuleEntity/SpecEntity with @Convert    │   │
│  │ Flyway: V1__srs_init.sql, V2__rls.sql                   │   │
│  └────────────────────────────────────────────────────────┘   │
└────────────────────────────┬─────────────────────────────────┘
                             │ SPI/ACL calls (REST)
┌────────────────────────────▼─────────────────────────────────┐
│                  Downstream Dependencies                       │
│  Keycloak (Auth) │ Redis/Valkey (Cache) │ MinIO (optional)    │
│  PostgreSQL (Base) │ ai-tool-registry │ Agent Engine          │
│  ai-platform-api │ ai-admin-service │ ai-eval-service         │
└──────────────────────────────────────────────────────────────┘
```

---

> **References**:
> - Multi-version Skill management: §7.2 (semantic versioning, canary rollout, DAG dependency resolution)
> - Multi-engine Rule dispatch: §7.3 (OPA/Rego + Drools coexistence, engine field routing)
> - Full design: `design/DESIGN.md` (16 sections)
> - Architecture framework: `../../OpenStrata架构设计文档 v2.8.md` §7, §10.4, §15.6, §16
> - SPI contract tests: `skills/SKILLS.md` — `bump-spi-version` rule
> - OpenAPI spec: `specs/SPECS.md` — endpoint table and data model DDL
> - Runtime resolution SLA: cache-hit <10ms, DB fallback <100ms
