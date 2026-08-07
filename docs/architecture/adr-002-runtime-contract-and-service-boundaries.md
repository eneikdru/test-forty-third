# Architecture Decision Record (ADR): Runtime Contract and Service Boundaries

## 1. Context and Problem Statement
When a new project starts, developer agents and human operators must have explicit knowledge of the repository structure, runtime commands, and backend/frontend boundaries. This ensures that Jules can execute short role-owned tasks without guessing where code belongs, preventing cognitive friction, architectural drift, or misplacement of artifacts.

---

## 2. Technical Lead Compiler (BARCAN-TAG-01 Metadata)
The Solution Architect role (BARCAN-TAG-01) designs and formalizes the domain, service boundaries, and structural contracts of the solution:

*   **JTBD (Jobs to be Done):** When a new project starts, I want the repository structure, runtime commands, and backend/frontend boundaries to be explicit, so that Jules can execute short role-owned tasks without guessing where code belongs.
*   **Lean Value:** `essential` (Eliminates guessing, redundant navigation, and structural misalignment across parallel agent sessions).
*   **TOC Constraint Reference:** `Lack of explicit local runtime boundaries and developer execution contract`.
*   **Six Sigma Metric:** `100% adherence to defined folder structures and commands across downstream PRs`.
*   **Definition of Done (DoD):** Runtime contract exists, is documented under `docs/architecture/`, and explicitly names the next owner role (`BARCAN-TAG-02` - Core API Developer).

---

## 3. Philosophical Proof Obligations & Grounding

### A. Principle of Quantified Actualism (Рут Баркан Маркус - Actualist Semantics)
To prevent abstract or non-functional stubs from entering the codebase, we restrict the system domain to active, physically realized database entities and business logic elements:
- **Project ID:** `2bbd00c8-c877-4bf5-8fff-2f53bea9dd9d`
- **Branch:** `jules-8043812447573544627-82795ac6`
- **Commit:** `2f6ee3d8323157c94bf4d608be9324fcc853e1cd`
- **Runtime SHA:** `2f6ee3d8323157c94bf4d608be9324fcc853e1cd`
- **Task State:** `COMPLETED_BOOTSTRAP_BOUNDARY`
- **Actualist Mandate:** All packages, interfaces, and persistence models must map directly to physically active components (e.g., the existing H2-backed Flyway schemas). Interface "skeletons" designed "for future flexibility" without current physical application logic are strictly forbidden.

### B. Applied Formal Ontology & Taxonomy (Бэрри Смит)
A rigid taxonomy of classes and types is enforced. Each class has one strictly defined place:
- **Core Entities** (e.g., `Document`, `Category`, `Role`) must reside inside structured, non-anemic domain packages containing their invariants and business behavior.
- **Service orchestrators** are strictly isolated from domain decision-making.

### C. Modern Mereology: Part-Whole Relationships (Питер Саймонс)
We maintain mathematically precise part-whole relationships within our data models and structures:
- `DocumentVersion` is an essential part of the `Document` whole. It is topologically dependent on `Document` and cannot exist in isolation.
- Cascade deletion and aggregate constraints are enforced at the database level (`ON DELETE CASCADE`) and reflected in domain entity relationships.

### D. Topology of Spatio-Temporal Boundaries (Ахилле Варци)
The spatial boundaries between backend and frontend code are physically and topologically isolated to avoid semantic leaks and dependency cycles:
- **Backend Boundary:** Rooted strictly at `/` containing the Java project and maven structure. All backend source code must live under `src/main/java` (e.g. `com.eneik.generated`).
- **Frontend Boundary:** Rooted strictly at `frontend/`. No frontend node packages, assets, or configs are permitted outside this boundary.

### E. Priority Monism (Джонатан Шаффер)
We protect the fundamental architectural dependencies of the whole system. The parent database schemas and the backend Maven contract (`pom.xml`) ground the downstream service layers. Downstream features are grounded by, and must conform to, the central database schema validated in the build pipeline.

### F. Metaphysics of Essence (Кит Файн)
We strictly separate core properties of objects from their transient statuses. Transitions between states (such as document versions moving from `DRAFT` to `ACTIVE` or `ARCHIVED`) must be atomically-guarded and verified, ensuring the underlying identity of the core object remains intact.

---

## 4. Repository Runtime Contract

To eliminate guessing, any agent or operator executing tasks in this repository must use the following commands and structure:

| Parameter | Value / Command | Purpose |
| :--- | :--- | :--- |
| **Project Root** | `/` (Repository root directory) | Primary execution environment |
| **Install Command** | `mvn dependency:resolve` | Fetches Maven dependencies and verifies the POM |
| **Run Command** | `mvn spring-boot:run` | Boots the backend server on port `8080` |
| **Test Command** | `mvn test` | Executes the complete JUnit / Spring Boot test suite |
| **Backend Boundary** | `src/main/java/` | Contains all backend Java sources and domain logic |
| **Frontend Boundary**| `frontend/` | Scoped directory for UI, Svelte/Node modules, and frontend assets |
| **Database Migrations** | `src/main/resources/db/migration/` | Location for all Flyway database migrations |
| **Target Directories** | `target/`, `frontend/node_modules/` | Build artifacts to be kept strictly out of git commits |

---

## 5. Handoff Note & Next Steps

This delivery decision has been formalized by `BARCAN-TAG-01` (Solution Architect) as a completed runtime contract and service-boundary decision.

*   **Next Owner Role:** `BARCAN-TAG-02` (Core API / Backend Developer).
*   **Next Action Item:** Begin implementing core controllers, domain service entities, and repository layers under `src/main/java/com/eneik/generated/` obeying the exact package boundaries and database schemas defined in `V20260807005515150__create_document_schema.sql` and the telemetry contracts defined in previous ADRs.
*   **No Scope Expansion:** Downstream tasks are strictly limited to the core features; do not introduce external libraries or multi-module gradle/maven subprojects unless explicitly requested by a Technical Lead task brief.
