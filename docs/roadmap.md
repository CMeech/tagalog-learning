# Roadmap

The version 0.1 foundation is complete. The next phase prioritizes the shortest weekly content loop:
generate a lesson package from source material, validate it, import it, and export it to Anki.

## Planned milestones

1. [Milestone 6 — Lesson Package Contract](milestones/06-lesson-package-contract.md)
2. [Milestone 7 — Weekly Lesson Pipeline](milestones/07-csv-import.md)

Content quality is handled while generating and validating the lesson package. The application does
not maintain editorial or export statuses; successful import accepts the package into SQLite, and any
imported lesson can be exported repeatedly. Anki delivery uses a short manual TSV import rather than
a custom Anki integration.

## Later work

- **Vocabulary category coverage:** query vocabulary coverage by stored tags such as lifestyle,
  colors, and food. Define the meaning of sufficient coverage and the command interface when this
  work is planned.
- **In-application AI enrichment:** generate example sentences, explanations, related vocabulary,
  and grammar suggestions inside the application. Agent-generated lesson packages are the current
  solution.
- **Audio and media foundation:** manage durable audio files independently of a TTS vendor. See the
  existing [planning draft](milestones/08-audio-media-foundation.md).
- **TTS providers:** generate stored audio through providers such as Azure, OpenAI, or ElevenLabs.
  Anki's built-in TTS is the current solution. See the existing
  [planning draft](milestones/09-tts-providers.md).
- **Repository implementations:** create full persistence abstractions after the database schema is finalized.
- **Anki package generation:** generate complete Anki packages automatically.
- **Direct Anki integration:** update a running Anki collection through an integration such as
  AnkiConnect if manual TSV imports become burdensome.
- **Domain services:** introduce richer business workflows.
