# Milestone 9 — TTS Providers

Goal: generate Tagalog audio through a replaceable provider and store the result through the media
foundation.

## Tasks

- [ ] Define a provider-neutral TTS interface with text, locale, voice, format, and speaking options
      as input and audio plus provider metadata as output.
- [ ] Add configuration loading for provider, voice, locale, output format, and media location, with
      CLI flags overriding configuration.
- [ ] Read API credentials from environment variables or an OS-backed secret mechanism; never store
      secrets in SQLite, config files, logs, or command output.
- [ ] Implement one provider first, selected after a short quality/cost evaluation for Tagalog.
- [ ] Add `tagalog audio generate <type> <id>` and an explicit batch form filtered by lesson or
      import-run ID.
- [ ] Add `--dry-run` to show text, provider, voice, estimated request count, and replacement behavior
      without calling the provider.
- [ ] Require an explicit command and show the request count before generation so TTS never incurs
      cost merely because content was imported.
- [ ] Add bounded retries for transient failures, rate-limit handling, timeouts, and resumable batch
      behavior that does not regenerate successful items.
- [ ] Store provider name, voice, locale, model when available, source-text checksum, and generation
      timestamp with each audio asset.
- [ ] Detect stale audio when its source text changes and expose regeneration through an explicit
      command.
- [ ] Print per-item and batch summaries without logging credentials or provider response bodies that
      may contain sensitive data.
- [ ] Document provider setup, expected costs, dry runs, generation, failure recovery, and switching
      providers.

## Validation

- [ ] Contract tests exercise providers through a fake implementation without network access.
- [ ] Tests cover configuration precedence and missing/invalid credential errors.
- [ ] Tests cover partial batch failure, retry limits, resume behavior, and idempotency.
- [ ] Tests prove audio metadata captures provenance and stale detection follows content edits.
- [ ] An opt-in integration smoke test verifies the selected provider without running in the default
      build or exposing secrets.

## Decisions to confirm before implementation

- The first provider and Tagalog voice. Compare current Tagalog voice quality, pricing, licensing,
  output formats, rate limits, and credential setup for OpenAI, Azure, and ElevenLabs.
- Whether audio is generated for Tagalog text only or also for English translations.
- Whether generation happens only on explicit commands or can be triggered automatically after
  approval. Explicit generation is the safer first version.
