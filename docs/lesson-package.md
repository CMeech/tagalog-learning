# Lesson Package Authoring Contract (v1)

This document is the complete authoring contract for a single lesson package. A package is a
directory containing `lesson.json` and at least one of `vocabulary.csv`, `sentences.csv`, or
`grammar.csv`. Empty entity files may be omitted. Other files are ignored.

## Files and limits

- File names are exact and case-sensitive.
- Every contract file is UTF-8 without a byte-order mark. Text must be Unicode NFC normalized.
- `lesson.json` must conform to [`lesson-package.schema.json`](lesson-package.schema.json). JSON
  property names are exact and case-sensitive; unspecified properties are invalid.
- Each contract file may be at most 10 MiB (10,485,760 bytes), and the four contract files together
  may be at most 25 MiB (26,214,400 bytes). Ignored files do not count toward this limit.
- Each CSV file may contain at most 100,000 data rows. A decoded metadata string or CSV field may
  contain at most 1,048,576 Unicode characters. These limits are checked in addition to the byte
  limits so malformed input cannot cause unbounded parser allocations or collections.
- A package contains one lesson only. There is no ordering dependency among its rows.

## Manifest

`lesson.json` has these properties:

| Property | Required | Meaning |
| --- | --- | --- |
| `schema_version` | yes | Integer contract version; must be `1`. |
| `lesson.id` | yes | Stable lesson UUID. |
| `lesson.name` | yes | Non-blank lesson name. |
| `lesson.description` | no | Non-blank description when supplied; no default. |
| `sources` | yes | Source records; may be an empty array. Source IDs must be unique. |
| `sources[].id` | yes | Stable source UUID. |
| `sources[].name` | yes | Non-blank source name. |
| `sources[].type` | yes | One of `TEACHER`, `COURSE`, `BOOK`, `SONG`, `VIDEO`, `WEBSITE`, `OTHER`. |
| `sources[].reference` | no | Non-blank path, title, page, or URL when supplied; no default. |
| `default_source_id` | no | UUID of a source in `sources`; no default. |

The schema validates structure and scalar spelling. Cross-field requirements—unique source IDs and
membership of `default_source_id` in `sources`—are also part of the contract and are checked by the
application.

## CSV files

CSV uses RFC 4180-style records: comma delimiter, double-quote quoting, doubled double quotes inside
quoted fields, and CRLF or LF record endings. A field containing a comma, double quote, or newline
must be quoted. Embedded newlines are preserved. Headers must exactly match the templates in
[`examples/import`](../examples/import), including order and case; no extra or missing columns are
allowed.

All metadata and CSV string scalars are NFC-normalized and trimmed at their outer edges after
parsing. Internal spaces and quoted newlines are preserved. A blank optional field means absent. A
blank required field is invalid. Whitespace-only values are blank. There is no syntax for an
explicit empty string. Enum values and UUIDs must already have their required case and spelling after
trimming; the importer does not case-correct them.

UUIDs are lowercase canonical strings such as `3fa85f64-5717-4562-b3fc-2c963f66afa6`. Every row ID
must be unique across the package. `source_id`, `vocabulary_ids`, and `grammar_ids` may point to a
record in this package or an existing SQLite record, subject to the relationship type.

List fields use a literal pipe (`|`) delimiter. Each item is individually trimmed; blank items,
duplicates, and literal pipes within values are invalid. List order is not meaningful. A blank list
is empty. Relationship lists contain UUIDs. Tags contain names that must be NFC, already lowercase
under Unicode lowercase conversion, and contain neither Unicode whitespace nor `|`. This keeps one
CSV item equal to one Anki tag; `::` may be used for Anki hierarchy. Repeated tag names are invalid.

### `vocabulary.csv`

Header: `id,tagalog,english,root_word,part_of_speech,difficulty,frequency_rank,source_id,tags`

- Required: `id`, `tagalog`, `english`, `part_of_speech`.
- `part_of_speech`: `NOUN`, `PRONOUN`, `VERB`, `ADJECTIVE`, `ADVERB`, `PREPOSITION`,
  `CONJUNCTION`, `PARTICLE`, `INTERJECTION`, `PHRASE`, or `OTHER`.
- Blank `difficulty` defaults to `BEGINNER`; otherwise it is `BEGINNER`, `INTERMEDIATE`, or
  `ADVANCED`.
- `frequency_rank`, when present, is a positive base-10 integer with no sign.
- `root_word`, `source_id`, and `tags` are optional. A blank `source_id` uses the metadata default;
  it remains absent if the metadata has no default.

### `sentences.csv`

Header: `id,text,translation,difficulty,source_id,vocabulary_ids,grammar_ids`

- Required: `id`, `text`, `translation`.
- Blank `difficulty` defaults to `BEGINNER`; its other values are as above.
- `source_id`, `vocabulary_ids`, and `grammar_ids` are optional. Source defaulting is as above.

### `grammar.csv`

Header: `id,name,description,formula,source_id`

- Required: `id`, `name`, `description`, `formula`.
- `source_id` is optional and uses the same defaulting rule.
- Examples are represented only by sentence `grammar_ids` relationships.

Every imported vocabulary, sentence, and grammar row gains an association with the metadata lesson.
The knowledge record itself remains global and may be associated with many lessons. A row's resolved
`source_id` is provenance for that entity's occurrence in this lesson rather than exclusive ownership
of the entity. A non-blank `source_id` must resolve to a source in this metadata or one already stored
in SQLite. Sources listed in the metadata are global records subject to the same
new/unchanged/conflict/update rules as other global records; the metadata also associates them with
the lesson.

## Normalized content and duplicate comparison

Normalization occurs before validation or comparison: decode UTF-8, require NFC input, trim string
scalars and list items, apply blank/default rules, validate scalar types, then treat tag and
relationship lists as unordered sets. Normalization does not collapse internal whitespace or change
letter case.

An entity's content fingerprint excludes its own UUID and includes its complete package-owned
representation:

- lesson: normalized `name` and `description`;
- source: normalized `name`, `type`, and `reference`;
- vocabulary: normalized scalar columns and the sorted set of tag names;
- sentence: normalized scalar columns and the sorted vocabulary and grammar UUID sets;
- grammar: normalized scalar columns.

Lesson UUID and resolved source UUID are deliberately excluded from these global entity
fingerprints. They form a lesson/entity association with per-lesson provenance. Reusing an existing
entity UUID in a later lesson therefore adds knowledge-graph context without duplicating or changing
the entity. Changing association provenance while correcting the same lesson is still an explicit
package update, but it never removes associations belonging to other lessons.

Two records with the same UUID compare these fingerprints to decide unchanged versus conflict. Two
records of the same entity type with different UUIDs but equal fingerprints are exact-content
duplicates and validation reports both UUIDs. Fingerprints are compared both within the package and
against SQLite. Different entity types are never duplicates of one another.

Tags are the deliberate exception to package UUID authoring: `tags` contains names, not tag records.
The importer resolves or creates the database `Tag` by its normalized lowercase name and assigns its
internal UUID. Authors preserve the tag spelling, not a tag UUID. This does not introduce an external
key for any package record.

## Stable identity and corrections

Generate a UUID once for each lesson, source, vocabulary item, sentence, and grammar concept. UUIDv4
is recommended. Keep the ID beside the content in every working draft. When correcting spelling,
translation, metadata, tags, or relationships, edit the existing row and preserve its UUID. Assign a
new UUID only when introducing a genuinely new domain record; never derive identity from mutable
text or row position. Reuse the known UUID when referring to an earlier package's record.

A different UUID with otherwise identical normalized content is invalid. Omitting an old row does
not delete it. Imports are incremental, and updates must be explicitly enabled when an existing UUID
has changed content.

## Copyable generation prompt

```text
Create one Tagalog lesson package from the supplied source material. Output exactly one lesson.json
and at least one non-empty CSV named vocabulary.csv, sentences.csv, or grammar.csv. Follow
docs/lesson-package.md and validate lesson.json against docs/lesson-package.schema.json. Copy CSV
headers exactly from examples/import/*.csv. Use UTF-8, Unicode NFC, lowercase canonical UUIDs, and
schema_version 1. Preserve any UUIDs supplied with the source or prior package; generate UUIDv4 only
for genuinely new records. Do not change IDs while correcting content. Include source provenance,
use the metadata default source where appropriate, quote CSV fields correctly, and use pipe-separated
unique values only for tags and relationship IDs. Do not invent extra properties, columns, files, or
editorial workflow state. Tag names must be lowercase NFC tokens without whitespace or pipes. Return
the files plus a short validation summary listing counts and every cross-package relationship.
```

See [`examples/lesson-package`](../examples/lesson-package) for a complete package.
