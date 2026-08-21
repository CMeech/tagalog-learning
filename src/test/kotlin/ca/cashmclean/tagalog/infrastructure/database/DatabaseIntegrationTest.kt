package ca.cashmclean.tagalog.infrastructure.database

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Path
import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID

class DatabaseIntegrationTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    private val requiredTables = setOf(
        "vocabulary",
        "sentence",
        "grammar_concept",
        "lesson",
        "source",
        "tag",
        "vocabulary_tag",
        "sentence_vocabulary",
        "sentence_grammar",
        "lesson_source",
        "lesson_vocabulary",
        "lesson_sentence",
        "lesson_grammar",
        "import_run",
    )

    @Test
    fun `Migration creates every required table and is idempotent`() {
        val manager = manager()

        assertEquals(2, manager.migrate())
        assertEquals(0, manager.migrate())
        manager.validate()

        val tables = queryStrings(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'flyway_%'",
        )
        assertEquals(requiredTables, tables)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   "])
    fun `Database rejects blank vocabulary text`(invalidText: String) {
        manager().migrate()

        assertThrows(SQLException::class.java) {
            execute(
                """INSERT INTO vocabulary
                   (id, tagalog, english_meaning, part_of_speech, difficulty)
                   VALUES (?, ?, 'hello', 'NOUN', 'BEGINNER')""".trimIndent(),
                UUID.randomUUID().toString(),
                invalidText,
            )
        }
    }

    @Test
    fun `Database rejects invalid enum and frequency values`() {
        manager().migrate()

        assertThrows(SQLException::class.java) {
            insertVocabulary(partOfSpeech = "INVALID", frequencyRank = 1)
        }
        assertThrows(SQLException::class.java) {
            insertVocabulary(partOfSpeech = "NOUN", frequencyRank = 0)
        }
    }

    @Test
    fun `Foreign keys and relationship uniqueness are enforced`() {
        manager().migrate()
        val vocabularyId = insertVocabulary(partOfSpeech = "NOUN", frequencyRank = 1)
        val tagId = UUID.randomUUID().toString()
        execute("INSERT INTO tag (id, name) VALUES (?, 'food')", tagId)
        execute(
            "INSERT INTO vocabulary_tag (vocabulary_id, tag_id) VALUES (?, ?)",
            vocabularyId,
            tagId,
        )

        assertThrows(SQLException::class.java) {
            execute(
                "INSERT INTO vocabulary_tag (vocabulary_id, tag_id) VALUES (?, ?)",
                vocabularyId,
                tagId,
            )
        }
        assertThrows(SQLException::class.java) {
            execute(
                "INSERT INTO vocabulary_tag (vocabulary_id, tag_id) VALUES (?, ?)",
                UUID.randomUUID().toString(),
                tagId,
            )
        }
    }

    @Test
    fun `Deleting content cascades to relationship rows`() {
        manager().migrate()
        val vocabularyId = insertVocabulary(partOfSpeech = "NOUN", frequencyRank = null)
        val tagId = UUID.randomUUID().toString()
        execute("INSERT INTO tag (id, name) VALUES (?, 'food')", tagId)
        execute(
            "INSERT INTO vocabulary_tag (vocabulary_id, tag_id) VALUES (?, ?)",
            vocabularyId,
            tagId,
        )

        execute("DELETE FROM vocabulary WHERE id = ?", vocabularyId)

        assertEquals(0, queryInt("SELECT count(*) FROM vocabulary_tag"))
    }

    @Test
    fun `V2 backfills legacy lesson membership and source provenance`() {
        val lessonId = UUID.randomUUID().toString()
        val sourceId = UUID.randomUUID().toString()
        val vocabularyId = UUID.randomUUID().toString()

        // Recreate a V1 database explicitly, then apply V2 to exercise its backfill statements.
        val legacyConfig = DatabaseConfig(temporaryDirectory.resolve("legacy.db"))
        val v1Sql = requireNotNull(javaClass.getResourceAsStream("/db/migration/V1__create_learning_schema.sql"))
            .bufferedReader().use { it.readText() }
        DriverManager.getConnection(legacyConfig.jdbcUrl).use { connection ->
            connection.createStatement().use { statement ->
                v1Sql.split(';').map(String::trim).filter(String::isNotEmpty).forEach(statement::execute)
            }
            connection.prepareStatement("INSERT INTO lesson (id, name) VALUES (?, 'Legacy lesson')").use {
                it.setString(1, lessonId); it.executeUpdate()
            }
            connection.prepareStatement("INSERT INTO source (id, name, type) VALUES (?, 'Legacy source', 'BOOK')").use {
                it.setString(1, sourceId); it.executeUpdate()
            }
            connection.prepareStatement(
                """INSERT INTO vocabulary
                   (id, tagalog, english_meaning, part_of_speech, difficulty, lesson_id, source_id)
                   VALUES (?, 'ako', 'I', 'PRONOUN', 'BEGINNER', ?, ?)""".trimIndent(),
            ).use {
                it.setString(1, vocabularyId); it.setString(2, lessonId); it.setString(3, sourceId); it.executeUpdate()
            }
        }
        // Flyway cannot adopt an unversioned non-empty schema without a baseline, so run V2 directly.
        val v2Sql = requireNotNull(javaClass.getResourceAsStream("/db/migration/V2__add_lesson_associations_and_import_history.sql"))
            .bufferedReader().use { it.readText() }
        DriverManager.getConnection(legacyConfig.jdbcUrl).use { connection ->
            connection.createStatement().use { statement ->
                v2Sql.split(';').map(String::trim).filter(String::isNotEmpty).forEach(statement::execute)
            }
            connection.prepareStatement(
                "SELECT source_id FROM lesson_vocabulary WHERE lesson_id = ? AND vocabulary_id = ?",
            ).use { statement ->
                statement.setString(1, lessonId)
                statement.setString(2, vocabularyId)
                statement.executeQuery().use { result ->
                    assertEquals(true, result.next())
                    assertEquals(sourceId, result.getString(1))
                }
            }
        }
    }

    @Test
    fun `Exposed mappings cover every required table`() {
        val mappedTables = setOf(
            VocabularyTable,
            Sentences,
            GrammarConcepts,
            Lessons,
            Sources,
            Tags,
            VocabularyTags,
            SentenceVocabulary,
            SentenceGrammar,
            LessonSources,
            LessonVocabulary,
            LessonSentences,
            LessonGrammar,
            ImportRuns,
        ).mapTo(mutableSetOf()) { it.tableName }

        assertEquals(requiredTables, mappedTables)
    }

    private fun manager() = DatabaseManager(config())

    private fun config() = DatabaseConfig(temporaryDirectory.resolve("tagalog.db"))

    private fun insertVocabulary(partOfSpeech: String, frequencyRank: Int?): String {
        val id = UUID.randomUUID().toString()
        execute(
            """INSERT INTO vocabulary
               (id, tagalog, english_meaning, part_of_speech, difficulty, frequency_rank)
               VALUES (?, 'kumain', 'to eat', ?, 'BEGINNER', ?)""".trimIndent(),
            id,
            partOfSpeech,
            frequencyRank,
        )
        return id
    }

    private fun execute(sql: String, vararg parameters: Any?) {
        DriverManager.getConnection(config().jdbcUrl).use { connection ->
            connection.prepareStatement(sql).use { statement ->
                parameters.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
                statement.executeUpdate()
            }
        }
    }

    private fun queryStrings(sql: String): Set<String> =
        DriverManager.getConnection(config().jdbcUrl).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { results ->
                    buildSet { while (results.next()) add(results.getString(1)) }
                }
            }
        }

    private fun queryInt(sql: String): Int =
        DriverManager.getConnection(config().jdbcUrl).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { results ->
                    results.next()
                    results.getInt(1)
                }
            }
        }
}
