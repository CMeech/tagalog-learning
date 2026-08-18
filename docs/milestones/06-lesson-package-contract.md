# Milestone 6 — Lesson Package Contract

Goal: define the stable file contract that an agent can generate from source material and that the
application can validate, import, and export without editorial workflow state.

## Product workflow

```text
source material -> generated lesson package -> validate/correct files -> import -> export to Anki
```

The lesson package is reviewed before import. A successful import means the material is accepted into
the collection. SQLite remains the durable source of truth after import; the package is a repeatable
input artifact, not a second database.

## Package layout

```text
lesson-package/
├── lesson.json
├── vocabulary.csv
├── sentences.csv
└── grammar.csv
```

`lesson.json` is required. At least one CSV file is required; empty entity files may be omitted.
Unknown files are ignored so source documents and working notes may be kept beside the package.

All files use UTF-8 and Unicode NFC normalization. JSON property names and CSV headers are exact and
case-sensitive. UUIDs are lowercase canonical strings and are stable across corrections, imports,
exports, and Anki updates.

## Manifest contract

`lesson.json` contains:

```json
{
  "schema_version": 1,
  "lesson": {
    "id": "00000000-0000-0000-0000-000000000000",
    "name": "Lesson name",
    "description": "Optional description"
  },
  "sources": [
    {
      "id": "00000000-0000-0000-0000-000000000000",
      "name": "Source name",
      "type": "BOOK",
      "reference": "Optional path, title, page, or URL"
    }
  ],
  "default_source_id": "00000000-0000-0000-0000-000000000000"
}
```

`sources` may be empty and `default_source_id` may be omitted. A supplied default must identify a
source in the manifest. Source types use the existing domain enum.

## CSV contracts

### `vocabulary.csv`

```text
id,tagalog,english,root_word,part_of_speech,difficulty,frequency_rank,source_id,tags
```

- Required: `id`, `tagalog`, `english`, and `part_of_speech`.
- `difficulty` defaults to `BEGINNER`.
- `root_word`, `frequency_rank`, `source_id`, and `tags` are optional.
- `tags` is a pipe-separated list of tag names.

### `sentences.csv`

```text
id,text,translation,difficulty,source_id,vocabulary_ids,grammar_ids
```

- Required: `id`, `text`, and `translation`.
- `difficulty` defaults to `BEGINNER`.
- `source_id`, `vocabulary_ids`, and `grammar_ids` are optional.
- Relationship columns are pipe-separated UUIDs and may reference records in the package or records
  already present in SQLite.

### `grammar.csv`

```text
id,name,description,formula,source_id
```

- Required: `id`, `name`, `description`, and `formula`.
- `source_id` is optional.
- Grammar examples are derived from sentence relationships rather than duplicated in this file.

When `source_id` is blank, the manifest's `default_source_id` applies. Pipe-separated values may not
contain literal pipes. Repeated values in a list are invalid. Scalar values are trimmed; meaningful
internal whitespace and embedded newlines are preserved according to standard CSV quoting rules.

## Identity and repeat-import rules

- [ ] The package UUID is the domain/database UUID; do not introduce a second external-key system.
- [ ] A UUID absent from SQLite represents a new record.
- [ ] An existing UUID with identical normalized content is unchanged.
- [ ] An existing UUID with different content is a conflict unless import explicitly enables updates.
- [ ] Updates replace the complete package-owned representation of the record, including tags and
      sentence relationships.
- [ ] Omitting a previously imported UUID from a later or corrected package leaves the SQLite record
      unchanged; packages are incremental and never imply synchronization or deletion.
- [ ] An exact content duplicate under a different UUID is a validation error with both IDs reported.
- [ ] UUIDs remain stable when generated files are corrected.

## Anki contract

The application writes one UTF-8 TSV file per entity type. Each file uses the UUID as its first field
so Anki can match existing notes when first-field updating is enabled.

The user imports these files manually through Anki. Direct Anki automation and AnkiConnect are not
part of this milestone.

- Vocabulary fields: `ID`, `Tagalog`, `English`, `Root Word`, `Part of Speech`, `Difficulty`, `Tags`,
  `Lesson`, `Source`.
- Sentence fields: `ID`, `Tagalog`, `English`, `Difficulty`, `Vocabulary`, `Grammar`, `Lesson`,
  `Source`.
- Grammar fields: `ID`, `Name`, `Description`, `Formula`, `Examples`, `Lesson`, `Source`.

Relationship fields contain readable Tagalog words, grammar names, and example sentences rather than
raw UUID lists. UUIDs remain the internal relationship keys.

## Implementation tasks

### M6.1 — Freeze the authoring contract

- [ ] Add a JSON Schema for `lesson.json` and document every property, default, and enum.
- [ ] Add canonical header-only CSV templates under `examples/import/`.
- [ ] Add a complete sample lesson package with Tagalog characters, relationships, tags, and source
      provenance.
- [ ] Document filename, UTF-8, NFC, UUID, quoting, whitespace, blank, list-delimiter, and maximum-size
      rules.
- [ ] Document how an agent chooses stable UUIDs and preserves them while correcting a package.
- [ ] Add a copyable generation prompt that requires this exact contract.

### M6.2 — Freeze the Anki contract

- [ ] Document the three Anki note types and exact field order.
- [ ] Provide copyable front/back card templates and shared CSS for vocabulary, sentences, and grammar.
- [ ] Include native Anki TTS tags for Tagalog fields and explain device voice availability.
- [ ] Document column mapping, first-field update matching, HTML handling, tags, and deck selection for
      each TSV file.
- [ ] Document the short recurring manual-import procedure and how to locate a note by UUID when an
      explicit database deletion must also be applied in Anki.
- [ ] Define how empty optional values and readable relationship fields render.
- [ ] Add expected TSV fixtures for the sample lesson package.

### M6.3 — Verify the contract before implementation

- [ ] Walk the sample package through every identity, update, relationship, and duplicate rule on
      paper.
- [ ] Verify that next week's sentence can reference a vocabulary UUID from an earlier package.
- [ ] Verify that correcting and re-importing a package preserves the UUID used by Anki.
- [ ] Verify that removing a row from a package does not delete or detach previously imported content.
- [ ] Verify all desired card content can be produced from the manifest and three CSV files.
- [ ] Record any intentional v1 omissions so the importer does not accidentally grow new formats.

## Definition of done

- [ ] Another agent can generate a conforming package without reading application code.
- [ ] Every package value has an unambiguous database and Anki destination.
- [ ] Cross-package relationships and correction/re-import behavior are fully specified.
- [ ] The sample package has deterministic expected Anki outputs.
- [ ] No review or export status is part of the domain or persistence design.

## Version 1 omissions

- Anki package (`.apkg`) generation.
- Direct Anki/AnkiConnect integration; TSV files are imported manually.
- Stored audio and provider-backed TTS.
- Multiple lessons in one package.
- Literal pipe characters inside list values.
- Automated linguistic judgment inside the application; content quality is handled while generating
  and correcting the package.
