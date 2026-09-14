# BUX — Investment Plans Architecture

Take-home deliverable for BUX's "Staff Software Engineer (Backend)" assignment:
a production-ready architecture design for an **Investment Plans** feature on
the BUX mobile app. The brief is at `docs/staff-software-engineer-backend-2.pdf`.
The output is a design document, not a working implementation.

## Deliverable

1. [Architecture Overview](docs/1-architecture-overview.md) · [PDF](docs/pdf/1-architecture-overview.pdf) · [Detailed](docs/detailed/1-architecture-overview.md)
2. [Key Design Decisions](docs/2-key-design-decisions.md) · [PDF](docs/pdf/2-key-design-decisions.pdf) · [Detailed](docs/detailed/2-key-design-decisions.md)
3. [Trade-offs](docs/3-trade-offs.md) · [PDF](docs/pdf/3-trade-offs.pdf) · [Detailed](docs/detailed/3-trade-offs.md)
4. [Risks & Mitigations](docs/4-risks-and-mitigations.md) · [PDF](docs/pdf/4-risks-and-mitigations.pdf) · [Detailed](docs/detailed/4-risks-and-mitigations.md)
5. [Assumptions & Open Questions](docs/5-assumptions-and-open-questions.md) · [PDF](docs/pdf/5-assumptions-and-open-questions.pdf) · [Detailed](docs/detailed/5-assumptions-and-open-questions.md)
6. [Non-Functional Requirements](docs/6-non-functional-requirements.md) · [PDF](docs/pdf/6-non-functional-requirements.pdf) · [Detailed](docs/detailed/6-non-functional-requirements.md)
7. [MVP / Post-MVP](docs/7-mvp-post-mvp.md) · [PDF](docs/pdf/7-mvp-post-mvp.pdf) · [Detailed](docs/detailed/7-mvp-post-mvp.md)
8. [Delivery Plan](docs/8-delivery-plan.md) · [PDF](docs/pdf/8-delivery-plan.pdf) · [Detailed](docs/detailed/8-delivery-plan.md)
9. [AI Tool Usage](docs/9-ai-tool-usage.md) · [PDF](docs/pdf/9-ai-tool-usage.pdf) · [Detailed](docs/detailed/9-ai-tool-usage.md)

## Illustrative implementation

`pom.xml` + `src/` hold pseudocode (Java 25 / Spring Boot 4.1.1 / Kafka /
Postgres 18) for four key mechanisms from the design: the transactional
outbox write path (`api`), the execution trigger/fan-out flow with its
idempotency guarantee (`execution`), order-status event handling and
reactive plan deactivation (`orderstatus`), and the CQRS read-side
projection (`projection`). Not intended to compile or run — see the
comment at the top of `pom.xml`.

### Requirements

To (attempt to) build this module you'd need:

- **Java 25**
- **Maven 3.9.15**
- **Docker** — the integration tests start real Postgres/Kafka containers via Testcontainers, so a running Docker daemon is required to run them
- **Lombok** IDE plugin, if opening the code in an IDE (so the IDE understands the generated getters/constructors/builders instead of showing false errors)

As noted above, the module isn't actually expected to compile (Spring Boot
4.1.1 doesn't exist yet at time of writing) — these are the versions the
pseudocode targets, not a tested toolchain.

### Generated code (OpenAPI)

The HTTP API is contract-first: [`src/main/resources/openapi/investment-plans-api.yaml`](src/main/resources/openapi/investment-plans-api.yaml)
is the source of truth, and the `openapi-generator-maven-plugin` execution
in `pom.xml` generates the `PlansApi`/`ExecutionsApi` interfaces and every
model class (`CreatePlanRequest`, `PlanSummary`, `PlanDetail`,
`ExecutionSummary`, `ExecutionDetail`, `Investment`, `OrderSummary`, …)
under `com.bux.investmentplans.api.generated`. None of that generated code
is checked into git — `PlanController`/`ExecutionController` in `src/main`
implement the generated interfaces by hand, and MapStruct mappers
(`PlanApiMapper`, `ExecutionApiMapper`) translate between the generated
models and this module's own domain/read-model types.

To (re)generate the sources:

```
mvn generate-sources
```

This writes the generated interfaces/models to
`target/generated-sources/openapi`, which the build also adds to the
compile source root — running a normal `mvn compile`/`mvn test` triggers
the same generation step first.

### Tests

The integration tests under `src/test/java` use
[Testcontainers](https://testcontainers.com/) to spin up real Postgres 18
and Kafka containers per test class (see
`support/AbstractIntegrationTest`), rather than mocking the database/broker.
Each test exercises one of the four flows above end-to-end and asserts the
specific guarantee that flow exists to provide — e.g. `ExecutionTriggerFlowIT`
sends a duplicate `InitiateExecution` and asserts it's a no-op (the
idempotency guarantee from `2-key-design-decisions.md` 2.5). Requires
Docker to be running.

## Folder structure

```
docs/
├── 1-*.md … 9-*.md        trimmed deliverable (current version)
├── detailed/              full, untrimmed originals of each section
├── pdf/                   rendered PDF of each section
└── staff-software-engineer-backend-2.pdf   the assignment brief
pom.xml, src/               illustrative pseudocode for key design mechanisms
├── main/resources/openapi/investment-plans-api.yaml   OpenAPI spec (source of truth for the HTTP API)
└── test/java/…            Testcontainers-backed integration tests, one per flow
```
