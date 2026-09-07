# Lesson Package Authoring Contract (v2)

A lesson package is one UTF-8 JSON file named `lesson.json`. It is the repeatable generated input for
one lesson; SQLite becomes the source of truth after import. The application does not accept package
directories, CSV input, YAML, Markdown, JSON Lines, or multiple lessons in one file.

## File contract

- `lesson.json` must conform to [`lesson-package.schema.json`](lesson-package.schema.json).
- The file is UTF-8 without a byte-order mark and at most 25 MiB (26,214,400 bytes).
- JSON property names, enum values, and UUID spelling are exact and case-sensitive.
- Duplicate JSON properties, unknown properties, and JSON `null` values are invalid.
- Every decoded string is at most 1,048,576 Unicode characters.
- Each top-level entity array and nested collection array contains at most 100,000 items.
- Strings are Unicode NFC-normalized and trimmed at their outer edges after parsing. Internal
  whitespace and escaped newlines are preserved.
- Optional scalar values are omitted. Blank strings do not represent absence.

## Document shape

The top-level properties `schema_version`, `lesson`, `sources`, `vocabulary`, `sentences`, and
`grammar` are required. `schema_version` is `2`. Entity arrays may be empty, but at least one of
`vocabulary`, `sentences`, or `grammar` must contain a record. `default_source_id` is optional.

See [`examples/lesson-package/lesson.json`](../examples/lesson-package/lesson.json) for the canonical
complete example.

### Lesson and sources

- `lesson` requires `id` and `name`; `description` is optional.
- Each source requires `id`, `name`, and `type`; `reference` is optional.
- Source types are `TEACHER`, `COURSE`, `BOOK`, `SONG`, `VIDEO`, `WEBSITE`, or `OTHER`.
- A supplied `default_source_id` must identify a source in `sources`.
- A missing entity `source_id` uses `default_source_id` when one is present.

### Vocabulary

Each `vocabulary` item requires `id`, `tagalog`, `english`, `part_of_speech`, and `tags`.
`root_word`, `difficulty`, `frequency_rank`, and `source_id` are optional.

- Part of speech is one of `NOUN`, `PRONOUN`, `VERB`, `ADJECTIVE`, `ADVERB`, `PREPOSITION`,
  `CONJUNCTION`, `PARTICLE`, `INTERJECTION`, `PHRASE`, or `OTHER`.
- Missing `difficulty` defaults to `BEGINNER`; other values are `INTERMEDIATE` and `ADVANCED`.
- `frequency_rank` is a positive integer.
- `tags` is a required duplicate-free JSON array. Tags are lowercase tokens without whitespace;
  `::` may express Anki hierarchy.

### Sentences

Each `sentences` item requires `id`, `text`, `translation`, `vocabulary_ids`, and `grammar_ids`.
`difficulty` and `source_id` are optional. Missing difficulty defaults to `BEGINNER`.

`vocabulary_ids` and `grammar_ids` are required duplicate-free JSON arrays. References may resolve to
records defined in this file or knowledge already stored in SQLite.

### Grammar

Each `grammar` item requires `id`, `name`, `description`, and `formula`. `source_id` is optional.
Examples are derived from sentence relationships rather than duplicated on grammar concepts.

Grammar concepts are durable learning records, not general notes about a sentence. Write each concept
as one narrow recall target:

- Make `name` a specific learner-facing question or prompt that makes the expected recall clear.
- Make `description` answer that prompt directly. Put the short answer first and limit supporting
  explanation to what is needed to understand the rule in the linked sentences.
- Use `formula` only for an accurate productive pattern, transformation, or morphological breakdown.
  Do not invent a general formula from one observed form. When no symbolic pattern would clarify the
  concept, use a short concrete construction instead.
- Split independently testable facts into separate concepts. For example, the meanings of `pa` and
  the question marker `ba` should not share one concept merely because they occur together once.
- Do not generalize from a lyric, translation, or isolated example beyond what the source supports.
  Prefer a narrower reliable explanation over a broad rule with exceptions left unstated.
- Link a grammar UUID only to sentences that actually demonstrate that concept. Every linked sentence
  must agree with the concept's prompt, explanation, and formula.
- Prefer one strong, representative sentence relationship over several weak or incidental ones.
- Omit a proposed grammar concept when the source does not support a clear, reliable explanation.
  A sentence may have an empty `grammar_ids` array.

Good:

```json
{
  "id": "40000000-0000-4000-8000-000000000001",
  "name": "In makita kita, who does kita refer to?",
  "description": "Kita combines a first-person singular actor with a second-person singular object: ‘I … you.’",
  "formula": "verb + kita = I [verb] you"
}
```

This record presents one question, answers it immediately, and describes the construction shown by
the linked sentence. By contrast, avoid a broad topic label such as `Pronouns`, a paragraph surveying
several pronouns, or a formula that does not generate the cited form.

## Identity, comparison, and corrections

Every lesson, source, vocabulary item, sentence, and grammar concept has a lowercase canonical UUID.
Generate a UUID once and preserve it across corrections, imports, exports, and Anki updates.

- A UUID absent from SQLite is an insert.
- An existing UUID with identical normalized global content is unchanged.
- An existing UUID with different global content is a conflict unless `--update-existing` is used.
- Equal normalized content under different UUIDs is a validation error.
- Tags and relationship arrays are unordered sets for comparison and persistence.
- Lesson membership and per-lesson source provenance are associations, not global entity content.
- Reusing global knowledge in another lesson adds an association without duplicating the entity.
- Omitting a record never deletes or detaches stored knowledge. Packages are incremental.
- Explicit updates replace complete package-owned fields, tags, sentence relationships, and the
  included entity's association metadata for this lesson without altering other lessons.

The successful import checksum is SHA-256 over the exact accepted input bytes. Formatting an
otherwise equivalent document differently changes its package checksum but not its entity
insert/unchanged/conflict assessments.

## Diagnostics

Validation identifies values by JSON path, for example:

```text
lesson.json $.vocabulary[0].id: must be a lowercase canonical UUID
lesson.json $.sentences[1].vocabulary_ids[0]: Vocabulary UUID does not exist
```

Array indexes in paths are zero-based. Supplied values are escaped and length-limited. Structural
parsing must succeed before independent semantic errors can be collected. Validation is read-only.

## Generation prompt

```text
Create one Tagalog lesson as exactly one JSON document named lesson.json. Follow
docs/lesson-package.md and schema version 2 in docs/lesson-package.schema.json. Do not wrap the JSON
in Markdown fences and do not create CSV, YAML, Markdown, or additional input files. Use UTF-8,
Unicode NFC, lowercase canonical UUIDs, exact enum spelling, JSON arrays for tags and relationships,
and omit absent optional properties rather than using null. Preserve supplied UUIDs while correcting
content and generate UUIDv4 only for genuinely new records. Include source provenance, reuse known
UUIDs for cross-lesson relationships, and produce no unknown properties. Write each grammar concept
as one narrow learner-facing recall question with a concise direct answer. Split independently
testable facts, use formulas only when they accurately describe the linked forms, and associate a
grammar UUID only with sentences that genuinely demonstrate it. Do not generalize beyond the source
or create a grammar concept when a clear, reliable explanation is not supported.
```

## Output boundary

JSON is only the authoring and import format. The application still exports separate vocabulary,
sentence, and grammar TSV files because those are the delivery contract for Anki note types.
