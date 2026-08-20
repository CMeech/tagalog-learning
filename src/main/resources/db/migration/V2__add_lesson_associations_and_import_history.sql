CREATE TABLE lesson_source (
    lesson_id TEXT NOT NULL REFERENCES lesson(id) ON DELETE CASCADE,
    source_id TEXT NOT NULL REFERENCES source(id) ON DELETE RESTRICT,
    PRIMARY KEY (lesson_id, source_id)
);

CREATE TABLE lesson_vocabulary (
    lesson_id TEXT NOT NULL REFERENCES lesson(id) ON DELETE CASCADE,
    vocabulary_id TEXT NOT NULL REFERENCES vocabulary(id) ON DELETE CASCADE,
    source_id TEXT REFERENCES source(id) ON DELETE RESTRICT,
    PRIMARY KEY (lesson_id, vocabulary_id)
);

CREATE TABLE lesson_sentence (
    lesson_id TEXT NOT NULL REFERENCES lesson(id) ON DELETE CASCADE,
    sentence_id TEXT NOT NULL REFERENCES sentence(id) ON DELETE CASCADE,
    source_id TEXT REFERENCES source(id) ON DELETE RESTRICT,
    PRIMARY KEY (lesson_id, sentence_id)
);

CREATE TABLE lesson_grammar (
    lesson_id TEXT NOT NULL REFERENCES lesson(id) ON DELETE CASCADE,
    grammar_concept_id TEXT NOT NULL REFERENCES grammar_concept(id) ON DELETE CASCADE,
    source_id TEXT REFERENCES source(id) ON DELETE RESTRICT,
    PRIMARY KEY (lesson_id, grammar_concept_id)
);

INSERT OR IGNORE INTO lesson_source (lesson_id, source_id)
SELECT lesson_id, source_id FROM vocabulary WHERE lesson_id IS NOT NULL AND source_id IS NOT NULL;
INSERT OR IGNORE INTO lesson_source (lesson_id, source_id)
SELECT lesson_id, source_id FROM sentence WHERE lesson_id IS NOT NULL AND source_id IS NOT NULL;
INSERT OR IGNORE INTO lesson_source (lesson_id, source_id)
SELECT lesson_id, source_id FROM grammar_concept WHERE lesson_id IS NOT NULL AND source_id IS NOT NULL;

INSERT INTO lesson_vocabulary (lesson_id, vocabulary_id, source_id)
SELECT lesson_id, id, source_id FROM vocabulary WHERE lesson_id IS NOT NULL;
INSERT INTO lesson_sentence (lesson_id, sentence_id, source_id)
SELECT lesson_id, id, source_id FROM sentence WHERE lesson_id IS NOT NULL;
INSERT INTO lesson_grammar (lesson_id, grammar_concept_id, source_id)
SELECT lesson_id, id, source_id FROM grammar_concept WHERE lesson_id IS NOT NULL;

CREATE TABLE import_run (
    id TEXT PRIMARY KEY,
    lesson_id TEXT NOT NULL REFERENCES lesson(id) ON DELETE RESTRICT,
    package_checksum TEXT NOT NULL UNIQUE CHECK (length(package_checksum) = 64),
    schema_version INTEGER NOT NULL,
    imported_at TEXT NOT NULL,
    inserted_count INTEGER NOT NULL CHECK (inserted_count >= 0),
    updated_count INTEGER NOT NULL CHECK (updated_count >= 0),
    unchanged_count INTEGER NOT NULL CHECK (unchanged_count >= 0),
    newly_related_count INTEGER NOT NULL CHECK (newly_related_count >= 0)
);

CREATE INDEX lesson_vocabulary_vocabulary_idx ON lesson_vocabulary(vocabulary_id);
CREATE INDEX lesson_sentence_sentence_idx ON lesson_sentence(sentence_id);
CREATE INDEX lesson_grammar_grammar_idx ON lesson_grammar(grammar_concept_id);
CREATE INDEX lesson_source_source_idx ON lesson_source(source_id);
CREATE INDEX import_run_lesson_idx ON import_run(lesson_id, imported_at);
