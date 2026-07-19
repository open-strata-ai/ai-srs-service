-- ai-srs-service schema (schema: srs). JSON columns stored as TEXT (portable).
-- Flyway owns the schema (spring.jpa.hibernate.ddl-auto=validate in all profiles).
-- Flyway is configured with schemas: srs, so unqualified names land in `srs`.

CREATE TABLE IF NOT EXISTS specs (
  spec_id       VARCHAR(64) PRIMARY KEY,
  tenant_id     VARCHAR(64) NOT NULL,
  name          VARCHAR(128) NOT NULL,
  kind          VARCHAR(16)  NOT NULL,
  input_schema  TEXT,
  output_schema TEXT,
  examples      TEXT,
  CONSTRAINT uq_specs_tenant_name_kind UNIQUE (tenant_id, name, kind)
);

CREATE TABLE IF NOT EXISTS rules (
  rule_id    VARCHAR(64) PRIMARY KEY,
  tenant_id  VARCHAR(64) NOT NULL,
  name       VARCHAR(128) NOT NULL,
  version    VARCHAR(32)  NOT NULL,
  engine     VARCHAR(16)  NOT NULL,
  policy     TEXT         NOT NULL,
  action     VARCHAR(8)   NOT NULL,
  enabled    BOOLEAN      NOT NULL,
  CONSTRAINT uq_rules_tenant_name_version UNIQUE (tenant_id, name, version)
);

CREATE TABLE IF NOT EXISTS skills (
  skill_id     VARCHAR(64) PRIMARY KEY,
  tenant_id    VARCHAR(64) NOT NULL,
  name         VARCHAR(128) NOT NULL,
  version      VARCHAR(32)  NOT NULL,
  type         VARCHAR(32)  NOT NULL,
  schema_json  TEXT         NOT NULL,
  deps_json    TEXT,
  enabled      BOOLEAN      NOT NULL,
  package_ref  VARCHAR(256),
  CONSTRAINT uq_skills_tenant_name_version UNIQUE (tenant_id, name, version)
);

CREATE TABLE IF NOT EXISTS skill_tests (
  test_id   VARCHAR(64) PRIMARY KEY,
  skill_id  VARCHAR(64) NOT NULL,
  status    VARCHAR(16) NOT NULL,
  report    TEXT
);
