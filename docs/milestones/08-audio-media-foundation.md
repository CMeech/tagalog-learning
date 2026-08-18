# Milestone 8 — Audio and Media Foundation

Goal: manage durable audio files independently of any TTS vendor and associate them with learning
content.

## Scope

This milestone supports audio files for vocabulary and sentences. Image and general attachment
support remain deferred, but the design must not prevent adding them later.

## Tasks

- [ ] Add an audio asset domain model containing an ID, content association, purpose, format, size,
      checksum, duration when known, origin, and creation timestamp.
- [ ] Add Flyway-managed audio metadata and content-association tables.
- [ ] Define a configurable media directory outside SQLite; store portable relative paths in the
      database rather than binary audio blobs or machine-specific absolute paths.
- [ ] Add a file-storage abstraction that writes atomically and derives collision-safe names from
      stable IDs and checksums.
- [ ] Add commands to attach an existing audio file, list/show audio, and remove an association.
- [ ] Verify supported MIME types and file signatures, size limits, checksums, and missing/orphaned
      files.
- [ ] Extend database validation to report missing files, checksum mismatches, unsupported formats,
      and dangling associations.
- [ ] Define replacement behavior while retaining enough origin metadata to regenerate audio later.
- [ ] Make exporters able to consume associated approved audio without knowing its storage layout.
- [ ] Document backup and restore requirements for the database and media directory together.

## Validation

- [ ] Unit tests cover asset validation, naming, and association rules.
- [ ] Integration tests cover attach, inspect, replace, detach, and validation commands.
- [ ] Tests prove interrupted or failed copies do not leave partial files or database rows.
- [ ] Tests prove duplicate content is detected by checksum and handled deterministically.
- [ ] Export tests prove associated audio is available through the provider-neutral export model.

## Decisions to confirm during implementation

- Which first formats to accept. MP3 is the likely portable default; WAV may be useful for lossless
  provider output but consumes substantially more space.
- Whether one content record may keep multiple voice/language variants or only one active audio file
  per purpose in the first version.
