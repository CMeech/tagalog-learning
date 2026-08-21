# Lesson Package Verification

The canonical version 2 input is
[`examples/lesson-package/lesson.json`](../examples/lesson-package/lesson.json). Its deterministic
Anki outputs remain under `examples/lesson-package/expected-anki/`.

## Contract checks

1. The document contains one lesson, one source, four vocabulary items, three sentences, and two
   grammar concepts.
2. Every authored record has one stable lowercase canonical UUID.
3. Missing entity `source_id` values resolve to `default_source_id`.
4. Tags and sentence relationships are native JSON arrays with no duplicate items.
5. Sentence vocabulary and grammar UUIDs resolve against the complete document.
6. Global entity content excludes lesson membership and per-lesson source provenance.
7. Reusing a known UUID in a later lesson adds an association without duplicating global knowledge.
8. Correcting global content preserves its UUID and requires `--update-existing`.
9. Omitting an imported entity never deletes or detaches it.
10. Equal normalized content under a different UUID is rejected.

## Anki projection

- Vocabulary TSV rows use vocabulary scalar values and tags plus readable lesson and source names.
- Sentence TSV rows replace relationship UUIDs with readable vocabulary and grammar values.
- Grammar TSV rows derive examples from sentences that reference each grammar UUID.
- Every TSV uses the global entity UUID as its first field so Anki can update existing notes.
- The JSON input format does not change the established Anki note types, field order, templates, CSS,
  or manual import settings.

## Intentional boundaries

Version 2 accepts only a single `lesson.json` input. It does not accept CSV input, package
directories, `.apkg`, YAML, Markdown, JSON Lines, direct Anki collections, audio, or more than one
lesson. SQLite remains the durable source of truth after successful import.
