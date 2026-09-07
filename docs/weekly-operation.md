# Weekly lesson operation

SQLite is the source of truth. Keep each reviewed `lesson.json` file so later corrections can
preserve its UUIDs. The commands below use only Docker/OrbStack and the Compose-managed database
volume.

## Normal weekly workflow

Build the application during initial setup and after application changes. Run `init` after every
build or update before publishing; the SQLite volume persists across container replacement, but a
persistent database does not receive new schema migrations until `init` runs. Repeating `init` is
safe and reports that the database is up to date.

```shell
mkdir -p run
docker compose build
docker compose run --rm app init
```

If publishing reports a missing database table, stop and run `docker compose run --rm app init`, then
retry the same package. A failure before the import commit does not create a partial lesson.

For each reviewed lesson, publish its single `lesson.json` file. This command validates, imports, and
exports the lesson. The output directory must not already exist; use a new name for every export.
The sample command is:

```shell
docker compose run --rm -v "$PWD/examples:/examples:ro" -v "$PWD/run:/exports" app lesson publish /examples/lesson-package/lesson.json --output /exports/week-1
```

Before publishing generated grammar, review each concept for learning quality as well as schema
validity:

- Its `name` asks for one identifiable piece of knowledge, and its `description` answers it directly.
- Its `formula` accurately describes the cited form rather than extrapolating from one example.
- Each related sentence actually demonstrates the concept.
- Independent facts are separate concepts, and uncertain or incidental observations are omitted.

It is valid to leave `grammar` or a sentence's `grammar_ids` empty. Retaining only vocabulary and
sentence material is preferable when a grammar explanation is not yet clear enough to review.

`publish` composes the same validation, import, and export services. If export fails after import,
the SQLite commit is retained and the command prints an exact `tagalog anki export` retry command.
That command is expressed from inside the application container. When running through Docker, wrap
it with the export mount as shown below and choose a new destination; do not import the package again
merely to retry export.

```shell
docker compose run --rm -v "$PWD/run:/exports" app anki export --lesson <lesson-id> --output /exports/<new-directory>
```

Add `--format json` to validation, import, inspection, export, or publish for automation. The stable
objects and exit codes are defined in [the CLI contract](cli-contract.md).

## Diagnostic workflow

Use the individual commands when reviewing or troubleshooting a lesson. They are not additional
steps required before `publish`, because import and publish perform validation internally. The
following example uses two distinct export destinations so every command can be run in sequence:

```shell
docker compose run --rm -v "$PWD/examples:/examples:ro" app lesson validate /examples/lesson-package/lesson.json
docker compose run --rm -v "$PWD/examples:/examples:ro" app lesson import /examples/lesson-package/lesson.json
docker compose run --rm app lesson show 10000000-0000-4000-8000-000000000001
docker compose run --rm -v "$PWD/run:/exports" app anki export --lesson 10000000-0000-4000-8000-000000000001 --output /exports/week-1-manual
docker compose run --rm -v "$PWD/run:/exports" app anki export --lesson 10000000-0000-4000-8000-000000000001 --output /exports/week-1-repeat
```

## Correcting an imported package

Retain every existing lesson, source, vocabulary, sentence, and grammar UUID. Edit only the incorrect
content or relationships in the retained package, then validate it. A changed existing UUID is
reported as a conflict by default, which protects against accidental replacement. After reviewing
the conflict, validate and import explicitly with updates enabled:

```shell
docker compose run --rm -v "$PWD/examples:/examples:ro" app lesson validate /examples/lesson-package/lesson.json --update-existing
docker compose run --rm -v "$PWD/examples:/examples:ro" app lesson import /examples/lesson-package/lesson.json --update-existing
```

Validation accepts `--update-existing` so intentional corrections can be checked before import.
Omitting a row never deletes or detaches stored knowledge. Use an explicit entity `delete` command
when deletion is truly intended.

## Anki setup and recurring imports

Before the first import, complete [Initial setup](initial-setup.md), which walks through creating all
three note types, fields, card templates, and shared styling. The exported TSVs are note files rather
than packaged Anki decks, so import them in Anki Desktop and then sync them to AnkiWeb. For every
weekly TSV after that:

1. Use Anki Desktop **File → Import** and select the note type named by the file header. Stop rather
   than import if Anki shows `Basic`; select or create the required custom note type first.
2. Select a deck; this affects new notes, not existing-note updates.
3. Keep **Allow HTML in fields** off and confirm columns map in the documented order. Vocabulary
   column 7 maps to Anki's built-in **Tags** metadata.
4. Match duplicates on the first field (`ID`), scope matching to note type, and enable updating
   existing notes.
5. Inspect the import summary, then sync Anki Desktop to AnkiWeb.

Import `vocabulary.tsv`, `sentences.tsv`, and `grammar.tsv` when present; their order does not matter
because readable relationships are already rendered into each file. UUID matching updates notes
without resetting scheduling. After an explicit database deletion, search Anki Browse for the UUID,
verify the note type and `ID`, and delete the note—not merely one card—manually. TSV imports cannot
delete Anki notes.
