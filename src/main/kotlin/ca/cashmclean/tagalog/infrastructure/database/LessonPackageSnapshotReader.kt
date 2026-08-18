package ca.cashmclean.tagalog.infrastructure.database

import ca.cashmclean.tagalog.application.lessonpackage.StoredGrammar
import ca.cashmclean.tagalog.application.lessonpackage.StoredLesson
import ca.cashmclean.tagalog.application.lessonpackage.StoredLessonPackageSnapshot
import ca.cashmclean.tagalog.application.lessonpackage.StoredSentence
import ca.cashmclean.tagalog.application.lessonpackage.StoredSource
import ca.cashmclean.tagalog.application.lessonpackage.StoredVocabulary
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.UUID

class LessonPackageSnapshotReader(private val config: DatabaseConfig) {
    fun read(): StoredLessonPackageSnapshot = DriverManager.getConnection(config.readOnlyJdbcUrl).use { connection ->
        connection.createStatement().use { it.execute("PRAGMA query_only = ON") }
        StoredLessonPackageSnapshot(
            lessons = connection.query("SELECT id, name, description FROM lesson") { results ->
                StoredLesson(uuid(results, "id"), results.getString("name"), results.getString("description"))
            },
            sources = connection.query("SELECT id, name, type, reference FROM source") { results ->
                StoredSource(uuid(results, "id"), results.getString("name"), results.getString("type"), results.getString("reference"))
            },
            vocabulary = readVocabulary(connection),
            sentences = readSentences(connection),
            grammar = connection.query("SELECT id, name, description, formula, lesson_id, source_id FROM grammar_concept") { results ->
                StoredGrammar(
                    uuid(results, "id"), results.getString("name"), results.getString("description"), results.getString("formula"),
                    optionalUuid(results, "lesson_id"), optionalUuid(results, "source_id"),
                )
            },
        )
    }

    private fun readVocabulary(connection: Connection): List<StoredVocabulary> {
        val tags = connection.query("SELECT vt.vocabulary_id, t.name FROM vocabulary_tag vt JOIN tag t ON t.id = vt.tag_id") {
            UUID.fromString(it.getString(1)) to it.getString(2)
        }.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }
        return connection.query(
            "SELECT id, tagalog, english_meaning, root_word, part_of_speech, difficulty, frequency_rank, lesson_id, source_id FROM vocabulary",
        ) { results ->
            val id = uuid(results, "id")
            StoredVocabulary(
                id, results.getString("tagalog"), results.getString("english_meaning"), results.getString("root_word"),
                results.getString("part_of_speech"), results.getString("difficulty"), results.getInt("frequency_rank").let { if (results.wasNull()) null else it },
                optionalUuid(results, "lesson_id"), optionalUuid(results, "source_id"), tags[id].orEmpty(),
            )
        }
    }

    private fun readSentences(connection: Connection): List<StoredSentence> {
        val vocabulary = connection.query("SELECT sentence_id, vocabulary_id FROM sentence_vocabulary") {
            UUID.fromString(it.getString(1)) to UUID.fromString(it.getString(2))
        }.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }
        val grammar = connection.query("SELECT sentence_id, grammar_concept_id FROM sentence_grammar") {
            UUID.fromString(it.getString(1)) to UUID.fromString(it.getString(2))
        }.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }
        return connection.query("SELECT id, text, translation, difficulty, lesson_id, source_id FROM sentence") { results ->
            val id = uuid(results, "id")
            StoredSentence(
                id, results.getString("text"), results.getString("translation"), results.getString("difficulty"),
                optionalUuid(results, "lesson_id"), optionalUuid(results, "source_id"), vocabulary[id].orEmpty(), grammar[id].orEmpty(),
            )
        }
    }

    private fun <T> Connection.query(sql: String, convert: (ResultSet) -> T): List<T> =
        createStatement().use { statement ->
            statement.executeQuery(sql).use { results -> buildList { while (results.next()) add(convert(results)) } }
        }

    private fun uuid(results: ResultSet, column: String): UUID = UUID.fromString(results.getString(column))
    private fun optionalUuid(results: ResultSet, column: String): UUID? = results.getString(column)?.let(UUID::fromString)
}
