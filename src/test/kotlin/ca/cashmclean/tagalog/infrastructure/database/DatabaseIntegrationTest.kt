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
    )

    @Test
    fun `migration creates every required table and is idempotent`() {
        val manager = manager()

        assertEquals(1, manager.migrate())
        assertEquals(0, manager.migrate())
        manager.validate()

        val tables = queryStrings(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'flyway_%'",
        )
        assertEquals(requiredTables, tables)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   "])
    fun `database rejects blank vocabulary text`(invalidText: String) {
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
    fun `database rejects invalid enum and frequency values`() {
        manager().migrate()

        assertThrows(SQLException::class.java) {
            insertVocabulary(partOfSpeech = "INVALID", frequencyRank = 1)
        }
        assertThrows(SQLException::class.java) {
            insertVocabulary(partOfSpeech = "NOUN", frequencyRank = 0)
        }
    }

    @Test
    fun `foreign keys and relationship uniqueness are enforced`() {
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
    fun `deleting content cascades to relationship rows`() {
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
