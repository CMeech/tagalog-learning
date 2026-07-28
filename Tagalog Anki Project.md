# Tagalog Anki Project

## Goals

The primary goal of this project is to build a long-term, automated language learning system for Tagalog.

Objectives:

- Strengthen everyday vocabulary.
- Improve listening comprehension.
- Learn grammar systematically.
- Retain vocabulary through spaced repetition.
- Generate cards automatically instead of manually.
- Build a reusable language-learning pipeline.

---

# Learning Philosophy

Anki is **not** where new concepts are learned.

Instead:

Lessons
↓
Practice
↓
Immersion (music, YouTube, conversations)
↓
Anki reviews
↓
Long-term retention

New concepts should first be encountered through lessons or real language use. Anki reinforces those concepts until they become automatic.

---

# One Deck

Use one deck:

🇵🇭 Tagalog

Organization should come from note types and tags rather than multiple decks.

---

# Note Types

## Vocabulary

Fields

- Tagalog
- English
- Part of Speech
- Root Word
- Example Sentence
- Example Translation
- Word Audio
- Sentence Audio
- Image
- Frequency Rank
- Lesson
- Source
- Notes
- Tags

Cards Generated

1. Tagalog → English
2. English → Tagalog
3. Audio Recognition
4. Sentence Recognition
5. Cloze Sentence (optional)

---

## Sentence

Fields

- Tagalog Sentence
- English Translation
- Audio
- Vocabulary Used
- Grammar Concepts
- Source
- Notes

Cards

- Audio → Sentence
- Sentence → Meaning
- Cloze Cards

---

## Grammar

Grammar is intentionally included as a first-class note type.

Fields

- Pattern Name
- Explanation
- Formula
- Examples
- Audio
- Related Grammar
- Common Mistakes
- Lesson
- Tags

Example

Pattern:

mag + verb

Explanation:

Creates actor-focus verbs.

Example:

Magluluto ako bukas.

Translation:

I will cook tomorrow.

Cards

Pattern → Explanation

Explanation → Pattern

Fill-in-the-pattern

Recognize correct usage

Audio recognition

---

## Listening

Fields

- Audio
- Transcript
- Translation
- Difficulty
- Source

Cards

Audio → Transcript

Audio → Translation

Transcript → Audio Recall

---

# Tags

Examples

lesson1

lesson2

lesson3

noun

verb

adjective

grammar

travel

family

shopping

food

frequency500

frequency1000

song

youtube

conversation

---

# Images

Images should primarily be used for:

- nouns
- food
- objects
- places
- animals

Images are generally unnecessary for abstract concepts or grammar.

---

# Audio

Every vocabulary item should ideally contain:

Word audio

Sentence audio

Native-quality pronunciation is strongly preferred.

---

# Sources

Track where every item originated.

Possible values

- Personal lesson
- Pimsleur
- YouTube
- Movie
- Song
- Conversation
- AI Generated

---

# Difficulty

Optional

1

2

3

or

Easy

Medium

Hard

---

# Frequency

Include a frequency rank whenever possible.

This allows future filtering for:

Top 500

Top 1000

Top 2000

etc.

---

# Long-Term Workflow

Lessons

↓

Vocabulary

↓

Grammar

↓

Sentences

↓

Immersion

↓

Review

---

# Automated Pipeline

The long-term goal is to completely automate Anki generation.

Pipeline

Lessons
↓

SQLite Database

↓

Validation

↓

AI enrichment

↓

Example sentence generation

↓

Grammar linking

↓

Audio generation

↓

Media generation

↓

CSV Export

↓

Anki Import

---

# Future Expansion

Potential future additions

- Morphology analysis
- Frequency statistics
- Related vocabulary
- Synonyms
- Antonyms
- Minimal pairs
- Pronunciation notes
- IPA
- Pitch/accent notes (if useful)
- Multiple example sentences
- Cloze generation
- Grammar dependency graph
