# Weekly lesson operation

SQLite is the source of truth. Keep each reviewed `lesson.json` file so later corrections can
preserve its UUIDs. The commands below use only Docker/OrbStack and the Compose-managed database
volume.

## Canonical Docker workflow

Create a host directory for exports, build and initialize the application, then validate, import,
inspect, and export the sample. Each export target must not already exist.

```shell
mkdir -p run
docker compose build
docker compose run --rm app init
docker compose run --rm -v "$PWD/examples:/examples:ro" app lesson validate /examples/lesson-package/lesson.json
docker compose run --rm -v "$PWD/examples:/examples:ro" app lesson import /examples/lesson-package/lesson.json
docker compose run --rm app lesson show 10000000-0000-4000-8000-000000000001
docker compose run --rm -v "$PWD/run:/exports" app anki export --lesson 10000000-0000-4000-8000-000000000001 --output /exports/week-1
docker compose run --rm -v "$PWD/run:/exports" app anki export --lesson 10000000-0000-4000-8000-000000000001 --output /exports/week-1-repeat
```

Add `--format json` to validation, import, inspection, export, or publish for automation. The stable
objects and exit codes are defined in [the CLI contract](cli-contract.md).

For the normal weekly path, the equivalent convenience command is:

```shell
docker compose run --rm -v "$PWD/examples:/examples:ro" -v "$PWD/run:/exports" app lesson publish /examples/lesson-package/lesson.json --output /exports/week-1
```

`publish` composes the same validation, import, and export services. If export fails after import,
the SQLite commit is retained and the command prints an exact `tagalog anki export` retry command.
Use that command with a new destination; do not import the package again merely to retry export.

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

Before the first import, create the three note types and copy the supplied card templates exactly as
described in [the Anki contract](anki-contract.md#note-types-and-field-order). For every weekly TSV:

1. Use Anki desktop **File → Import** and select the note type named by the file header.
2. Select a deck; this affects new notes, not existing-note updates.
3. Keep **Allow HTML in fields** off and confirm columns map in the documented order. Vocabulary
   column 7 maps to Anki's built-in **Tags** metadata.
4. Match duplicates on the first field (`ID`), scope matching to note type, and enable updating
   existing notes.

Import `vocabulary.tsv`, `sentences.tsv`, and `grammar.tsv` when present. UUID matching updates notes
without resetting scheduling. After an explicit database deletion, search Anki Browse for the UUID,
verify the note type and `ID`, and delete the note—not merely one card—manually. TSV imports cannot
delete Anki notes.
