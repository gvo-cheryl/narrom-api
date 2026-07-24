# Naroom API Repository Instructions

## Instruction Structure

Claude Code loads the following repository instructions:

- `CLAUDE.md`: backend project and development rules
- `.claude/CLAUDE.md`: OMC operating rules
- `docs/PRODUCT_CONTEXT.md`: approved Naroom product context

This repository currently has no `AGENTS.md`.
Do not add an `@AGENTS.md` import unless that file is intentionally created later.

## Project Overview

- Project: `naroom-api`
- Role: backend API for the Naroom mobile application
- Base package: `com.naroom.api`
- Normal development branch: `dev`
- Stable branch: `main`
- Product context: `docs/PRODUCT_CONTEXT.md`

Naroom is a long-term self-understanding platform that connects personal records with reflection, pattern discovery, and small changes explicitly chosen by the user.

Before implementing product behavior, read `docs/PRODUCT_CONTEXT.md` and the current GitHub Issue or user-provided requirements.

Determine scope and acceptance criteria in this order:

1. The user's current explicit instructions
2. The current GitHub Issue or task requirements
3. Approved product documents, ADRs, API contracts, and migrations
4. Existing code and tests
5. These repository-wide instructions

If a conflict affects behavior, schema, security, or public API compatibility, do not choose silently. Report the conflict and request a decision.

## Current Technical Baseline

- Java 21
- Spring Boot 4.1.0
- Gradle 9.5.1
- Gradle Kotlin DSL
- Gradle Wrapper
- Spring Web
- Bean Validation
- Spring Boot Actuator
- Spring Data JPA
- Supabase PostgreSQL
- Flyway
- JUnit 5
- Spring Boot Test

Use `build.gradle.kts`, the actual Gradle state, configuration files, and existing code as the source of truth for installed dependencies.

Do not assume Spring Security, Springdoc OpenAPI, Kakao integration, Redis, or any external API is installed until it is actually present in the project.

Do not add or upgrade dependencies, plugins, infrastructure, or external integrations that are unrelated to the current task.

## Important Paths

- Application code: `src/main/java/com/naroom/api`
- Test code: `src/test/java/com/naroom/api`
- Configuration: `src/main/resources`
- Flyway migrations: `src/main/resources/db/migration`
- Product context: `docs/PRODUCT_CONTEXT.md`
- Draft API contracts: `docs/contracts/drafts`
- Generated OpenAPI snapshots: `docs/openapi`
- Architecture documents: `docs/architecture`
- Domain documents: `docs/domain`
- ADRs: `docs/adr`
- Application health API: `GET /api/v1/health`
- Actuator health API: `GET /actuator/health`

If an approved path already serves the same purpose, do not create a duplicate directory.

## Local Development

```bash
./gradlew bootRun
```

The default local profile is `local`.

The application may load `.env.local` while running. Claude Code may use approved execution commands, but it must not read, print, summarize, modify, or expose the contents of `.env.local`.

## Build and Verification

Use the Gradle Wrapper.

```bash
./gradlew test
./gradlew clean build
```

After changing code:

1. Run focused tests for the changed behavior.
2. Run `./gradlew test`.
3. Run `./gradlew clean build`.
4. If the public API contract changed, regenerate and validate OpenAPI.
5. Review the final diff.
6. Report changed files, commands run, and results.
7. Report any validation that could not be run or failed, with the reason.

Do not report the task as complete when required verification has failed.

## Scope Management

Before editing:

1. Read the current Issue or user request.
2. Inspect the relevant product documents, code, tests, migrations, and API contracts.
3. Separate included scope from excluded scope.
4. Briefly state the files expected to change and the verification plan.

When the request is clear and within the approved scope, do not wait for an additional approval.

Stop and ask for clarification when:

- Product documents materially conflict.
- A public API decision is not approved.
- Applied migrations conflict with the requested entity model.
- A security policy is undecided.
- An unapproved external service or secret is required.
- The requested scope must expand substantially.

Do not perform unrelated refactoring, renaming, cleanup, dependency upgrades, or architecture redesign.

## Package and Architecture Rules

- Place business APIs under `/api/v1`.
- Actuator and operational management endpoints are exceptions.
- Use feature-based packages under `com.naroom.api`.
- Put only genuinely cross-cutting concerns shared by multiple features in `global`.
- Do not place feature-specific logic in generic common or utility packages.
- Prefer constructor injection.
- Prefer Bean Validation for request validation.
- Keep Controllers focused on HTTP concerns.
- Do not place business logic in Controllers.
- Define transaction boundaries in the Service or Application layer.
- Separate request DTOs, response DTOs, domain models, and persistence Entities.
- Never expose JPA Entities directly from Controllers.
- Java records may be used for appropriate immutable request and response models.
- Use descriptive English names for classes, methods, fields, and variables.
- Follow the existing code style and approved project structure.
- Explain the reason and impact when changing a public request or response contract.

## Code Comment Rules

Default to no comments. Add one only when it earns its place:

- A DB-level constraint or meaning that is not visible from the Java code alone (a CHECK constraint spanning two columns, a column that is only non-null in one specific state, a nullable FK caused by `ON DELETE SET NULL`, etc.).
- The reasoning behind a non-obvious choice (why a field is `Short` instead of `Integer` to match a `smallint` column and pass `ddl-auto=validate`, why a value is fixed instead of environment-specific, why two similar-looking fields are conceptually different).
- Deferred or incomplete work and known caveats, prefixed with `// TODO:` (why something is intentionally minimal now, what has to be added when a later feature lands, a known limitation).
- A one-line class-level comment or short `/** */`, only when the class's role is not already clear from its name and package.

Do not add comments that restate what the code already says: plain getters/setters, obvious field declarations, or a comment that just repeats a well-named method's name. No multi-paragraph Javadoc and no `@param`/`@return` boilerplate for internal (non-published-library) code — this is application code, not a library API.

Prefer a single-line `//` right above the relevant field/line. Use a short `/** */` only for the rare class-level note. No emoji.

## API Contract and Documentation Rules

A public API contract includes:

- Endpoint path and HTTP method
- Authentication requirements
- Request and response schemas
- Required, optional, and nullable fields
- Validation constraints
- Enum values
- Error codes and error schema
- Date, time, and timezone formats
- Pagination rules
- Compatibility and deprecation policy

Work on public APIs in this order:

1. Approve the API contract or create a draft.
2. Implement the Controller, DTOs, validation, and behavior.
3. Add focused and integration tests.
4. Generate the OpenAPI document.
5. Validate the generated document.
6. Manage the approved snapshot as a commit target according to the documentation automation policy.

Rules:

- OpenAPI owns the executable HTTP contract.
- Markdown explains concepts, policies, flows, and decisions.
- ADRs preserve important architecture decisions and their rationale.
- Flyway owns database schema history.
- Never manually edit generated OpenAPI.
- Do not duplicate detailed endpoint schemas in general Markdown documents.
- Add OpenAPI annotations only when code, types, and validation cannot express the contract clearly enough.
- Do not leave important design results only in terminal output or `.omc/`.

## Mobile API Compatibility

The backend and mobile application may be deployed at different times.

- Prefer additive, backward-compatible changes.
- Do not immediately delete or rename existing fields.
- Do not change the meaning of existing fields.
- When compatibility is required, introduce new fields as optional.
- Consider application fallback behavior when adding Enum values.
- Create an explicit compatibility or API versioning plan for incompatible changes.
- Deploy a compatible backend before the application begins to depend on it.
- Remove old behavior only after supported application versions no longer use it.

## Success Response and Error Rules

- Use one documented success response convention.
- Do not wrap responses that must not have a body, such as HTTP 204.
- Use one documented public error response format.
- Public errors must include a stable machine-readable error code.
- Validation errors may identify invalid fields but must not expose submitted secrets.
- Authentication and authorization failures must use the same public error contract as Controller exceptions.
- Never expose stack traces, SQL, internal class names, internal paths, raw provider responses, tokens, or sensitive user data.
- Include public error codes used by the application in OpenAPI or an approved API document.

If a success wrapper or error schema has not yet been approved, do not finalize one silently. Draft alternatives and request a decision.

## Database and Flyway Rules

- Applied Flyway migrations are immutable.
- Do not modify, rename, reorder, or delete applied migrations.
- Add every schema change as a new migration.
- Match Entities to the actual applied database schema.
- Do not change the schema merely to make Entity implementation easier.
- Verify unique constraints, foreign keys, indexes, ownership, timestamps, state transitions, and deletion behavior.
- Repositories are responsible only for persistence.
- Do not expose persistence Entities as public DTOs.
- Keep `spring.jpa.hibernate.ddl-auto` compatible with the Flyway-based `validate` policy.
- Store and exchange time according to the approved UTC and ISO 8601 policy.

If migrations, Entities, the data dictionary, and product documents do not agree, stop implementation and report the differences.

## Account and Identifier Rules

- Identify a Naroom member by the internal Naroom member ID.
- Keep `Member` and `SocialIdentity` as separate concepts.
- Store external provider identifiers separately from the member.
- Use the Kakao service user ID as the stable Kakao identifier.
- Apply the approved unique constraint to provider and provider user ID.
- Email, nickname, name, birth date, gender, and age range are not primary identifiers.
- Do not automatically merge accounts because personal information matches.
- Implement only Kakao login in Beta 1.
- The data model may remain extensible for future providers.
- Do not implement Google login, account linking, login method management, or account merging without a separate request.

Older documents may include Google login or Beta 1 account linking. The currently approved implementation scope is an extensible provider data structure and Kakao login only. Do not implement the older scope; report the conflict.

## Account Deletion Rules

- Block normal account access immediately after a deletion request.
- Change member status to `PENDING_DELETION`.
- Keep only the approved minimum data during the seven-day grace period.
- Do not use pending-deletion data for normal services or AI processing.
- Permanently delete approved data after the grace period.
- Do not automatically restore the account when the user logs in during the grace period.
- Restoration requires the user's explicit confirmation.
- Do not expose existing records until restoration completes successfully.

## Authentication and Token Rules

- After validating Kakao login, Naroom issues its own Access Token and Refresh Token.
- Do not use a Kakao Access Token as a Naroom session token.
- Never store the raw Naroom Refresh Token.
- Store only the approved secure hash or verifier and minimum session metadata.
- Do not store Kakao Access or Refresh Tokens unless a separately approved feature requires later Kakao API calls.
- Before implementation, define expiration, rotation, reuse detection, revocation, logout, and concurrent-session policies.
- Do not weaken secure defaults for local development convenience.
- Never log authorization codes, Access Tokens, Refresh Tokens, ID Tokens, secrets, or complete Authorization headers.
- Authentication tests and fixtures must use fake values only.

## Product Invariants

- Save the user's original record before and independently of AI processing.
- AI processing failure must never cause loss of the original record.
- Do not treat AI output as a definitive diagnosis or judgment.
- Emotion and energy values are observations, not scores that evaluate the user.
- Use `작은 실험` in user-facing Korean copy.
- Existing internal backend code may use `challenge` if that term is already approved.
- Do not introduce rankings, comparison, streak pressure, or failure-centered language without explicit approval.

## Environment Variables and Secrets

Manage environment variable names and safe examples in `.env.example`.

Never place real secrets in:

- Source code
- Test code or fixtures
- Logs
- README or project documents
- `.env.example`
- OpenAPI output
- Git commits
- Claude or OMC output

Claude Code must not read, modify, or print:

- `.env.local`
- `.env.*.local`
- Database passwords
- JWT signing keys
- OpenAI API keys
- Kakao Client Secrets
- Firebase or Google Cloud service account JSON
- Certificates and private keys
- Keystores
- External service tokens
- Real user records
- Real authentication tokens

If a secret is found in code or a Git diff, do not repeat its value. Report only the affected file and the potential exposure.

## Protected and Caution Files

Do not modify the following without an explicit request:

- `build/`
- `.gradle/`
- `.idea/`
- `.env.local`
- `.env.*.local`
- `gradle/wrapper/gradle-wrapper.jar`
- Gradle Wrapper version or configuration
- Applied Flyway migrations
- Secret exclusion rules in `.gitignore`
- Generated OpenAPI snapshots by manual editing

If the Gradle Wrapper must change, explain why and propose the official Wrapper command before modifying it.

## OMC and Repository Boundaries

- Follow `.claude/CLAUDE.md` for OMC operating rules.
- Do not use OMC runtime state as authoritative project information.
- Store long-lived decisions in approved project documents.
- Do not modify `naroom-app` during backend-only work.
- Do not assign unresolved duplicate contract decisions to parallel Workers.
- Parallelize implementation only when contracts and file ownership are clear.
- Workers must not independently change an approved API contract.
- If a contract issue appears during implementation, stop and report it before changing the contract.

## Git Rules

- `dev` is the normal development and verification branch.
- `main` is the stable branch.
- Do not create a `feature/*` branch without an explicit request.
- Pull Requests are optional and used only for requested large or high-risk changes.
- Do not commit, push, merge, rebase, create a PR, close an Issue, or change remotes without explicit instruction.
- Keep source, migration, documentation, generated contract, and AI-tool changes logically separated when practical.
- Never commit local environment files, OMC runtime state, personal settings, secrets, user data, or authentication tokens.
- Write commit messages in Korean. Keep original English technical terms, proper nouns, and identifiers as-is (e.g. `Spring Security`, `ProblemDetail`, class/method names) instead of translating them.
- Write the commit body as short, technical bullet points (what changed), not narrative prose explaining rationale at length.
- Do not include any mention of Claude, AI, or automated-generation tools in the commit message or body.
- Do not add a `Generated with Claude Code` or similar signature.
- Do not add a `Co-Authored-By: Claude` trailer.
- Write the commit message concisely from the developer's perspective, describing the actual purpose of the change.
- Keep each commit scoped to a single logical change.
- Before running the commit, show the user the changed files and the commit message and get approval.

## Working Procedure

1. Read `docs/PRODUCT_CONTEXT.md`.
2. Read the current Issue or user requirements.
3. Inspect related code, tests, migrations, and contracts.
4. Define included and excluded scope.
5. State expected file changes and the verification plan.
6. Implement only the approved scope.
7. Run focused tests, the full test suite, and the build.
8. Regenerate OpenAPI when the public contract changes.
9. Review the final diff and check for secrets.
10. Report changes, commands, results, risks, and unresolved decisions.
