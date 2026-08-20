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

    private fun databaseUrl() = "jdbc:sqlite:${temporaryDirectory.resolve("tagalog.db")}?foreign_keys=on"

    private data class CommandOutput(val exitCode: Int, val text: String, val error: String)
}
