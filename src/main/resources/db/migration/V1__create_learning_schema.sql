CREATE TABLE lesson (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    description TEXT CHECK (description IS NULL OR length(trim(description)) > 0)
);

CREATE TABLE source (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    type TEXT NOT NULL CHECK (type IN ('TEACHER', 'COURSE', 'BOOK', 'SONG', 'VIDEO', 'WEBSITE', 'OTHER')),
    reference TEXT CHECK (reference IS NULL OR length(trim(reference)) > 0)
);

CREATE TABLE tag (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE COLLATE NOCASE CHECK (length(trim(name)) > 0)
);

CREATE TABLE vocabulary (
    id TEXT PRIMARY KEY,
    tagalog TEXT NOT NULL CHECK (length(trim(tagalog)) > 0),
    english_meaning TEXT NOT NULL CHECK (length(trim(english_meaning)) > 0),
    root_word TEXT CHECK (root_word IS NULL OR length(trim(root_word)) > 0),
    part_of_speech TEXT NOT NULL CHECK (part_of_speech IN ('NOUN', 'PRONOUN', 'VERB', 'ADJECTIVE', 'ADVERB', 'PREPOSITION', 'CONJUNCTION', 'PARTICLE', 'INTERJECTION', 'PHRASE', 'OTHER')),
    difficulty TEXT NOT NULL CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    frequency_rank INTEGER CHECK (frequency_rank IS NULL OR frequency_rank > 0),
    lesson_id TEXT REFERENCES lesson(id) ON DELETE SET NULL,
    source_id TEXT REFERENCES source(id) ON DELETE SET NULL
);

CREATE TABLE sentence (
    id TEXT PRIMARY KEY,
    text TEXT NOT NULL CHECK (length(trim(text)) > 0),
    translation TEXT NOT NULL CHECK (length(trim(translation)) > 0),
    difficulty TEXT NOT NULL CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    lesson_id TEXT REFERENCES lesson(id) ON DELETE SET NULL,
    source_id TEXT REFERENCES source(id) ON DELETE SET NULL
);

CREATE TABLE grammar_concept (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    description TEXT NOT NULL CHECK (length(trim(description)) > 0),
    formula TEXT NOT NULL CHECK (length(trim(formula)) > 0),
    lesson_id TEXT REFERENCES lesson(id) ON DELETE SET NULL,
    source_id TEXT REFERENCES source(id) ON DELETE SET NULL
);

CREATE TABLE vocabulary_tag (
    vocabulary_id TEXT NOT NULL REFERENCES vocabulary(id) ON DELETE CASCADE,
    tag_id TEXT NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
    PRIMARY KEY (vocabulary_id, tag_id)
);

CREATE TABLE sentence_vocabulary (
    sentence_id TEXT NOT NULL REFERENCES sentence(id) ON DELETE CASCADE,
    vocabulary_id TEXT NOT NULL REFERENCES vocabulary(id) ON DELETE CASCADE,
    PRIMARY KEY (sentence_id, vocabulary_id)
);

CREATE TABLE sentence_grammar (
    sentence_id TEXT NOT NULL REFERENCES sentence(id) ON DELETE CASCADE,
    grammar_concept_id TEXT NOT NULL REFERENCES grammar_concept(id) ON DELETE CASCADE,
    PRIMARY KEY (sentence_id, grammar_concept_id)
);
