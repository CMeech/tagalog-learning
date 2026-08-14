# Deferred Roadmap

Do not implement these features until the foundation is stable.

- **AI enrichment:** generate example sentences, explanations, related vocabulary, and grammar suggestions. Requires a stable domain model and review workflow.
- **TTS providers:** move audio generation into the application using a provider such as Azure, OpenAI, or ElevenLabs. AwesomeTTS in Anki is the current solution.
- **Repository implementations:** create full persistence abstractions after the database schema is finalized.
- **CSV import/export:** support bulk content movement.
- **Anki package generation:** generate complete Anki packages automatically.
- **Review workflow:** move content through `Generated -> Needs Review -> Approved -> Exported`.
- **Media management:** manage audio, images, and files.
- **Domain services:** introduce richer business workflows.
