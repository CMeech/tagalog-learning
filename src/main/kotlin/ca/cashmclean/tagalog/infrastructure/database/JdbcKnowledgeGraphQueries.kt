package ca.cashmclean.tagalog.infrastructure.database

import ca.cashmclean.tagalog.application.KnowledgeEntityType
import ca.cashmclean.tagalog.application.KnowledgeGraphQueries
import ca.cashmclean.tagalog.application.KnowledgeReference
import ca.cashmclean.tagalog.application.LessonAssociationView
import ca.cashmclean.tagalog.domain.Difficulty
import ca.cashmclean.tagalog.domain.GrammarConcept
import ca.cashmclean.tagalog.domain.Lesson
import ca.cashmclean.tagalog.domain.PartOfSpeech
import ca.cashmclean.tagalog.domain.Sentence
import ca.cashmclean.tagalog.domain.Vocabulary
import java.sql.DriverManager
import java.util.UUID

class JdbcKnowledgeGraphQueries(private val config: DatabaseConfig) : KnowledgeGraphQueries {
    override fun lesson(id: UUID): Lesson? = queryOne("SELECT id, name, description FROM lesson WHERE id = ?", id) {
        Lesson(UUID.fromString(it.getString(1)), it.getString(2), it.getString(3))
    }

    override fun vocabulary(id: UUID): Vocabulary? = queryOne(
        """SELECT id, tagalog, english_meaning, root_word, part_of_speech, difficulty, frequency_rank
           FROM vocabulary WHERE id = ?""".trimIndent(),
        id,
    ) {
        Vocabulary(
            UUID.fromString(it.getString(1)), it.getString(2), it.getString(3), it.getString(4),
            PartOfSpeech.valueOf(it.getString(5)), Difficulty.valueOf(it.getString(6)),
            it.getInt(7).let { value -> if (it.wasNull()) null else value },
        )
    }

    override fun sentence(id: UUID): Sentence? = queryOne("SELECT id, text, translation, difficulty FROM sentence WHERE id = ?", id) {
        Sentence(UUID.fromString(it.getString(1)), it.getString(2), it.getString(3), Difficulty.valueOf(it.getString(4)))
    }

    override fun grammar(id: UUID): GrammarConcept? = queryOne("SELECT id, name, description, formula FROM grammar_concept WHERE id = ?", id) {
        GrammarConcept(UUID.fromString(it.getString(1)), it.getString(2), it.getString(3), it.getString(4))
    }

    override fun lessonsFor(type: KnowledgeEntityType, entityId: UUID): List<LessonAssociationView> {
        val (table, column) = when (type) {
            KnowledgeEntityType.VOCABULARY -> "lesson_vocabulary" to "vocabulary_id"
            KnowledgeEntityType.SENTENCE -> "lesson_sentence" to "sentence_id"
            KnowledgeEntityType.GRAMMAR -> "lesson_grammar" to "grammar_concept_id"
        }
        return query(
            """SELECT l.id, l.name, a.source_id, s.name
               FROM $table a
               JOIN lesson l ON l.id = a.lesson_id
               LEFT JOIN source s ON s.id = a.source_id
               WHERE a.$column = ?
               ORDER BY l.id""".trimIndent(),
            entityId,
        ) { result ->
            LessonAssociationView(
                UUID.fromString(result.getString(1)), result.getString(2),
                result.getString(3)?.let(UUID::fromString), result.getString(4),
            )
        }
    }

    override fun sentencesUsingVocabulary(vocabularyId: UUID) = references(
        """SELECT s.id, s.text FROM sentence_vocabulary r
           JOIN sentence s ON s.id = r.sentence_id
           WHERE r.vocabulary_id = ? ORDER BY s.id""".trimIndent(),
        vocabularyId,
    )

    override fun sentencesUsingGrammar(grammarId: UUID) = references(
        """SELECT s.id, s.text FROM sentence_grammar r
           JOIN sentence s ON s.id = r.sentence_id
           WHERE r.grammar_concept_id = ? ORDER BY s.id""".trimIndent(),
        grammarId,
    )

    override fun vocabularyUsedBySentence(sentenceId: UUID) = references(
        """SELECT v.id, v.tagalog FROM sentence_vocabulary r
           JOIN vocabulary v ON v.id = r.vocabulary_id
           WHERE r.sentence_id = ? ORDER BY v.id""".trimIndent(),
        sentenceId,
    )

    override fun grammarUsedBySentence(sentenceId: UUID) = references(
        """SELECT g.id, g.name FROM sentence_grammar r
           JOIN grammar_concept g ON g.id = r.grammar_concept_id
           WHERE r.sentence_id = ? ORDER BY g.id""".trimIndent(),
        sentenceId,
    )

    private fun references(sql: String, id: UUID): List<KnowledgeReference> = query(sql, id) {
        KnowledgeReference(UUID.fromString(it.getString(1)), it.getString(2))
    }

    private fun <T> query(sql: String, id: UUID, convert: (java.sql.ResultSet) -> T): List<T> =
        DriverManager.getConnection(config.readOnlyJdbcUrl).use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, id.toString())
                statement.executeQuery().use { result -> buildList { while (result.next()) add(convert(result)) } }
            }
        }

    private fun <T> queryOne(sql: String, id: UUID, convert: (java.sql.ResultSet) -> T): T? =
        DriverManager.getConnection(config.readOnlyJdbcUrl).use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, id.toString())
                statement.executeQuery().use { result -> if (result.next()) convert(result) else null }
            }
        }
}
