package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.infrastructure.database.DatabaseConfig
import ca.cashmclean.tagalog.infrastructure.database.LessonPackageSnapshotReader
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.time.Instant
import java.util.UUID

data class LessonImportResult(
    val importRunId: UUID,
    val lessonId: UUID,
    val packageChecksum: String,
    val schemaVersion: Int,
    val importedAt: String,
    val inserted: Int,
    val updated: Int,
    val unchanged: Int,
    val newlyRelated: Int,
    val exactRerun: Boolean,
)

class LessonPackageImportException(
    message: String,
    val validation: LessonPackageValidationResult? = null,
) : IllegalArgumentException(message)

class LessonPackageImporter(
    private val config: DatabaseConfig,
    private val loader: LessonPackageLoader = LessonPackageLoader(),
    private val clock: () -> Instant = Instant::now,
    private val idGenerator: () -> UUID = UUID::randomUUID,
) {
    fun importPackage(packageDirectory: Path, updateExisting: Boolean = false): LessonImportResult {
        val checksum = LessonPackageChecksum.calculate(packageDirectory)
        DriverManager.getConnection(config.jdbcUrl).use { connection ->
            previousRun(connection, checksum)?.let { return it.copy(exactRerun = true) }
        }

        val loaded = loader.loadForValidation(packageDirectory)
        val candidate = loaded.candidate
            ?: throw LessonPackageImportException("Lesson package is invalid.", LessonPackageValidationResult(null, loaded.errors, emptyList(), emptyList()))
        val validator = LessonPackageValidator(loader) { LessonPackageSnapshotReader(config).read() }
        val validation = validator.validate(candidate, loadErrors = loaded.errors, allowUpdates = updateExisting)
        if (!validation.isValid) throw LessonPackageImportException(
            "Lesson package is invalid: ${validation.errors.joinToString("; ") { it.message }}",
            validation,
        )

        DriverManager.getConnection(config.jdbcUrl).use { connection ->
            connection.autoCommit = false
            try {
                previousRun(connection, checksum)?.let {
                    connection.rollback()
                    return it.copy(exactRerun = true)
                }
                val result = persist(connection, candidate, validation, checksum, updateExisting)
                connection.commit()
                return result
            } catch (exception: Exception) {
                connection.rollback()
                if (exception is LessonPackageImportException) throw exception
                throw LessonPackageImportException("Import failed and SQLite was rolled back: ${exception.message}")
            }
        }
    }

    private fun persist(
        connection: Connection,
        candidate: LessonPackageCandidate,
        validation: LessonPackageValidationResult,
        checksum: String,
        updateExisting: Boolean,
    ): LessonImportResult {
        val dispositions = validation.assessments.associate { (it.type to it.id) to it.disposition }
        persistLesson(connection, candidate.lesson, dispositions.getValue("lesson" to candidate.lesson.id))
        candidate.sources.forEach { persistSource(connection, it, dispositions.getValue("source" to it.id)) }
        candidate.vocabulary.forEach { persistVocabulary(connection, it, dispositions.getValue("vocabulary" to it.id)) }
        candidate.grammar.forEach { persistGrammar(connection, it, dispositions.getValue("grammar" to it.id)) }
        candidate.sentences.forEach { persistSentence(connection, it, dispositions.getValue("sentence" to it.id)) }

        var newlyRelated = 0
        candidate.sources.forEach { source ->
            newlyRelated += insertAssociation(connection, "lesson_source", candidate.lesson.id, "source_id", source.id, null)
        }
        candidate.vocabulary.forEach { vocabulary ->
            newlyRelated += upsertEntityAssociation(connection, "lesson_vocabulary", candidate.lesson.id, "vocabulary_id", vocabulary.id, vocabulary.sourceId, updateExisting)
        }
        candidate.sentences.forEach { sentence ->
            newlyRelated += upsertEntityAssociation(connection, "lesson_sentence", candidate.lesson.id, "sentence_id", sentence.id, sentence.sourceId, updateExisting)
        }
        candidate.grammar.forEach { grammar ->
            newlyRelated += upsertEntityAssociation(connection, "lesson_grammar", candidate.lesson.id, "grammar_concept_id", grammar.id, grammar.sourceId, updateExisting)
        }

        val runId = idGenerator()
        val importedAt = clock().toString()
        val result = LessonImportResult(
            runId, candidate.lesson.id, checksum, candidate.schemaVersion, importedAt,
            validation.inserts, validation.updates, validation.unchanged, newlyRelated, false,
        )
        connection.execute(
            """INSERT INTO import_run
               (id, lesson_id, package_checksum, schema_version, imported_at,
                inserted_count, updated_count, unchanged_count, newly_related_count)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""".trimIndent(),
            runId, candidate.lesson.id, checksum, candidate.schemaVersion, importedAt,
            result.inserted, result.updated, result.unchanged, result.newlyRelated,
        )
        return result
    }

    private fun persistLesson(connection: Connection, value: LessonCandidate, disposition: CandidateDisposition) {
        when (disposition) {
            CandidateDisposition.INSERT -> connection.execute("INSERT INTO lesson (id, name, description) VALUES (?, ?, ?)", value.id, value.name, value.description)
            CandidateDisposition.UPDATE -> connection.execute("UPDATE lesson SET name = ?, description = ? WHERE id = ?", value.name, value.description, value.id)
            else -> Unit
        }
    }

    private fun persistSource(connection: Connection, value: SourceCandidate, disposition: CandidateDisposition) {
        when (disposition) {
            CandidateDisposition.INSERT -> connection.execute("INSERT INTO source (id, name, type, reference) VALUES (?, ?, ?, ?)", value.id, value.name, value.type.name, value.reference)
            CandidateDisposition.UPDATE -> connection.execute("UPDATE source SET name = ?, type = ?, reference = ? WHERE id = ?", value.name, value.type.name, value.reference, value.id)
            else -> Unit
        }
    }

    private fun persistVocabulary(connection: Connection, value: VocabularyCandidate, disposition: CandidateDisposition) {
        when (disposition) {
            CandidateDisposition.INSERT -> connection.execute(
                """INSERT INTO vocabulary
                   (id, tagalog, english_meaning, root_word, part_of_speech, difficulty, frequency_rank)
                   VALUES (?, ?, ?, ?, ?, ?, ?)""".trimIndent(),
                value.id, value.tagalog, value.english, value.rootWord, value.partOfSpeech.name, value.difficulty.name, value.frequencyRank,
            )
            CandidateDisposition.UPDATE -> connection.execute(
                """UPDATE vocabulary SET tagalog = ?, english_meaning = ?, root_word = ?,
                   part_of_speech = ?, difficulty = ?, frequency_rank = ? WHERE id = ?""".trimIndent(),
                value.tagalog, value.english, value.rootWord, value.partOfSpeech.name, value.difficulty.name, value.frequencyRank, value.id,
            )
            else -> Unit
        }
        if (disposition == CandidateDisposition.INSERT || disposition == CandidateDisposition.UPDATE) {
            connection.execute("DELETE FROM vocabulary_tag WHERE vocabulary_id = ?", value.id)
            value.tags.forEach { name ->
                val tagId = connection.queryString("SELECT id FROM tag WHERE name = ? COLLATE NOCASE", name)
                    ?: idGenerator().also { connection.execute("INSERT INTO tag (id, name) VALUES (?, ?)", it, name) }.toString()
                connection.execute("INSERT INTO vocabulary_tag (vocabulary_id, tag_id) VALUES (?, ?)", value.id, tagId)
            }
        }
    }

    private fun persistGrammar(connection: Connection, value: GrammarCandidate, disposition: CandidateDisposition) {
        when (disposition) {
            CandidateDisposition.INSERT -> connection.execute("INSERT INTO grammar_concept (id, name, description, formula) VALUES (?, ?, ?, ?)", value.id, value.name, value.description, value.formula)
            CandidateDisposition.UPDATE -> connection.execute("UPDATE grammar_concept SET name = ?, description = ?, formula = ? WHERE id = ?", value.name, value.description, value.formula, value.id)
            else -> Unit
        }
    }

    private fun persistSentence(connection: Connection, value: SentenceCandidate, disposition: CandidateDisposition) {
        when (disposition) {
            CandidateDisposition.INSERT -> connection.execute("INSERT INTO sentence (id, text, translation, difficulty) VALUES (?, ?, ?, ?)", value.id, value.text, value.translation, value.difficulty.name)
            CandidateDisposition.UPDATE -> connection.execute("UPDATE sentence SET text = ?, translation = ?, difficulty = ? WHERE id = ?", value.text, value.translation, value.difficulty.name, value.id)
            else -> Unit
        }
        if (disposition == CandidateDisposition.INSERT || disposition == CandidateDisposition.UPDATE) {
            connection.execute("DELETE FROM sentence_vocabulary WHERE sentence_id = ?", value.id)
            connection.execute("DELETE FROM sentence_grammar WHERE sentence_id = ?", value.id)
            value.vocabularyIds.forEach { connection.execute("INSERT INTO sentence_vocabulary (sentence_id, vocabulary_id) VALUES (?, ?)", value.id, it) }
            value.grammarIds.forEach { connection.execute("INSERT INTO sentence_grammar (sentence_id, grammar_concept_id) VALUES (?, ?)", value.id, it) }
        }
    }

    private fun insertAssociation(
        connection: Connection,
        table: String,
        lessonId: UUID,
        entityColumn: String,
        entityId: UUID,
        sourceId: UUID?,
    ): Int = connection.execute("INSERT OR IGNORE INTO $table (lesson_id, $entityColumn${if (sourceId == null) "" else ", source_id"}) VALUES (?, ?${if (sourceId == null) "" else ", ?"})", lessonId, entityId, *listOfNotNull(sourceId).toTypedArray())

    private fun upsertEntityAssociation(
        connection: Connection,
        table: String,
        lessonId: UUID,
        entityColumn: String,
        entityId: UUID,
        sourceId: UUID?,
        updateExisting: Boolean,
    ): Int {
        val existing = connection.queryNullableString(
            "SELECT source_id FROM $table WHERE lesson_id = ? AND $entityColumn = ?",
            lessonId, entityId,
        )
        if (!existing.found) {
            connection.execute("INSERT INTO $table (lesson_id, $entityColumn, source_id) VALUES (?, ?, ?)", lessonId, entityId, sourceId)
            return 1
        }
        val supplied = sourceId?.toString()
        if (existing.value != supplied) {
            if (!updateExisting) throw LessonPackageImportException(
                "Source provenance for $entityId in lesson $lessonId differs; re-import with --update-existing for an intentional correction.",
            )
            connection.execute("UPDATE $table SET source_id = ? WHERE lesson_id = ? AND $entityColumn = ?", sourceId, lessonId, entityId)
        }
        return 0
    }

    private fun previousRun(connection: Connection, checksum: String): LessonImportResult? =
        connection.prepareStatement("SELECT * FROM import_run WHERE package_checksum = ?").use { statement ->
            statement.setString(1, checksum)
            statement.executeQuery().use { result ->
                if (!result.next()) null else LessonImportResult(
                    UUID.fromString(result.getString("id")), UUID.fromString(result.getString("lesson_id")), checksum,
                    result.getInt("schema_version"), result.getString("imported_at"), result.getInt("inserted_count"),
                    result.getInt("updated_count"), result.getInt("unchanged_count"), result.getInt("newly_related_count"), true,
                )
            }
        }
}

object LessonPackageChecksum {
    private val recognizedFiles = listOf("lesson.json", "vocabulary.csv", "sentences.csv", "grammar.csv")

    fun calculate(packageDirectory: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        recognizedFiles.forEach { filename ->
            val path = packageDirectory.resolve(filename)
            if (Files.isRegularFile(path)) {
                val name = filename.toByteArray(Charsets.UTF_8)
                val content = Files.readAllBytes(path)
                digest.update(ByteBuffer.allocate(4).putInt(name.size).array())
                digest.update(name)
                digest.update(ByteBuffer.allocate(8).putLong(content.size.toLong()).array())
                digest.update(content)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

private data class NullableQueryResult(val found: Boolean, val value: String?)

private fun Connection.execute(sql: String, vararg parameters: Any?): Int = prepareStatement(sql).use { statement ->
    statement.bind(parameters)
    statement.executeUpdate()
}

private fun Connection.queryString(sql: String, vararg parameters: Any?): String? = prepareStatement(sql).use { statement ->
    statement.bind(parameters)
    statement.executeQuery().use { if (it.next()) it.getString(1) else null }
}

private fun Connection.queryNullableString(sql: String, vararg parameters: Any?): NullableQueryResult = prepareStatement(sql).use { statement ->
    statement.bind(parameters)
    statement.executeQuery().use { if (it.next()) NullableQueryResult(true, it.getString(1)) else NullableQueryResult(false, null) }
}

private fun PreparedStatement.bind(parameters: Array<out Any?>) {
    parameters.forEachIndexed { index, value -> setObject(index + 1, value?.toString()) }
}
