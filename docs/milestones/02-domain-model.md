# Milestone 2 — Domain Model

Goal: represent language concepts independently of storage and Anki.

## Entities

- **Vocabulary:** a Tagalog word or phrase, with ID, Tagalog text, English meaning, root word, part of speech, difficulty, and frequency rank.
- **Sentence:** a natural Tagalog sentence, with ID, text, translation, and difficulty.
- **GrammarConcept:** a grammar rule or pattern, with ID, name, description, and formula.
- **Lesson:** where knowledge was introduced.
- **Source:** the origin of knowledge, such as a teacher, Pimsleur, song, or video.
- **Tag:** flexible categorization, such as food, family, or travel.

## Tasks

- [ ] Create the domain package structure.
- [ ] Create entities.
- [ ] Add entity validation.
- [ ] Add unit tests.

## Validation

- [ ] Invalid objects are rejected.
- [ ] Domain tests pass.
