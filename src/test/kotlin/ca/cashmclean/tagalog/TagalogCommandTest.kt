package ca.cashmclean.tagalog

import ca.cashmclean.tagalog.cli.TagalogCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.ResourceLock
import picocli.CommandLine
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Files
import java.sql.DriverManager

@ResourceLock("SYSTEM_OUT")
class TagalogCommandTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `root command displays help`() {
        val output = captureOutput { CommandLine(TagalogCommand()).execute() }

        assertEquals(0, output.exitCode)
        assertTrue(output.text.contains("Usage: tagalog"))
        assertTrue(output.text.contains("validate"))
    }

    @Test
    fun `version command prints application version`() {
        val output = captureOutput { CommandLine(TagalogCommand()).execute("version") }

        assertEquals(0, output.exitCode)
        assertEquals(APPLICATION_VERSION, output.text.trim())
    }

    @Test
    fun `all milestone one commands execute successfully`() {
        withTemporaryDatabase {
            listOf("init", "validate", "migrate").forEach { command ->
                val output = captureOutput { CommandLine(TagalogCommand()).execute(command) }
                assertEquals(0, output.exitCode, "Expected '$command' to succeed")
            }
        }
    }

    @Test
    fun `creation workflows persist vocabulary sentences and grammar`() {
        withTemporaryDatabase {
            assertEquals(0, execute("init").exitCode)
            assertEquals(
                0,
                execute(
                    "vocabulary", "add", "--tagalog", "kumain", "--english", "to eat",
                    "--root", "kain", "--part-of-speech", "VERB", "--frequency-rank", "42",
                ).exitCode,
            )
            assertEquals(
                0,
                execute(
                    "sentence", "add", "--text", "Kumain ako.", "--translation", "I ate.",
                    "--difficulty", "BEGINNER",
                ).exitCode,
            )
            assertEquals(
                0,
                execute(
                    "grammar", "add", "--name", "Actor focus", "--description", "Focuses on the actor",
                    "--formula", "um + root",
                ).exitCode,
            )

            assertEquals(1, rowCount("vocabulary"))
            assertEquals(1, rowCount("sentence"))
            assertEquals(1, rowCount("grammar_concept"))
            assertEquals(0, execute("validate").exitCode)
        }
    }

    @Test
    fun `invalid workflow input returns an error without persisting data`() {
        withTemporaryDatabase {
            execute("init")

            val output = execute(
                "vocabulary", "add", "--tagalog", "salamat", "--english", "thank you",
                "--part-of-speech", "PHRASE", "--frequency-rank", "0",
            )

            assertEquals(1, output.exitCode)
            assertEquals(0, rowCount("vocabulary"))
        }
    }

    @Test
    fun `validation reports malformed stored entities`() {
        withTemporaryDatabase {
            execute("init")
            DriverManager.getConnection(databaseUrl()).use { connection ->
                connection.prepareStatement(
                    "INSERT INTO sentence (id, text, translation, difficulty) VALUES ('not-a-uuid', 'Oo.', 'Yes.', 'BEGINNER')",
                ).use { it.executeUpdate() }
            }

            val output = execute("validate")

            assertEquals(1, output.exitCode)
            assertTrue(output.error.contains("Invalid sentence 'not-a-uuid'"))
        }
    }

    @Test
    fun `lesson validate supports text and json without changing SQLite`() {
        withTemporaryDatabase {
            assertEquals(0, execute("init").exitCode)
            val packagePath = Path.of("examples/lesson-package").toAbsolutePath().toString()
            val before = contentRowCount()

            val text = execute("lesson", "validate", packagePath)
            val json = execute("lesson", "validate", packagePath, "--format", "json")

            assertEquals(0, text.exitCode)
            assertTrue(text.text.contains("Lesson package is valid"))
            assertTrue(text.text.contains("11 insert"))
            assertEquals(0, json.exitCode)
            assertTrue(json.text.contains("\"valid\":true"))
            assertTrue(json.text.contains("\"lesson_id\":\"10000000-0000-4000-8000-000000000001\""))
            assertEquals(before, contentRowCount(), "Read-only validation must not add or change content")
        }
    }

    @Test
    fun `lesson import reports counts and exact rerun in text and json`() {
        withTemporaryDatabase {
            assertEquals(0, execute("init").exitCode)
            val packagePath = Path.of("examples/lesson-package").toAbsolutePath().toString()

            val imported = execute("lesson", "import", packagePath)
            val rerun = execute("lesson", "import", packagePath, "--format", "json")

            assertEquals(0, imported.exitCode)
            assertTrue(imported.text.contains("11 inserted"))
            assertTrue(imported.text.contains("10 newly related"))
            assertEquals(0, rerun.exitCode)
            assertTrue(rerun.text.contains("\"success\":true"))
            assertTrue(rerun.text.contains("\"exact_rerun\":true"))
            assertEquals(1, rowCount("import_run"))
        }
    }

    @Test
    fun `lesson list is deterministic and supports empty text and stable json`() {
        withTemporaryDatabase {
            execute("init")
            assertEquals("No lessons found.", execute("lesson", "list").text.trim())
            assertEquals("{\"lessons\":[]}", execute("lesson", "list", "--format", "json").text.trim())

            DriverManager.getConnection(databaseUrl()).use { connection ->
                connection.prepareStatement("INSERT INTO lesson (id, name) VALUES (?, ?)").use { statement ->
                    listOf("b0000000-0000-4000-8000-000000000000" to "Second", "a0000000-0000-4000-8000-000000000000" to "First").forEach {
                        statement.setString(1, it.first)
                        statement.setString(2, it.second)
                        statement.executeUpdate()
                    }
                }
            }
            val listed = execute("lesson", "list")
            assertTrue(listed.text.indexOf("First") < listed.text.indexOf("Second"))
        }
    }

    @Test
    fun `lesson show includes provenance relationships counts and import history`() {
        withTemporaryDatabase {
            execute("init")
            val packagePath = Path.of("examples/lesson-package").toAbsolutePath().toString()
            execute("lesson", "import", packagePath)
            val lessonId = "10000000-0000-4000-8000-000000000001"

            val text = execute("lesson", "show", lessonId)
            val json = execute("lesson", "show", lessonId, "--format", "json")

            assertEquals(0, text.exitCode)
            assertTrue(text.text.contains("Counts: 1 sources, 4 vocabulary, 3 sentences, 2 grammar, 1 imports"))
            assertTrue(text.text.contains("Vocabulary:"))
            assertTrue(text.text.contains("source: Usapang Tagalog, Aralin 1"))
            assertTrue(text.text.contains("Grammar:"))
            assertTrue(text.text.contains("Import history:"))
            assertEquals(0, json.exitCode)
            assertTrue(json.text.contains("\"found\":true"))
            assertTrue(json.text.contains("\"import_history\":[{"))
            assertTrue(json.text.contains("\"vocabulary\":[{"))
            assertTrue(json.text.contains("\"source\":{\"id\":"))
        }
    }

    @Test
    fun `lesson show reports an unknown id in text and stable json`() {
        withTemporaryDatabase {
            execute("init")
            val id = "00000000-0000-4000-8000-000000000000"

            val text = execute("lesson", "show", id)
            val json = execute("lesson", "show", id, "--format", "json")

            assertEquals(2, text.exitCode)
            assertEquals("Lesson not found: $id", text.error.trim())
            assertEquals(2, json.exitCode)
            assertEquals("{\"found\":false,\"lesson_id\":\"$id\",\"message\":\"Lesson not found: $id\"}", json.error.trim())
        }
    }

    @Test
    fun `entity show reports complete content relationships and lesson provenance`() {
        withTemporaryDatabase {
            execute("init")
            execute("lesson", "import", Path.of("examples/lesson-package").toAbsolutePath().toString())

            val vocabulary = execute("vocabulary", "show", "30000000-0000-4000-8000-000000000004", "--format", "json")
            val sentence = execute("sentence", "show", "50000000-0000-4000-8000-000000000003", "--format", "json")
            val grammar = execute("grammar", "show", "40000000-0000-4000-8000-000000000002", "--format", "json")

            assertEquals(0, vocabulary.exitCode)
            assertTrue(vocabulary.text.contains("\"tagalog\":\"po\""))
            assertTrue(vocabulary.text.contains("\"reference\":\"Kabanata 1, pahina 3–5\""))
            assertTrue(vocabulary.text.contains("\"used_by_sentences\":[{"))
            assertEquals(0, sentence.exitCode)
            assertTrue(sentence.text.contains("\"vocabulary\":[{"))
            assertTrue(sentence.text.contains("\"grammar\":[{"))
            assertEquals(0, grammar.exitCode)
            assertTrue(grammar.text.contains("\"example_sentences\":[{"))

            val unknown = execute("grammar", "show", "00000000-0000-4000-8000-000000000000", "--format", "json")
            assertEquals(2, unknown.exitCode)
            assertEquals("{\"found\":false,\"type\":\"grammar\",\"id\":\"00000000-0000-4000-8000-000000000000\",\"message\":\"Grammar not found: 00000000-0000-4000-8000-000000000000\"}", unknown.error.trim())
        }
    }

    @Test
    fun `explicit deletion refuses references then removes sentence associations and prints Anki notice`() {
        withTemporaryDatabase {
            execute("init")
            execute("lesson", "import", Path.of("examples/lesson-package").toAbsolutePath().toString())
            val vocabularyId = "30000000-0000-4000-8000-000000000001"
            val sentenceId = "50000000-0000-4000-8000-000000000001"

            val refused = execute("vocabulary", "delete", vocabularyId)
            assertEquals(3, refused.exitCode)
            assertTrue(refused.error.contains(sentenceId))
            assertEquals(4, rowCount("vocabulary"))

            DriverManager.getConnection(databaseUrl()).use { connection ->
                connection.createStatement().use { it.executeUpdate(
                    "CREATE TRIGGER fail_sentence_delete AFTER DELETE ON sentence BEGIN SELECT RAISE(ABORT, 'forced rollback'); END",
                ) }
            }
            val rolledBack = execute("sentence", "delete", sentenceId)
            assertEquals(1, rolledBack.exitCode)
            assertEquals(1, countWhere("sentence", "id", sentenceId))
            assertTrue(countWhere("sentence_vocabulary", "sentence_id", sentenceId) > 0)
            DriverManager.getConnection(databaseUrl()).use { connection ->
                connection.createStatement().use { it.executeUpdate("DROP TRIGGER fail_sentence_delete") }
            }

            val deletedSentence = execute("sentence", "delete", sentenceId)
            assertEquals(0, deletedSentence.exitCode)
            assertTrue(deletedSentence.text.contains("TSV import cannot remove notes"))
            assertEquals(0, countWhere("lesson_sentence", "sentence_id", sentenceId))
            assertEquals(0, countWhere("sentence_vocabulary", "sentence_id", sentenceId))
            assertEquals(0, countWhere("sentence_grammar", "sentence_id", sentenceId))

            val deletedVocabulary = execute("vocabulary", "delete", vocabularyId, "--format", "json")
            assertEquals(0, deletedVocabulary.exitCode)
            assertTrue(deletedVocabulary.text.contains("\"anki_manual_removal_required\":true"))
            assertEquals(0, countWhere("lesson_vocabulary", "vocabulary_id", vocabularyId))
            assertEquals(0, countWhere("vocabulary", "id", vocabularyId))

            val grammarId = "40000000-0000-4000-8000-000000000002"
            val refusedGrammar = execute("grammar", "delete", grammarId)
            assertEquals(3, refusedGrammar.exitCode)
            assertTrue(refusedGrammar.error.contains("50000000-0000-4000-8000-000000000003"))
            execute("sentence", "delete", "50000000-0000-4000-8000-000000000003")
            val deletedGrammar = execute("grammar", "delete", grammarId)
            assertEquals(0, deletedGrammar.exitCode)
            assertEquals(0, countWhere("lesson_grammar", "grammar_concept_id", grammarId))

            val unknown = execute("sentence", "delete", "00000000-0000-4000-8000-000000000000", "--format", "json")
            assertEquals(2, unknown.exitCode)
            assertTrue(unknown.error.contains("\"deleted\":false"))
        }
    }

    @Test
    fun `Anki export matches fixtures is repeatable and protects destinations`() {
        withTemporaryDatabase {
            execute("init")
            execute("lesson", "import", Path.of("examples/lesson-package").toAbsolutePath().toString())
            val lessonId = "10000000-0000-4000-8000-000000000001"
            val first = temporaryDirectory.resolve("export-one")
            val second = temporaryDirectory.resolve("export-two")

            assertEquals(0, execute("anki", "export", "--lesson", lessonId, "--output", first.toString()).exitCode)
            listOf("vocabulary.tsv", "sentences.tsv", "grammar.tsv").forEach { name ->
                assertEquals(Files.readString(Path.of("examples/lesson-package/expected-anki/$name")), Files.readString(first.resolve(name)))
            }
            val manifest = Files.readString(first.resolve("export.json"))
            assertTrue(manifest.contains("\"schema_version\" : 1"))
            assertTrue(manifest.contains("\"lesson_id\" : \"$lessonId\""))
            assertTrue(manifest.contains("\"sha256\""))
            assertTrue(manifest.contains("\"row_count\" : 4"))

            assertEquals(0, execute("anki", "export", "--lesson", lessonId, "--output", second.toString()).exitCode)
            assertEquals(Files.readString(first.resolve("vocabulary.tsv")), Files.readString(second.resolve("vocabulary.tsv")))
            val exists = execute("anki", "export", "--lesson", lessonId, "--output", first.toString())
            assertEquals(2, exists.exitCode)
            val unknownOutput = temporaryDirectory.resolve("unknown")
            val unknown = execute("anki", "export", "--lesson", "00000000-0000-4000-8000-000000000000", "--output", unknownOutput.toString())
            assertEquals(2, unknown.exitCode)
            assertEquals(false, Files.exists(unknownOutput))
        }
    }

    @Test
    fun `Anki export omits empty TSVs uses requested lesson provenance and global grammar examples`() {
        withTemporaryDatabase {
            execute("init")
            execute("lesson", "import", Path.of("examples/lesson-package").toAbsolutePath().toString())
            val lessonId = "90000000-0000-4000-8000-000000000001"
            val sourceId = "90000000-0000-4000-8000-000000000002"
            DriverManager.getConnection(databaseUrl()).use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeUpdate("INSERT INTO lesson (id, name) VALUES ('$lessonId', 'Ikalawang aralin')")
                    statement.executeUpdate("INSERT INTO source (id, name, type, reference) VALUES ('$sourceId', 'Bagong aklat', 'BOOK', 'p. 9')")
                    statement.executeUpdate("INSERT INTO lesson_source VALUES ('$lessonId', '$sourceId')")
                    statement.executeUpdate("INSERT INTO lesson_grammar VALUES ('$lessonId', '40000000-0000-4000-8000-000000000002', '$sourceId')")
                }
            }
            val output = temporaryDirectory.resolve("second-lesson")
            assertEquals(0, execute("anki", "export", "--lesson", lessonId, "--output", output.toString()).exitCode)
            assertEquals(false, Files.exists(output.resolve("vocabulary.tsv")))
            assertEquals(false, Files.exists(output.resolve("sentences.tsv")))
            val grammar = Files.readString(output.resolve("grammar.tsv"))
            assertTrue(grammar.contains("Ikalawang aralin\tBagong aklat — p. 9"))
            assertTrue(grammar.contains("Magandang umaga po.; Ikaw po si José?"))
        }
    }

    private fun withTemporaryDatabase(action: () -> Unit) {
        val property = "tagalog.db.path"
        val previous = System.getProperty(property)
        try {
            System.setProperty(property, temporaryDirectory.resolve("tagalog.db").toString())
            action()
        } finally {
            if (previous == null) System.clearProperty(property) else System.setProperty(property, previous)
        }
    }

    private fun captureOutput(action: () -> Int): CommandOutput {
        val originalOut = System.out
        val originalErr = System.err
        val bytes = ByteArrayOutputStream()
        val errorBytes = ByteArrayOutputStream()
        return try {
            System.setOut(PrintStream(bytes))
            System.setErr(PrintStream(errorBytes))
            CommandOutput(action(), bytes.toString(Charsets.UTF_8), errorBytes.toString(Charsets.UTF_8))
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
    }

    private fun execute(vararg arguments: String) = captureOutput {
        CommandLine(TagalogCommand()).execute(*arguments)
    }

    private fun rowCount(table: String): Int =
        DriverManager.getConnection(databaseUrl()).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT count(*) FROM $table").use { results ->
                    results.next()
                    results.getInt(1)
                }
            }
        }

    private fun contentRowCount(): Int = listOf(
        "lesson", "source", "vocabulary", "sentence", "grammar_concept", "tag",
        "vocabulary_tag", "sentence_vocabulary", "sentence_grammar",
    ).sumOf(::rowCount)

    private fun countWhere(table: String, column: String, id: String): Int =
        DriverManager.getConnection(databaseUrl()).use { connection ->
            connection.prepareStatement("SELECT count(*) FROM $table WHERE $column = ?").use { statement ->
                statement.setString(1, id)
                statement.executeQuery().use { results -> results.next(); results.getInt(1) }
            }
        }

    private fun databaseUrl() = "jdbc:sqlite:${temporaryDirectory.resolve("tagalog.db")}?foreign_keys=on"

    private data class CommandOutput(val exitCode: Int, val text: String, val error: String)
}
