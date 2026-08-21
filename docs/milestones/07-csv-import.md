# Milestone 7 — Weekly Lesson Pipeline

Goal: validate a generated lesson package, import it atomically into SQLite, and produce repeatable
Anki-ready files through commands suitable for both interactive and automated use.

Milestone 6 defines the package and Anki contracts. This milestone implements them without adding
content review statuses.

## Weekly workflow

```text
1. Generate or correct a lesson package.
2. tagalog lesson validate <package> [--format text|json]
3. tagalog lesson import <package> [--update-existing] [--format text|json]
4. tagalog lesson show <lesson-id>
5. tagalog anki export --lesson <lesson-id> --output <new-directory>
6. Manually import vocabulary.tsv, sentences.tsv, and grammar.tsv into Anki.
```

After the individual commands are stable, a convenience command may run the same pipeline:

```text
tagalog lesson publish <package> --output <new-directory> [--update-existing]
```

## CLI behavior

- Text output is concise and intended for a person.
- `--format json` has a documented stable shape for agent-driven execution.
- Validation and conflict failures return non-zero exit codes and never partially modify SQLite.
- Import success reports the lesson ID plus inserted, updated, unchanged, and related counts.
- Export requires a new/nonexistent output directory, avoiding implicit overwrite or deletion.
- A repeated export may target another new directory and does not depend on export status.
- Omitting a previously imported row from a package never deletes it from SQLite.

## Implementation tasks

### M7.1 — Parse packages into typed candidates

- [x] Add and pin one CSV library and one JSON/schema-validation library.
- [x] Create manifest and CSV candidate models separate from domain and persistence models.
- [x] Load only the recognized package files and require the Milestone 6 minimum layout.
- [x] Validate `schema_version` before interpreting the remainder of the package.
- [x] Normalize strings to NFC and apply the documented trimming/default rules exactly once.
- [x] Parse UUIDs, enums, integers, quoted fields, embedded newlines, CRLF, BOM input, and
      pipe-separated lists.
- [x] Enforce documented file, row, and field size limits before allocating unbounded data.
- [x] Add parser tests using the canonical fixtures and malformed edge cases.

### M7.2 — Implement read-only validation

- [x] Add `tagalog lesson validate <package>`.
- [x] Validate complete headers before rows and report missing, extra, duplicate, and incorrectly
      cased columns.
- [x] Convert candidates into domain entities to reuse domain validation.
- [x] Collect all independent errors rather than stopping at the first bad row.
- [x] Report filename, one-based data-row number, column, safe supplied value, and correction guidance.
- [x] Enforce UUID uniqueness across the package and detect exact content duplicates under different
      UUIDs in both the package and SQLite.
- [x] Resolve source and relationship UUIDs against the complete package plus existing SQLite records.
- [x] Distinguish inserts, unchanged records, conflicting updates, and warnings in text and JSON
      results.
- [x] Guarantee validation opens no write transaction and add tests proving SQLite is unchanged.

### M7.3 — Persist the reusable knowledge graph and import history

The database is a durable Tagalog knowledge collection, not a staging database for Anki. Vocabulary,
sentences, and grammar concepts have global identities and may occur in many lessons. Lessons record
where knowledge was encountered; they do not own or duplicate that knowledge. Sentence-to-vocabulary
and sentence-to-grammar links remain durable semantic relationships that can be queried independently
of any export format.

- [x] Add a Flyway migration for `lesson_vocabulary`, `lesson_sentence`, `lesson_grammar`, and
      `lesson_source` associations; never alter V1. Backfill associations from V1 `lesson_id` and
      `source_id` values so existing collections retain lesson membership and provenance.
- [x] Store source provenance on each lesson/entity association where applicable. Reusing the same
      entity in a later lesson adds an association instead of changing the entity or causing a
      content conflict.
- [x] Treat V1 entity `lesson_id` and `source_id` columns as legacy compatibility data after backfill;
      new import and query code must use the association tables rather than assigning single owners.
- [x] Add read/query repository boundaries for lessons, vocabulary, sentences, grammar, and their
      associations so later inspection, export, and generation workflows do not depend on import
      internals or Anki concepts.
- [x] Add a Flyway migration for successful import-run metadata and constraints required by the
      package contract.
- [x] Store import-run UUID, lesson UUID, package checksum, schema version, timestamp, and separate
      counts for inserted, updated, unchanged, and newly related records.
- [x] Add `tagalog lesson import <package>`.
- [x] Treat an exact previously successful package checksum as an idempotent no-op that reports the
      original import run.
- [x] Insert new lesson, source, vocabulary, sentence, grammar, tag, semantic relationships, lesson
      associations, provenance, and import history in one transaction.
- [x] Compare existing UUIDs using global entity content only. Lesson membership and per-lesson source
      provenance are associations and never make otherwise identical entity content conflict.
- [x] Reject differing global content for an existing UUID unless `--update-existing` is present.
- [x] With `--update-existing`, replace complete package-owned global fields, tags, and sentence
      relationships atomically after all candidates validate. Replace the included entity's
      association metadata for this lesson without deleting associations from other lessons.
- [x] Treat packages as incremental: records and associations omitted from a later package remain
      unchanged. Package omission never means deletion or detachment.
- [x] Preserve existing records referenced by the package but not defined by it, while resolving the
      new semantic relationships to those records.
- [x] Persist successful import history in the same transaction as content and relationship changes.
- [x] Add integration tests for insert, exact rerun, conflict, explicit update, rollback, source
      defaults, tags, relationship replacement, and import history.
- [x] Add integration tests proving the same vocabulary, sentence, and grammar UUID can participate in
      multiple lessons without duplication or conflict, and that cross-package sentence relationships
      resolve to previously stored knowledge.
- [x] Add query tests proving the stored graph can answer both directions of core questions: sentences
      using a vocabulary item or grammar concept, concepts and vocabulary used by a sentence, and all
      lessons in which an entity occurs.

### M7.4 — Add collection inspection

- [x] Add `tagalog lesson list` with deterministic ordering and text/JSON output.
- [x] Add `tagalog lesson show <lesson-id>` with manifest metadata, sources, entity counts, and import
      history.
- [x] Include record UUIDs, lesson-scoped source provenance, and readable semantic relationship
      summaries so imported data can be diagnosed.
- [x] Add entity-level `show <id>` commands where necessary to inspect complete stored content,
      including every lesson association and that association's source provenance.
- [x] Add explicit `vocabulary delete <id>`, `sentence delete <id>`, and `grammar delete <id>` commands;
      never infer deletion from package omission.
- [x] Treat lesson membership and its source provenance as dependent association metadata: deleting
      an entity removes those associations in the same transaction and they do not by themselves
      prevent explicit deletion.
- [x] Refuse deletion when another global knowledge record references the target through a semantic
      relationship and report those records so they can be corrected first. In particular, refuse to
      delete vocabulary or grammar referenced by a sentence; deleting a sentence removes its outgoing
      vocabulary and grammar relationships atomically.
- [x] On deletion, print the UUID and remind the user that a matching Anki note must be deleted
      manually because TSV import cannot remove notes.
- [x] Add integration tests for unknown IDs, empty collections, ordering, and JSON stability.
- [x] Add deletion tests for unreferenced records, referenced-record refusal, transaction rollback,
      and the manual-Anki-removal notice.

### M7.5 — Implement repeatable Anki export

- [x] Add application export projections for vocabulary, sentences, and grammar without placing Anki
      concepts in domain entities.
- [x] Implement the three Milestone 6 TSV renderers and verify them against expected fixtures.
- [x] Add `tagalog anki export --lesson <lesson-id> --output <new-directory>`.
- [x] Keep delivery file-based: do not add AnkiConnect, require Anki to be running, or modify an Anki
      collection directly.
- [x] Resolve relationships into readable export fields while retaining UUID as the first field.
- [x] Select vocabulary, sentence, and grammar rows through their association with the requested
      lesson. Render each row's `Lesson` and `Source` fields from that same lesson association rather
      than from legacy entity ownership columns or an association belonging to another lesson.
- [x] For an exported grammar concept, derive `Examples` from every globally stored sentence related
      to that concept, including sentences associated with other lessons. Lesson membership limits
      which grammar rows are exported, not the global inverse relationship used to render examples.
- [x] Render every output into a temporary sibling directory; rename it to the requested destination
      only after every file and manifest succeeds.
- [x] Fail without changing the destination when it already exists, the lesson is unknown, or
      rendering fails.
- [x] Write `vocabulary.tsv`, `sentences.tsv`, `grammar.tsv`, and `export.json`; omit an entity TSV only
      when that entity type is absent from the lesson.
- [x] Include schema version, lesson ID, export timestamp, record UUIDs, file checksums, and row counts
      in `export.json` so output can be inspected and reproduced.
- [x] Do not mutate content or mark entities exported; the same lesson can be exported repeatedly.
- [x] Add escaping and integration tests for tabs, quotes, CRLF, embedded newlines, HTML-sensitive
      text, Tagalog characters, empty entity types, existing destinations, and repeat exports.

### M7.6 — Automate and document the weekly operation

- [x] Add stable JSON result schemas and exit codes for validation, import, inspection, and export.
- [x] Add `lesson publish` only as composition of the tested validate, import, and export services.
- [x] If publish imports successfully but export fails, retain the valid import and report an exact
      retry command rather than attempting rollback across SQLite and the filesystem.
- [x] Document first-time Anki note-type setup and the exact recurring import settings for all three
      files.
- [x] Document the manual deletion procedure for the rare case where an explicitly deleted database
      record already exists in Anki.
- [x] Document how to correct a package, preserve UUIDs, validate, and re-import with
      `--update-existing`.
- [x] Run the canonical sample from package validation through import and repeated Anki export using
      only documented Docker commands.
- [x] Add one end-to-end test covering generation fixtures, validation, import, inspection, correction,
      explicit update, cross-week relationship resolution, export, and repeat export.
- [x] In the end-to-end workflow, reuse the same vocabulary, sentence, and grammar UUIDs in two
      lessons with different source provenance. Verify inspection reports both associations and each
      lesson export selects the correct rows and renders that lesson's `Lesson` and `Source` values;
      also verify an exported grammar concept includes globally related example sentences from both
      lessons.

## Definition of done

- [x] A malformed package reports complete actionable errors and writes nothing.
- [x] A valid package imports all content and relationships atomically.
- [x] Exact reruns are harmless and corrections require an explicit update flag.
- [x] Package omission never deletes data; deletion is an explicit, reference-safe command.
- [x] Later packages can relate sentences to content imported in earlier weeks.
- [x] Any imported lesson can be exported again without database changes or status manipulation.
- [x] The Anki files match documented note types and update existing notes by UUID.
- [x] The sample weekly workflow passes under Docker and is usable through text or JSON output.

## Version 1 decisions

- Package UUIDs are database/domain UUIDs and Anki first-field identifiers.
- Vocabulary, sentence, and grammar UUIDs identify global knowledge records. Lesson membership and
  source provenance are many-to-many associations and are not part of global entity identity.
- SQLite is the source of truth after import; packages are retained inputs for repeatable corrections.
- Exact package reruns are no-ops, while content changes require `--update-existing`.
- Successful imports are recorded for provenance; failed validations and dry runs are not persisted.
- Export history lives in each output's `export.json`; entities have no export status.
- Anki delivery is a manual TSV import; direct Anki integration is deliberately deferred.
- Packages remain small, independent weekly inputs. They do not grow into snapshots of the complete
  collection and do not synchronize absent rows.
- Targeted single-entity CSV imports are deferred because the weekly lesson package is the primary
  product workflow.
