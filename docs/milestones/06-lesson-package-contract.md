# Milestone 6 — Lesson Package and Anki Contracts

Goal: define stable authoring and Anki delivery contracts that keep the domain independent of Anki.

Milestone 6 originally used one metadata file and three CSV input files. That authoring representation
was superseded before persistence by
[Milestone 7.2.5 — Single-File JSON Lesson Input](07.2.5-single-file-json-input.md). The current
authoring contract is [`../lesson-package.md`](../lesson-package.md) with schema
[`../lesson-package.schema.json`](../lesson-package.schema.json). The Anki TSV contract remains
[`../anki-contract.md`](../anki-contract.md).

## Current product contract

```text
source material -> generated lesson.json -> validate/import -> SQLite -> TSV export -> Anki
```

- One schema-versioned `lesson.json` contains the lesson, sources, vocabulary, sentences, grammar,
  tags, provenance, and relationships.
- SQLite is the source of truth after import; the JSON document is a retained correction artifact.
- Stable package UUIDs are domain/database UUIDs and Anki first-field identifiers.
- Knowledge identities are global. Lesson membership and source provenance are associations.
- Packages are incremental: omission never means deletion.
- Corrections preserve UUIDs and require explicit update authorization when global content changes.
- Anki delivery remains one UTF-8 TSV per entity type with readable relationship fields.
- Direct Anki automation, editorial statuses, audio, and `.apkg` generation remain out of scope.

## Completed decisions

- [x] Define a complete schema-validated authoring contract.
- [x] Define stable identity, duplicate, correction, cross-package relationship, and omission rules.
- [x] Keep lesson membership and provenance outside global entity identity.
- [x] Define vocabulary, sentence, and grammar Anki note fields and UUID update matching.
- [x] Provide Anki templates, shared CSS, import instructions, and deterministic expected TSV fixtures.
- [x] Verify that the authoring contract contains every value needed for SQLite and Anki exports.
- [x] Exclude content review state and export status from the domain.

## Completion history

The original Milestone 6 contract was complete and proved the product model, but its CSV input
representation created unnecessary parsing and multi-file coordination. M7.2.5 changes only the
authoring representation. It preserves the identity rules, normalized-content comparisons, database
relationships, Anki note types, expected TSV content, and intentional omissions established here.
