# Anki TSV and Note-Type Contract (v1)

The application exports three UTF-8, NFC-normalized tab-separated files: `vocabulary.tsv`,
`sentences.tsv`, and `grammar.tsv`. Rows are ordered by UUID ascending. Relationship values are
ordered by their UUIDs, making repeated exports byte-for-byte deterministic.

Each file begins with `#separator:Tab`, `#html:false`, `#notetype:...`, and `#columns:...` headers.
Vocabulary also declares `#tags column:7`. Exported values are plain text, not HTML; Anki's **Allow
HTML in fields** option must remain off. Tabs, quotes, CR, and LF are escaped with CSV-style double
quoting. Empty optional values are zero-length fields: they display nothing and never display a
placeholder such as `null`, `[]`, or `—`.

## Note types and field order

Create these note types exactly once, preserving the listed order:

1. **Tagalog Vocabulary**: `ID`, `Tagalog`, `English`, `Root Word`, `Part of Speech`, `Difficulty`,
   `Lesson`, `Source`. The logical TSV position between `Difficulty` and `Lesson` is `Tags`; it maps
   to Anki's built-in tags metadata, not a custom field. Anki reserves the name `Tags`, and
   `{{Tags}}` renders that metadata in the template.
2. **Tagalog Sentence**: `ID`, `Tagalog`, `English`, `Difficulty`, `Vocabulary`, `Grammar`, `Lesson`,
   `Source`.
3. **Tagalog Grammar**: `ID`, `Name`, `Description`, `Formula`, `Examples`, `Lesson`, `Source`.

This distinction makes the milestone's logical vocabulary order `ID`, `Tagalog`, `English`, `Root
Word`, `Part of Speech`, `Difficulty`, `Tags`, `Lesson`, `Source` importable without attempting to
create an invalid field named `Tags`.

## Value mapping and rendering

| TSV | Column | Value |
| --- | --- | --- |
| all | `ID` | Domain/database UUID; always the first field. |
| vocabulary | `Tagalog`, `English`, `Root Word`, `Part of Speech`, `Difficulty` | Corresponding vocabulary values; optional root word is blank. |
| vocabulary | `Tags` | Unique tag names, Unicode-code-point sorted and joined with one space for Anki. |
| sentences | `Tagalog`, `English`, `Difficulty` | Sentence text, translation, and difficulty. |
| sentences | `Vocabulary` | Related Tagalog vocabulary forms, relationship-UUID ordered and joined with `; `. |
| sentences | `Grammar` | Related grammar names, relationship-UUID ordered and joined with `; `. |
| grammar | `Name`, `Description`, `Formula` | Corresponding grammar values. |
| grammar | `Examples` | Related sentence Tagalog text, sentence-UUID ordered and joined with `; `. |
| all | `Lesson` | Lesson name. |
| all | `Source` | Source name followed by ` — ` and its reference when present; just the name otherwise; blank when absent. |

An empty relationship produces a blank field. Relationship fields contain readable text, never UUIDs.
Repeated readable values are retained when they come from distinct related UUIDs.

## Card templates

Copy the files under [`anki/templates`](../anki/templates) into each note type's **Front Template**,
**Back Template**, and **Styling** boxes. Each note type creates one forward card.

The templates use native Anki TTS, for example `{{tts fil_PH:Tagalog}}`. TTS is generated on the
review device and is not stored audio. Voice names and language availability come from the operating
system or Anki client; `fil_PH` may be unavailable on a particular device. Install a Filipino voice
in the OS where supported, restart Anki, and temporarily add `{{tts-voices:}}` to a template to see
available voices. If no compatible voice exists, the card still renders but does not speak.

## Recurring manual import

For each TSV file:

1. In desktop Anki choose **File → Import**, select the TSV, and choose the matching existing note
   type named by its `#notetype` header.
2. Select the desired deck. Deck selection affects new cards only; updates remain in their current
   deck. Do not configure a card-template deck override unless that behavior is intended.
3. Confirm the preview maps columns by the exact order above. For vocabulary, confirm column 7 maps
   to Anki **Tags**, not a note field. Leave **Allow HTML in fields** off.
4. Set duplicate matching to the first field, scope it to note type, and choose the option that
   updates existing notes. Import and inspect the summary before continuing with the next file.

Because `ID` is first, a corrected export updates the same Anki note while preserving scheduling.
The application UUID is deliberately not supplied as Anki's internal GUID.

Tags are imported only for vocabulary notes from the seventh TSV column. The application emits them
space-separated as required by Anki. Sentence and grammar exports do not modify Anki tags.

Package omission never deletes data in SQLite or Anki. After an explicit database deletion, locate
its Anki note in **Browse** by searching the UUID (or `ID:<uuid>` where field searches are supported),
verify its `ID` field and note type, then delete that note manually. Deleting a card instead of the
note may leave sibling cards if the note type is later expanded.

The deterministic expected output for the sample package is under
[`examples/lesson-package/expected-anki`](../examples/lesson-package/expected-anki).

For current import and TTS behavior, see the official [Anki text-file import
manual](https://docs.ankiweb.net/importing/text-files.html) and [field/TTS
manual](https://docs.ankiweb.net/templates/fields.html).
