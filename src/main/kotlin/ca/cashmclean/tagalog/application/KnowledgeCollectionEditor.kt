package ca.cashmclean.tagalog.application

import ca.cashmclean.tagalog.infrastructure.database.DatabaseConfig
import java.sql.DriverManager
import java.util.UUID

sealed interface DeleteResult {
    data object Deleted : DeleteResult
    data object NotFound : DeleteResult
    data class Referenced(val references: List<KnowledgeReference>) : DeleteResult
}

class KnowledgeCollectionEditor(private val config: DatabaseConfig) {
    fun deleteVocabulary(id: UUID): DeleteResult = deleteReferencedEntity(
        id, "vocabulary",
        """SELECT s.id, s.text FROM sentence_vocabulary r JOIN sentence s ON s.id = r.sentence_id
           WHERE r.vocabulary_id = ? ORDER BY s.id""".trimIndent(),
    )

    fun deleteGrammar(id: UUID): DeleteResult = deleteReferencedEntity(
        id, "grammar_concept",
        """SELECT s.id, s.text FROM sentence_grammar r JOIN sentence s ON s.id = r.sentence_id
           WHERE r.grammar_concept_id = ? ORDER BY s.id""".trimIndent(),
    )

    fun deleteSentence(id: UUID): DeleteResult = transaction { connection ->
        val deleted = connection.prepareStatement("DELETE FROM sentence WHERE id = ?").use {
            it.setString(1, id.toString()); it.executeUpdate()
        }
        if (deleted == 1) DeleteResult.Deleted else DeleteResult.NotFound
    }

    private fun deleteReferencedEntity(id: UUID, table: String, referenceSql: String): DeleteResult =
        transaction { connection ->
            val references = connection.prepareStatement(referenceSql).use { statement ->
                statement.setString(1, id.toString())
                statement.executeQuery().use { result ->
                    buildList { while (result.next()) add(KnowledgeReference(UUID.fromString(result.getString(1)), result.getString(2))) }
                }
            }
            if (references.isNotEmpty()) return@transaction DeleteResult.Referenced(references)
            val deleted = connection.prepareStatement("DELETE FROM $table WHERE id = ?").use {
                it.setString(1, id.toString()); it.executeUpdate()
            }
            if (deleted == 1) DeleteResult.Deleted else DeleteResult.NotFound
        }

    private fun <T> transaction(action: (java.sql.Connection) -> T): T =
        DriverManager.getConnection(config.jdbcUrl).use { connection ->
            connection.autoCommit = false
            try {
                action(connection).also { connection.commit() }
            } catch (exception: Exception) {
                connection.rollback()
                throw exception
            }
        }
}
