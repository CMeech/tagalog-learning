# Initial setup

Complete this setup once before importing the first weekly lesson. The application produces three
TSV files for three separate Anki note types. Anki note types belong to the whole collection, so all
three can be used in the same deck or in different decks.

## Initialize the application

Start OrbStack, then build and initialize the application from the repository root. Repeating `init`
later is safe; it applies only migrations that have not already run.

```shell
mkdir -p run
docker compose build
docker compose run --rm app init
```

## Create the Anki note types

Perform the following steps in Anki Desktop. For each note type:

1. Choose **Tools → Manage Note Types**.
2. Click **Add**.
3. Select **Add: Basic**, not a reversed or optional-reversed type.
4. Enter the exact note-type name shown below.
5. Select the new note type and click **Fields…**.
6. Rename, add, remove, and reorder its fields to match the listed order exactly.
7. Return to the note-type window and click **Cards…**.
8. Replace the complete contents of **Front Template**, **Back Template**, and **Styling** with the
   contents of the linked repository files.

The `ID` field must remain first because weekly imports use it to update the same Anki note without
resetting its scheduling. Do not create a field named `Tags`: Anki reserves that name for its built-in
note tags.

### Tagalog Vocabulary

Name the note type `Tagalog Vocabulary` and give it these fields:

1. `ID`
2. `Tagalog`
3. `English`
4. `Root Word`
5. `Part of Speech`
6. `Difficulty`
7. `Lesson`
8. `Source`

Copy the complete contents of:

- **Front Template:** [`vocabulary-front.html`](../anki/templates/vocabulary-front.html)
- **Back Template:** [`vocabulary-back.html`](../anki/templates/vocabulary-back.html)
- **Styling:** [`shared.css`](../anki/templates/shared.css)

### Tagalog Sentence

Name the note type `Tagalog Sentence` and give it these fields:

1. `ID`
2. `Tagalog`
3. `English`
4. `Difficulty`
5. `Vocabulary`
6. `Grammar`
7. `Lesson`
8. `Source`

Copy the complete contents of:

- **Front Template:** [`sentence-front.html`](../anki/templates/sentence-front.html)
- **Back Template:** [`sentence-back.html`](../anki/templates/sentence-back.html)
- **Styling:** [`shared.css`](../anki/templates/shared.css)

### Tagalog Grammar

Name the note type `Tagalog Grammar` and give it these fields:

1. `ID`
2. `Name`
3. `Description`
4. `Formula`
5. `Examples`
6. `Lesson`
7. `Source`

Copy the complete contents of:

- **Front Template:** [`grammar-front.html`](../anki/templates/grammar-front.html)
- **Back Template:** [`grammar-back.html`](../anki/templates/grammar-back.html)
- **Styling:** [`shared.css`](../anki/templates/shared.css)

The vocabulary and sentence templates request native Anki TTS with the `fil_PH` locale. A card still
works when that voice is unavailable; it simply may not speak until a compatible Filipino voice is
installed on the review device.

## Verify the first import

Publish a lesson using the process in [Weekly lesson operation](weekly-operation.md), then import each
generated TSV through **File → Import** in Anki Desktop:

| File | Note type |
| --- | --- |
| `vocabulary.tsv` | `Tagalog Vocabulary` |
| `sentences.tsv` | `Tagalog Sentence` |
| `grammar.tsv` | `Tagalog Grammar` |

For each file:

1. Confirm that Anki selected the note type shown in the table. Stop if it still shows `Basic`.
2. Select the destination deck. Deck choice affects new cards but does not move existing cards when
   later imports update their notes.
3. Keep **Allow HTML in fields** off.
4. Set **Existing notes** to **Update**.
5. Set **Match scope** to **Note type** and match using the first field, `ID`.
6. Confirm that every TSV column maps to the same-named custom field. For vocabulary only, map the
   seventh column to Anki's built-in **Tags** metadata rather than a custom field.
7. Import the file and inspect Anki's summary before continuing.

The order of the three files does not matter because readable relationship values are already
rendered into each export. After all files are imported, review one card of each note type and sync
Anki Desktop to AnkiWeb. AnkiWeb receives the notes through synchronization; it is not the TSV import
surface.

For the complete field and rendering contract, see [Anki TSV and Note-Type Contract](anki-contract.md).

