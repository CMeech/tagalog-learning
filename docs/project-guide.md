# Project Guide

## Purpose and vision

The Tagalog Learning Platform is a personal language knowledge management system. SQLite is the source of truth, language knowledge is modeled independently from Anki, and Anki is an export target. Content must be structured, validated, reusable, and suitable for future automation.

The application stores vocabulary, sentences, grammar concepts, lessons, sources, tags, and learning metadata. It does not replace Anki; structured knowledge flows from the platform through export systems into Anki.

## Accepted technology decisions

- **Language:** Kotlin for type safety, JVM interoperability, concise syntax, and tooling.
- **CLI:** Picocli for mature command hierarchies and natural Kotlin integration.
- **Database:** SQLite for portability, simple backups, and zero infrastructure.
- **Migrations:** Flyway for version-controlled, production-proven schema changes.
- **Database library:** Exposed for lightweight, Kotlin-native, type-safe SQL.
- **Testing:** JUnit 5.
- **Development environment:** Docker containers through OrbStack. The container supplies Java and Gradle; a host JDK is not required. Container validation is authoritative.
- **Audio:** Version 1 uses AwesomeTTS inside Anki. The application stores text, sentences, and metadata but does not generate audio. Azure, OpenAI, ElevenLabs, and local TTS engines remain future options.

## Architecture

The dependency direction is:

```text
CLI -> Application -> Domain -> Infrastructure -> SQLite
```

- **CLI:** parse commands, validate arguments, call application services, and display results. Commands include `init`, `version`, `validate`, and `migrate`.
- **Application:** execute workflows and coordinate use cases such as importing vocabulary, validating the database, and generating exports.
- **Domain:** own business concepts, rules, and relationships for vocabulary, sentences, grammar concepts, lessons, sources, and tags.
- **Infrastructure:** provide database, file, and external-system integrations through tools such as SQLite, Flyway, and Exposed.

## Target repository structure

```text
tagalog-learning/
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── Dockerfile
├── docker-compose.yml
├── docs/
│   ├── task-context.md
│   ├── project-guide.md
│   ├── conventions.md
│   ├── roadmap.md
│   └── milestones/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   └── resources/db/migration/
│   └── test/
└── .github/workflows/
```

## Version 0.1 completion

- [ ] Kotlin application runs
- [ ] Docker runs
- [ ] SQLite initializes
- [ ] Flyway migrations execute
- [ ] CLI works
- [ ] Tests pass in Docker
- [ ] Documentation exists
- [ ] Domain model is implemented
- [ ] Future roadmap is documented

Version 0.1 should be a stable foundation that can gain intelligence and automation without redesign.
