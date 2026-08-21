# Automation CLI contract (v1)

Commands intended for automation accept `--format json`. They write exactly one JSON object to
standard output on success and exactly one JSON object to standard error on an expected operational
failure. Field names and meanings below are stable for contract version 1. Adding optional fields is
backward compatible; removing or renaming a field requires a new documented contract version.

## Exit codes

| Code | Meaning |
| --- | --- |
| `0` | The requested operation completed successfully. |
| `1` | An unexpected runtime, database, filesystem, or command-line execution failure occurred. |
| `2` | Expected validation, conflict, not-found, or export-destination failure. The JSON body identifies the failure. |
| `3` | Explicit deletion was refused because another knowledge record still references the entity. |

Picocli may use its standard non-zero codes for malformed command syntax before a command runs.

## Result objects

Nullable fields are emitted as JSON `null`; collections are always arrays, even when empty.

- `lesson validate`: `valid`, nullable `lesson_id`, `summary` (`inserts`, `updates`, `unchanged`,
  `conflicts`, `warnings`, `errors`), `records`, `errors`, and `warnings`. A record has `type`, `id`,
  and `status`. A diagnostic has `filename`, nullable `row`, `column`, and `value`, plus `message` and
  `guidance`.
- `lesson import`: success has `success`, `exact_rerun`, `import_run_id`, `lesson_id`,
  `package_checksum`, `schema_version`, `imported_at`, and `summary` (`inserted`, `updated`,
  `unchanged`, `newly_related`). Failure has `success`, `message`, and `errors`.
- `lesson list`: `lessons`; each item has `id`, `name`, nullable `description`, and `counts`.
- `lesson show`: success has `found`, `lesson`, `counts`, `sources`, `vocabulary`, `sentences`,
  `grammar`, and `import_history`. Not-found has `found`, `lesson_id`, and `message`.
- Entity `show`: success has `found`, the entity under its type name (`vocabulary`, `sentence`, or
  `grammar`), `lessons`, and its relevant semantic relationships. Not-found has `found`, `type`, `id`,
  and `message`.
- `anki export`: success has `success`, `lesson_id`, `output`, `exported_at`, and `files`; each file
  has `name`, `sha256`, and `row_count`. Failure adds `error` and `message`.
- `lesson publish`: always has `success`, `stage`, `imported`, and `exported`. `stage` is one of
  `validation`, `import`, `export`, or `complete`. After a committed import it also has `lesson_id`
  and `import_run_id`. An export-stage failure includes `error`, `message`, and `retry_command`.

Timestamps are ISO-8601 UTC strings, UUIDs are canonical lowercase strings, and checksums are
lowercase SHA-256 hex strings. Arrays emitted from stored queries and exports use deterministic UUID
ordering.
