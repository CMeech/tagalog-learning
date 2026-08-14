# Tagalog Learning Platform
# Implementation Playbook

Version: 0.1.0

Status: Foundation Planning

---

# 1. Purpose

This document is the implementation guide for the Tagalog Learning Platform.

The purpose of this project is to create a personal language knowledge management system where:

- SQLite is the source of truth.
- Language knowledge is modeled independently from Anki.
- Anki is treated as an export target.
- Learning content is structured, validated, and reusable.
- Future automation can generate learning materials without rebuilding the system.

This document is designed to be consumed by:

- Developers
- Future maintainers
- AI coding assistants
- Automated implementation agents

Every milestone contains:

- Objectives
- Tasks
- Validation criteria
- Definition of Done

---

# 2. Project Vision

The system manages a learner's Tagalog knowledge.

The application stores:

- Vocabulary
- Sentences
- Grammar concepts
- Lessons
- Sources
- Tags
- Learning metadata

The system does not replace Anki.

Instead:

```
Tagalog Learning Platform
            |
            |
            v

Structured Knowledge Database

            |
            |
            v

Export Systems

            |
            |
            v

Anki
```

---

# 3. Technology Decisions

## Language

Decision:

Kotlin

Reason:

- Strong type safety
- Excellent JVM ecosystem
- Good interoperability
- Concise syntax
- Good tooling

Status:

Accepted

---

## CLI Framework

Decision:

Picocli

Reason:

- Mature JVM CLI framework
- Supports command hierarchy
- Works naturally with Kotlin

Status:

Accepted

---

## Database

Decision:

SQLite

Reason:

- Portable
- No infrastructure required
- Excellent for personal applications
- Easy backups

Status:

Accepted

---

## Migration Tool

Decision:

Flyway

Reason:

- Version controlled schema changes
- Repeatable migrations
- Production-proven

Status:

Accepted

---

## Database Library

Decision:

Exposed

Reason:

- Kotlin-native
- Type-safe SQL
- Lightweight compared to Hibernate

Status:

Accepted

---

## Testing

Decision:

JUnit 5

Reason:

- Kotlin compatible
- Mature ecosystem
- Excellent IDE support

Status:

Accepted

---

## Development Environments

The project is developed and tested in Docker containers using OrbStack as the local container runtime.

- A host-installed JDK is not required.
- Docker-compatible commands such as `docker compose` run through OrbStack.
- The container image supplies the supported Java and Gradle environment.

All development validation must pass through the OrbStack container workflow before changes are considered complete.

Status:

Accepted

---

## Audio Strategy

Decision:

Use AwesomeTTS inside Anki for Version 1.

The application will not generate audio.

The application stores:

- Text
- Sentences
- Metadata

Audio generation happens after export.

Future support:

- Azure TTS
- OpenAI TTS
- ElevenLabs
- Local TTS engines

Status:

Accepted

---

# 4. Architecture

The application follows a layered architecture.

```
CLI Layer

    |
    v

Application Layer

    |
    v

Domain Layer

    |
    v

Infrastructure Layer

    |
    v

SQLite Database
```

---

## CLI Layer

Responsibilities:

- Parse commands
- Validate arguments
- Call application services
- Display results

Example:

```
tagalog validate
tagalog migrate
tagalog version
```

---

## Application Layer

Responsibilities:

- Execute workflows
- Coordinate domain operations
- Handle use cases

Examples:

- Import vocabulary
- Validate database
- Generate exports

---

## Domain Layer

Responsibilities:

- Business concepts
- Rules
- Relationships

Entities:

- Vocabulary
- Sentence
- GrammarConcept
- Lesson
- Source
- Tag

---

## Infrastructure Layer

Responsibilities:

- Database access
- File access
- External integrations

Examples:

- SQLite
- Flyway
- Exposed

---

# 5. Repository Structure

Create:

```
tagalog-learning/

├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── Dockerfile
├── docker-compose.yml

├── docs/

│   ├── architecture.md
│   ├── domain-model.md
│   ├── epics.md
│   ├── user-stories.md
│   ├── conventions.md
│   └── implementation-playbook.md

├── src/

│   ├── main/

│   │   ├── kotlin/

│   │   └── resources/

│   │       └── db/migration/

│   └── test/

└── .github/

    └── workflows/
```

---

# Milestone 1 — Project Foundation

Goal:

Create a runnable Kotlin CLI application.

---

## Tasks

## 1.1 Create Gradle Project

Checklist:

- [x] Create Gradle Kotlin DSL project
- [x] Configure Kotlin JVM plugin
- [x] Configure Java version
- [x] Add application plugin
- [x] Document the container-managed JDK version

Validation:

- [x] The OrbStack build container uses the supported JDK
- [x] The Gradle build succeeds in OrbStack

---

## 1.2 Configure Dependencies

Add:

- [x] Picocli
- [x] SQLite JDBC
- [x] Flyway
- [x] Exposed
- [x] JUnit 5
- [x] Logging framework

Validation:

- [ ] Dependencies resolve
- [ ] Application compiles

---

## 1.3 Create CLI

Implement:

```
tagalog
```

Commands:

```
init
version
validate
migrate
```

Validation:

- [x] CLI starts
- [x] Help output works
- [x] Commands execute

---

## 1.4 Add Docker Support

Development runtime:

- [x] Use OrbStack to run Docker containers and Docker Compose

Create:

- [x] Dockerfile
- [x] docker-compose.yml

Validation:

```
docker compose up
```

passes.

---

## Milestone 1 Definition of Done

Complete when:

- [x] Project builds
- [x] Tests pass in Docker through OrbStack
- [x] CLI launches
- [x] Docker works through OrbStack
- [ ] CI passes

---

# Milestone 2 — Domain Model

Goal:

Represent language concepts.

---

# Entities

## Vocabulary

Represents:

A Tagalog word or phrase.

Attributes:

- ID
- Tagalog text
- English meaning
- Root word
- Part of speech
- Difficulty
- Frequency rank

---

## Sentence

Represents:

A natural Tagalog sentence.

Attributes:

- ID
- Text
- Translation
- Difficulty

---

## GrammarConcept

Represents:

A grammar rule or pattern.

Attributes:

- ID
- Name
- Description
- Formula

---

## Lesson

Represents:

Where knowledge was introduced.

---

## Source

Represents:

Origin of knowledge.

Examples:

- Teacher
- Pimsleur
- Song
- Video

---

## Tag

Represents:

Flexible categorization.

Examples:

- food
- family
- travel

---

# Tasks

- [ ] Create domain package structure
- [ ] Create entities
- [ ] Add entity validation
- [ ] Add unit tests

Validation:

- [ ] Invalid objects rejected
- [ ] Domain tests pass

---

# Milestone 3 — Database

Goal:

Persist domain data.

---

## Tasks

- [ ] Create Flyway migration
- [ ] Create tables
- [ ] Create relationships
- [ ] Add constraints
- [ ] Configure Exposed mappings

Tables:

```
vocabulary

sentence

grammar_concept

lesson

source

tag

vocabulary_tag

sentence_vocabulary

sentence_grammar
```

Validation:

- [ ] Migration succeeds
- [ ] Tables exist
- [ ] Constraints work

---

# Milestone 4 — Application Services

Goal:

Create usable workflows.

---

Tasks:

- [ ] Add vocabulary creation workflow
- [ ] Add sentence workflow
- [ ] Add grammar workflow
- [ ] Add validation workflow

Validation:

- [ ] CLI can execute workflows
- [ ] Errors handled correctly

---

# Milestone 5 — Export Foundation

Goal:

Prepare data for external systems.

---

Tasks:

- [ ] Define exporter interface
- [ ] Create export model
- [ ] Create placeholder Anki exporter

Validation:

- [ ] Export interface works
- [ ] Data transformation tested

---

# Deferred Features

These features are intentionally postponed.

They should not be implemented until the foundation is stable.

---

# Future Epic: AI Enrichment

Status:

Deferred

Purpose:

Generate:

- Example sentences
- Explanations
- Related vocabulary
- Grammar suggestions

Prerequisites:

- Stable domain model
- Review workflow

---

# Future Epic: TTS Providers

Status:

Deferred

Purpose:

Move audio generation into the application.

Possible providers:

- Azure
- OpenAI
- ElevenLabs

Current solution:

AwesomeTTS in Anki.

---

# Future Epic: Repository Implementations

Status:

Deferred

Purpose:

Create full persistence abstraction.

Prerequisite:

Database schema finalized.

---

# Future Epic: CSV Import/Export

Status:

Deferred

Purpose:

Bulk content movement.

---

# Future Epic: Anki Package Generation

Status:

Deferred

Purpose:

Generate complete Anki packages automatically.

---

# Future Epic: Review Workflow

Status:

Deferred

Purpose:

Support:

```
Generated

↓

Needs Review

↓

Approved

↓

Exported
```

---

# Future Epic: Media Management

Status:

Deferred

Purpose:

Manage:

- Audio
- Images
- Files

---

# Future Epic: Domain Services

Status:

Deferred

Purpose:

Introduce richer business workflows.

---

# Testing Strategy

Every feature requires:

## Unit Tests

Validate:

- Domain behavior
- Business rules

---

## Integration Tests

Validate:

- Database
- Migration
- CLI workflows

---

## Build Validation

With OrbStack running as the Docker-compatible container runtime, build the application image:

```
docker compose build
```

The image build runs the complete Gradle build and test suite. Smoke-test the resulting CLI image with:

```
docker compose run --rm app --help
docker compose run --rm app version
```

No host Java installation is required. OrbStack validation is the authoritative development test path.

---

# Development Conventions

## Code

Requirements:

- Prefer immutable data classes
- Keep domain logic outside infrastructure
- Avoid unnecessary frameworks
- Add tests with features

---

## Database

Requirements:

- Every schema change uses Flyway
- Never modify existing migrations
- Add new migrations

---

## Git

Commit examples:

```
feat: add vocabulary entity

test: add migration tests

docs: update architecture
```

---

# Definition of Complete Version 0.1

The project is considered complete when:

- [ ] Kotlin application runs
- [ ] Docker runs
- [ ] SQLite initializes
- [ ] Flyway migrations execute
- [ ] CLI works
- [ ] Tests pass in Docker
- [ ] Documentation exists
- [ ] Domain model is implemented
- [ ] Future roadmap is documented

---

# End State

Version 0.1 provides a stable foundation.

Future versions add intelligence and automation without requiring a redesign.
