# Milestone 3 — Database

Goal: persist domain data.

## Tasks

- [x] Create a Flyway migration.
- [x] Create tables.
- [x] Create relationships.
- [x] Add constraints.
- [x] Configure Exposed mappings.

Required tables:

```text
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

## Validation

- [x] Migration succeeds.
- [x] Tables exist.
- [x] Constraints work.

## Docker Compose test plan

- [x] `docker compose up` starts cleanly and initializes the database.
- [x] A second `docker compose up` starts without migration or startup errors.
- [x] The named database volume persists across both runs.
- [x] `docker compose run --rm app validate` succeeds against the persisted database.
