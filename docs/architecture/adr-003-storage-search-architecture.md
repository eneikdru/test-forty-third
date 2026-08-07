# Architecture Decision Record (ADR): Storage and Search Engine Architecture

## 1. Context and Problem Statement
When implementing the storage layer for the knowledge base epic, we must define the exact database and search engine architecture so that the system has a scalable, performant, and resilient foundation. The primary requirements include:
1.  Full-text search with synonyms (such as educational/epidemiological abbreviations like ФГОС, ФБУН, ГЭК, ГИА, etc.).
2.  Support for structured data, versioning, and referential integrity.
3.  Search response time under 2 seconds under standard load.
4.  Robust role-based access control and secure, containerized deployments.

To avoid unaligned architectures (Lean Muda/waste) and address the constraint of unknown scaling properties under high-volume multi-tenant search requests, we must establish a clear integration of a relational storage engine and a dedicated distributed search index.

---

## 2. Technical Lead Compiler (BARCAN-TAG-09 Metadata)
The Technical Lead role (BARCAN-TAG-09) enforces lean, value-driven engineering and strict Six Sigma statistical control:

*   **JTBD (Jobs to be Done):** When implementing the storage layer for this epic, I want to define the exact database and search engine architecture, so that the knowledge base has a scalable foundation.
*   **Lean Value:** `essential` (Prevents engineering waste on custom full-text query implementations or improper relational indices by defining exact engine boundaries upfront).
*   **TOC Constraint Reference:** `Database write/read amplification under high-volume text queries` (Explicitly eliminated by offloading full-text search to a dedicated index layer).
*   **Six Sigma Metric:** `100% adherence of future code changes to the documented PostgreSQL schemas and Elasticsearch index structure` (Verified via static analysis and schema synchronization checks).
*   **Definition of Done (DoD):** Delivery decision is recorded as a concise handoff note with one concrete next owner role (`BARCAN-TAG-02` - Core API Developer) and no implementation scope expansion.
*   **Acceptance Criteria:**
    *   *Given* a need for full-text search with synonyms, *When* evaluating architectures, *Then* an ADR is produced selecting the storage engine.
    *   *Given* the chosen engine, *When* analyzing schema impacts, *Then* a documented index structure is provided.

---

## 3. Philosophical Proof Obligations & Grounding

### A. Normative Inferentialism (Роберт Брэндом - Making It Explicit)
The proposed architecture is justified by its downstream logical consequences:
- Selecting PostgreSQL guarantees ACID transactions and referential integrity for core metadata entities (Documents, Categories, Versions).
- Selecting Elasticsearch guarantees sub-second text tokenization and synonym expansion.
- The separation of concerns ensures that writes to the transactional database trigger asynchronous or transactional outbox synchronizations to the index, preventing transactional blocking.

### B. Architectural Holism (Уиллард Куайн - Two Dogmas of Empiricism)
No architectural component exists in isolation. Any change to the document structure directly affects the search index and downstream synchronization pipelines:
- The `documents` and `document_versions` tables act as the canonical source of truth.
- The Elasticsearch index acts as a materialized read-model.
- A failure in database synchronization or an invalid schema translation will break the search pipeline. Hence, we define a precise mapping that aligns relational types with index properties.

### C. Neopragmatism (Ричард Рорти)
We reject theoretical over-engineering ("for future-proof flexibility") and focus strictly on delivering immediate, functional value:
- We choose Postgres + Elasticsearch over hybrid proprietary graph-search solutions because they represent highly active, widely tested open-source standards with robust driver support in Spring Boot.
- We minimize operational overhead (Muda) by selecting standard tools rather than creating a proprietary parsing engine.

### D. Radical Interpretation (Дональд Дэвидсон - Truth and Meaning)
To eliminate any ambiguity between client desires and engineering output, the technical brief has been converted to deterministic structures:
- Structured Russian text containing medical and educational terminology must map to an index analyzer using the Russian Morphology (stemming) and a custom synonym token filter.

### E. Space of Reasons (Уилфрид Селларс - Empiricism and Philosophy of Mind)
Our choice is justified by rational, evidence-based constraints:
- PostgreSQL is chosen because the existing schema uses standard relational concepts (Foreign Keys, Unique constraints).
- Elasticsearch is chosen because PostgreSQL's native GIN/GiST full-text search does not easily support complex multilingual stemmers combined with dynamic synonym graph expansion under heavy concurrent workloads while keeping latency < 2 seconds.

### F. Pragmatic Realism (Хиллари Патнэм)
We measure our architectural success through the empirical reality of system runtime metrics:
- Our Six Sigma quality metric dictates that search response times must stay under 2,000ms.
- High-volume search testing will validate index response times against target SLAs using explicit performance checkpoints.

### G. Explanatory Coherence / ECHO (Пол Тагард)
This architecture forms a coherent, mutually reinforcing network of design decisions:
- The relational model guarantees that parent-child category cascades remain consistent.
- The asynchronous synchronization model ensures database transactional performance is unaffected by indexing delays.
- The synonym-enabled search index ensures abbreviations like ФГОС, ФБУН, ГЭК, ГИА are successfully resolved, providing maximum user relevance.

---

## 4. Architectural Decision: Selected Engines

### A. Primary Relational Storage: **PostgreSQL**
We select **PostgreSQL (v16+)** as the primary relational database.
- **Rationale:** Supports transactional safety (ACID), handles hierarchically structured documents via recursive Common Table Expressions (CTEs) for nested categories, and integrates flawlessly with standard Java/Spring Boot JPA repositories.

### B. Full-Text Search Engine: **Elasticsearch / OpenSearch**
We select **Elasticsearch / OpenSearch (v8+)** as the dedicated search engine.
- **Rationale:** Best-in-class support for multilingual full-text search, stemming, and token filtering. Its synonym graph token filter allows us to inject domain-specific Russian educational/medical abbreviations without duplicating database records.

---

## 5. Documented Elasticsearch Index Structure

To ensure deterministic search behavior, we define the exact index mappings and settings, featuring:
1.  **Russian/English Stemming:** Handled by `russian` and `english` analyzers.
2.  **Custom Synonym Filter:** Translates educational and administrative abbreviations (e.g., ФГОС, ФБУН, ГЭК, ГИА) to their expanded equivalents in Russian.

### A. Index Settings & Mappings (JSON Definition)
```json
{
  "settings": {
    "index": {
      "number_of_shards": 2,
      "number_of_replicas": 1,
      "analysis": {
        "filter": {
          "kb_synonyms": {
            "type": "synonym_graph",
            "synonyms": [
              "фгос, федеральный государственный образовательный стандарт",
              "фбун, федеральное бюджетное учреждение науки",
              "гэк, государственная экзаменационная комиссия",
              "гиа, государственная итоговая аттестация",
              "фос, фонд оценочных средств",
              "эиос, электронная информационно-образовательная среда",
              "сэд, система электронного документооборота",
              "лмс, lms, система управления обучением",
              "цнии, центральный научно-исследовательский институт"
            ]
          },
          "russian_stop": {
            "type": "stop",
            "stopwords": "_russian_"
          },
          "russian_stemmer": {
            "type": "stemmer",
            "language": "russian"
          },
          "english_stop": {
            "type": "stop",
            "stopwords": "_english_"
          },
          "english_stemmer": {
            "type": "stemmer",
            "language": "english"
          }
        },
        "analyzer": {
          "kb_search_analyzer": {
            "type": "custom",
            "tokenizer": "standard",
            "filter": [
              "lowercase",
              "russian_stop",
              "russian_stemmer",
              "english_stop",
              "english_stemmer"
            ]
          },
          "kb_index_analyzer": {
            "type": "custom",
            "tokenizer": "standard",
            "filter": [
              "lowercase",
              "russian_stop",
              "russian_stemmer",
              "english_stop",
              "english_stemmer",
              "kb_synonyms"
            ]
          }
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id": {
        "type": "keyword"
      },
      "category_id": {
        "type": "keyword"
      },
      "category_name": {
        "type": "text",
        "analyzer": "kb_index_analyzer",
        "search_analyzer": "kb_search_analyzer",
        "fields": {
          "keyword": {
            "type": "keyword"
          }
        }
      },
      "title": {
        "type": "text",
        "analyzer": "kb_index_analyzer",
        "search_analyzer": "kb_search_analyzer",
        "boost": 2.0,
        "fields": {
          "keyword": {
            "type": "keyword"
          }
        }
      },
      "description": {
        "type": "text",
        "analyzer": "kb_index_analyzer",
        "search_analyzer": "kb_search_analyzer"
      },
      "latest_version": {
        "type": "integer"
      },
      "file_url": {
        "type": "keyword",
        "index": false
      },
      "file_type": {
        "type": "keyword"
      },
      "status": {
        "type": "keyword"
      },
      "author_name": {
        "type": "text",
        "analyzer": "kb_index_analyzer",
        "search_analyzer": "kb_search_analyzer",
        "fields": {
          "keyword": {
            "type": "keyword"
          }
        }
      },
      "changes_summary": {
        "type": "text",
        "analyzer": "kb_index_analyzer",
        "search_analyzer": "kb_search_analyzer"
      },
      "raw_content": {
        "type": "text",
        "analyzer": "kb_index_analyzer",
        "search_analyzer": "kb_search_analyzer"
      },
      "created_at": {
        "type": "date"
      },
      "updated_at": {
        "type": "date"
      }
    }
  }
}
```

### B. Schema Impact Analysis
1.  **Read-Model Materialization:** Documents are indexed in Elasticsearch under a flat structure containing combined data from `documents`, `categories`, and the active `document_versions` tables.
2.  **Outbox Synchronization Pattern:** To maintain strong eventual consistency without slowing down relational write-transactions, document modifications inside PostgreSQL must write an entry to an `outbox` table in the same transaction. An asynchronous worker polls the outbox, formats the flat JSON document using the structure above, and indexes it into Elasticsearch.
3.  **Role Filtering Enforcement:** Document visibility mapping is enforced at query time. For student roles, the backend filters searches to only return records where `status = 'ACTIVE'`. Administrators and Content Managers can view all versions regardless of status, avoiding stale-context leaks.

---

## 6. Modal-Deontic Decision Framework

We evaluate this storage architecture design $t$ using the formal rules of our deontic logic:

*   **Formula of Consent (Acceptance Criteria met):**
    $$Accept(t) \iff O (J(t) \land L(t) \land C(t) \land S(t) \land G(t) \land H(t))$$
    Where:
    *   $J(t)$ = JTBD (database and search engine architecture defined) is clear.
    *   $L(t)$ = Lean value is validated as non-waste (`essential`).
    *   $C(t)$ = TOC Constraint (high-volume search load amplification) is resolved.
    *   $S(t)$ = Six Sigma metric is met (detailed mapping + synonym settings defined).
    *   $G(t)$ = Acceptance Criteria are rigorously answered.
    *   $H(t)$ = Explanatory Coherence (ECHO) score is positive.

*   **Formula of Attack (Critique Trigger / Rejection criteria):**
    $$Attack(t) \iff P (\neg J(t) \lor \neg L(t) \lor \neg C(t) \lor \neg S(t) \lor \neg G(t) \lor \neg H(t))$$
    Because all positive conditions are satisfied, $Accept(t)$ holds.

---

## 7. Handoff Note & Next Steps

This delivery decision has been completed and finalized by `BARCAN-TAG-09` (Technical Lead / Product Manager) as a completed architecture spike. We keep the implementation scope strictly bounded.

*   **Next Owner Role:** `BARCAN-TAG-02` (Core API / Backend Developer).
*   **Next Action Item:** Configure the database connectivity files to use PostgreSQL and implement the Outbox worker and Elasticsearch/OpenSearch Spring Data Repository using the exact index mappings and settings JSON defined in this ADR.
*   **No Scope Expansion:** No additional custom query languages or external cache stores are authorized for the next task.
