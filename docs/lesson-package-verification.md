# Lesson Package Contract Verification

This is the pre-implementation walkthrough for contract version 1. It uses the package under
[`examples/lesson-package`](../examples/lesson-package) and its deterministic expected Anki files.

## Identity, repeat import, relationships, and duplicates

1. **One identity:** lesson `1000…0001`, source `2000…0001`, four vocabulary IDs, two grammar IDs,
   and three sentence IDs are used directly as domain and database UUIDs. No external key is made.
2. **New records:** against an empty database, every sample UUID is absent, so all eleven domain
   records are inserted along with tags and sentence relationships.
3. **Unchanged records:** importing the same normalized package again compares equal by UUID and
   complete content. It performs no entity, tag, or relationship changes.
4. **Conflicting correction:** changing `José` to `Jose` while keeping sentence ID `5000…0003`
   conflicts by default. With updates explicitly enabled, that UUID's complete scalar content and
   relationship sets replace the stored representation. The next TSV retains the same first field,
   so Anki updates the same note.
5. **Complete replacement on update:** removing grammar ID `4000…0002` from sentence `5000…0003`'s
   `grammar_ids` while updates are enabled removes that relationship. Vocabulary tags and both
   sentence relationship collections follow the same complete-replacement rule.
6. **Package omission is not deletion:** removing vocabulary row `3000…0004` from a corrected package
   does not delete `po` or detach it from already stored sentences. Only an explicit database delete
   can do so, and its Anki note must then be deleted manually by UUID.
7. **Different-ID content duplicate:** adding the same normalized `po` vocabulary content—including
   its resolved lesson, source, and tag set—as UUID `3000…0099` is rejected and reports both
   `3000…0004` and `3000…0099`. The documented per-entity fingerprints make this check deterministic
   against both the package and SQLite.
8. **Stable correction identity:** spelling, translation, source, difficulty, tag, and relationship
   corrections never generate another UUID. This preserves database identity and Anki scheduling.
9. **Relationships:** every sample sentence reference resolves to the corresponding vocabulary and
   grammar record. The expected TSVs render words, names, and example sentences rather than IDs.
10. **Default provenance:** every blank sample `source_id` resolves to manifest source `2000…0001`.

## Cross-package scenario

Suppose a later package contains sentence `60000000-0000-4000-8000-000000000001` with
`vocabulary_ids=30000000-0000-4000-8000-000000000001` and does not repeat the vocabulary row. The
reference resolves against SQLite after this sample has been imported. It is valid; a relationship
target need not be present in the same package. It is invalid only when absent from both locations or
when its entity type is wrong.

## Card-data sufficiency

- Vocabulary cards obtain their ID, front, meaning, root, part of speech, difficulty, and tags from
  `vocabulary.csv`; lesson and readable source values come from `lesson.json`.
- Sentence cards obtain their ID, two faces, and difficulty from `sentences.csv`; readable vocabulary
  and grammar are resolved through its UUID lists; lesson and source come from the manifest.
- Grammar cards obtain ID, name, description, and formula from `grammar.csv`; examples are the
  Tagalog text of every globally stored sentence that refers to each grammar ID, including sentences
  associated with other lessons. Lesson and source come from the exported lesson association.
- Optional and empty relationship rendering is specified in the Anki contract. No desired card field
  requires application code knowledge or data outside the four package files (except an explicitly
  allowed relationship to a previously imported SQLite record).

## Complete destination map

| Package value | SQLite/domain destination | Anki destination |
| --- | --- | --- |
| `schema_version` | Import compatibility check; not persisted as domain data. | Not exported. |
| `lesson.id` | `Lesson.id` | Used to resolve the lesson; not displayed because notes already carry their own UUID. |
| `lesson.name` | `Lesson.name` | Every TSV `Lesson` field. |
| `lesson.description` | `Lesson.description` | Not exported in v1. |
| `sources[].id` | `Source.id` and row source foreign keys | Used to resolve readable source text; not displayed. |
| `sources[].name` | `Source.name` | Every sourced TSV row's `Source` field. |
| `sources[].type` | `Source.type` | Not exported in v1. |
| `sources[].reference` | `Source.reference` | Appended to the readable `Source` field when present. |
| `default_source_id` | Import-time default for blank row source IDs. | Indirectly determines `Source`. |
| Vocabulary scalar columns | `Vocabulary` fields plus the manifest `Lesson.id` relationship | Matching vocabulary fields, except `frequency_rank`, which is database-only in v1. |
| Vocabulary `source_id` | Vocabulary-to-source reference | Readable `Source`. |
| Vocabulary `tags` | Vocabulary-to-tag relationships and `Tag.name` | Anki tags metadata. |
| Sentence scalar columns | `Sentence` fields plus the manifest `Lesson.id` relationship | Matching sentence fields. |
| Sentence `source_id` | Sentence-to-source reference | Readable `Source`. |
| Sentence `vocabulary_ids` | Sentence-to-vocabulary relationships | Readable `Vocabulary`. |
| Sentence `grammar_ids` | Sentence-to-grammar relationships | Readable `Grammar`; inversely supplies grammar `Examples`. |
| Grammar scalar columns | `GrammarConcept` fields plus the manifest `Lesson.id` relationship | Matching grammar fields. |
| Grammar `source_id` | Grammar-to-source reference | Readable `Source`. |

Values explicitly listed as not exported remain durable SQLite knowledge; no package value is
silently discarded during import.

## Intentional version 1 omissions

Version 1 accepts only one manifest plus the three named CSV shapes. It does not accept `.apkg`,
AnkiConnect/direct automation, stored or provider-generated audio, multiple lessons, literal pipes in
list items, alternate delimiters, alternate headers or filename aliases, embedded editorial/review or
export status, deletion/synchronization semantics, or automated linguistic judgment. Unknown files
are ignored as working material and never interpreted as another import format.

These are explicit non-features. An importer must reject or ignore them as described rather than
silently extending version 1.
