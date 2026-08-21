package ca.cashmclean.tagalog.infrastructure.database

import ca.cashmclean.tagalog.application.export.ExportAssociation
import ca.cashmclean.tagalog.application.export.ExportReference
import ca.cashmclean.tagalog.application.export.GrammarExportProjection
import ca.cashmclean.tagalog.application.export.LessonExport
import ca.cashmclean.tagalog.application.export.LessonExportQueries
import ca.cashmclean.tagalog.application.export.SentenceExportProjection
import ca.cashmclean.tagalog.application.export.VocabularyExportProjection
import ca.cashmclean.tagalog.domain.Difficulty
import ca.cashmclean.tagalog.domain.PartOfSpeech
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.UUID

class JdbcLessonExportQueries(private val config: DatabaseConfig) : LessonExportQueries {
    override fun lessonExport(lessonId: UUID): LessonExport? = DriverManager.getConnection(config.readOnlyJdbcUrl).use { connection ->
        val lessonName = connection.prepareStatement("SELECT name FROM lesson WHERE id = ?").use {
            it.setString(1, lessonId.toString()); it.executeQuery().use { result -> if (result.next()) result.getString(1) else null }
        } ?: return@use null

        LessonExport(
            lessonId,
            vocabulary(connection, lessonId, lessonName),
            sentences(connection, lessonId, lessonName),
            grammar(connection, lessonId, lessonName),
        )
    }

    private fun vocabulary(connection: Connection, lessonId: UUID, lessonName: String) = query(connection,
        """SELECT v.id, v.tagalog, v.english_meaning, v.root_word, v.part_of_speech, v.difficulty,
                  s.name, s.reference
           FROM lesson_vocabulary a JOIN vocabulary v ON v.id = a.vocabulary_id
           LEFT JOIN source s ON s.id = a.source_id WHERE a.lesson_id = ? ORDER BY v.id""".trimIndent(), lessonId,
    ) { result ->
        val id = UUID.fromString(result.getString(1))
        VocabularyExportProjection(id, result.getString(2), result.getString(3), result.getString(4),
            PartOfSpeech.valueOf(result.getString(5)), Difficulty.valueOf(result.getString(6)),
            stringValues(connection, """SELECT t.name FROM vocabulary_tag r JOIN tag t ON t.id = r.tag_id
                WHERE r.vocabulary_id = ? ORDER BY t.name""".trimIndent(), id),
            association(lessonName, result, 7))
    }

    private fun sentences(connection: Connection, lessonId: UUID, lessonName: String) = query(connection,
        """SELECT x.id, x.text, x.translation, x.difficulty, s.name, s.reference
           FROM lesson_sentence a JOIN sentence x ON x.id = a.sentence_id
           LEFT JOIN source s ON s.id = a.source_id WHERE a.lesson_id = ? ORDER BY x.id""".trimIndent(), lessonId,
    ) { result ->
        val id = UUID.fromString(result.getString(1))
        SentenceExportProjection(id, result.getString(2), result.getString(3), Difficulty.valueOf(result.getString(4)),
            references(connection, """SELECT v.id, v.tagalog FROM sentence_vocabulary r JOIN vocabulary v ON v.id = r.vocabulary_id
                WHERE r.sentence_id = ? ORDER BY v.id""".trimIndent(), id),
            references(connection, """SELECT g.id, g.name FROM sentence_grammar r JOIN grammar_concept g ON g.id = r.grammar_concept_id
                WHERE r.sentence_id = ? ORDER BY g.id""".trimIndent(), id),
            association(lessonName, result, 5))
    }

    private fun grammar(connection: Connection, lessonId: UUID, lessonName: String) = query(connection,
        """SELECT g.id, g.name, g.description, g.formula, s.name, s.reference
           FROM lesson_grammar a JOIN grammar_concept g ON g.id = a.grammar_concept_id
           LEFT JOIN source s ON s.id = a.source_id WHERE a.lesson_id = ? ORDER BY g.id""".trimIndent(), lessonId,
    ) { result ->
        val id = UUID.fromString(result.getString(1))
        GrammarExportProjection(id, result.getString(2), result.getString(3), result.getString(4),
            references(connection, """SELECT x.id, x.text FROM sentence_grammar r JOIN sentence x ON x.id = r.sentence_id
                WHERE r.grammar_concept_id = ? ORDER BY x.id""".trimIndent(), id),
            association(lessonName, result, 5))
    }

    private fun association(lessonName: String, result: ResultSet, sourceIndex: Int) =
        ExportAssociation(lessonName, result.getString(sourceIndex), result.getString(sourceIndex + 1))

    private fun references(connection: Connection, sql: String, id: UUID) = query(connection, sql, id) {
        ExportReference(UUID.fromString(it.getString(1)), it.getString(2))
    }

    private fun stringValues(connection: Connection, sql: String, id: UUID) = query(connection, sql, id) { it.getString(1) }

    private fun <T> query(connection: Connection, sql: String, id: UUID, convert: (ResultSet) -> T): List<T> =
        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, id.toString())
            statement.executeQuery().use { result -> buildList { while (result.next()) add(convert(result)) } }
        }
}
