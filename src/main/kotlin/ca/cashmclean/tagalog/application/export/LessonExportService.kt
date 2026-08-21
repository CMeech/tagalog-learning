package ca.cashmclean.tagalog.application.export

import ca.cashmclean.tagalog.infrastructure.export.anki.AnkiGrammarExporter
import ca.cashmclean.tagalog.infrastructure.export.anki.AnkiSentenceExporter
import ca.cashmclean.tagalog.infrastructure.export.anki.AnkiVocabularyExporter
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.UUID

data class ExportFileResult(val name: String, val sha256: String, val rowCount: Int)
data class LessonExportResult(
    val lessonId: UUID,
    val output: Path,
    val exportedAt: Instant,
    val files: List<ExportFileResult>,
)

class LessonExportException(message: String, val kind: Kind) : RuntimeException(message) {
    enum class Kind { DESTINATION_EXISTS, LESSON_NOT_FOUND, WRITE_FAILED }
}

class LessonExportService(
    private val queries: LessonExportQueries,
    private val clock: Clock = Clock.systemUTC(),
    private val json: ObjectMapper = ObjectMapper(),
) {
    fun export(lessonId: UUID, destination: Path): LessonExportResult {
        val absoluteDestination = destination.toAbsolutePath().normalize()
        if (Files.exists(absoluteDestination)) throw LessonExportException(
            "Export destination already exists: $absoluteDestination", LessonExportException.Kind.DESTINATION_EXISTS,
        )
        val projection = try {
            queries.lessonExport(lessonId)
        } catch (exception: Exception) {
            throw LessonExportException("Failed to read lesson $lessonId: ${exception.message}", LessonExportException.Kind.WRITE_FAILED)
        } ?: throw LessonExportException("Lesson not found: $lessonId", LessonExportException.Kind.LESSON_NOT_FOUND)
        val parent = absoluteDestination.parent ?: Path.of(".").toAbsolutePath().normalize()
        if (!Files.isDirectory(parent)) throw LessonExportException(
            "Export destination parent does not exist: $parent", LessonExportException.Kind.WRITE_FAILED,
        )
        val temporary = Files.createTempDirectory(parent, ".${absoluteDestination.fileName}.tmp-")
        try {
            val files = buildList {
                if (projection.vocabulary.isNotEmpty()) add(write(temporary, "vocabulary.tsv",
                    AnkiVocabularyExporter().let { it.render(it.export(projection.vocabulary)) }, projection.vocabulary.size))
                if (projection.sentences.isNotEmpty()) add(write(temporary, "sentences.tsv",
                    AnkiSentenceExporter().let { it.render(it.export(projection.sentences)) }, projection.sentences.size))
                if (projection.grammar.isNotEmpty()) add(write(temporary, "grammar.tsv",
                    AnkiGrammarExporter().let { it.render(it.export(projection.grammar)) }, projection.grammar.size))
            }
            val exportedAt = clock.instant()
            val manifest = linkedMapOf(
                "schema_version" to 1, "lesson_id" to lessonId.toString(), "exported_at" to exportedAt.toString(),
                "records" to linkedMapOf(
                    "vocabulary" to projection.vocabulary.map { it.id.toString() },
                    "sentences" to projection.sentences.map { it.id.toString() },
                    "grammar" to projection.grammar.map { it.id.toString() },
                ),
                "files" to files.associate { it.name to linkedMapOf("sha256" to it.sha256, "row_count" to it.rowCount) },
            )
            Files.writeString(temporary.resolve("export.json"), json.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + "\n")
            try {
                Files.move(temporary, absoluteDestination, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, absoluteDestination)
            }
            return LessonExportResult(lessonId, absoluteDestination, exportedAt, files)
        } catch (exception: LessonExportException) {
            cleanup(temporary); throw exception
        } catch (exception: Exception) {
            cleanup(temporary)
            throw LessonExportException("Failed to export lesson $lessonId: ${exception.message}", LessonExportException.Kind.WRITE_FAILED)
        }
    }

    private fun write(directory: Path, name: String, content: String, rows: Int): ExportFileResult {
        val bytes = content.toByteArray(Charsets.UTF_8)
        Files.write(directory.resolve(name), bytes)
        return ExportFileResult(name, MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }, rows)
    }

    private fun cleanup(directory: Path) {
        if (!Files.exists(directory)) return
        Files.walk(directory).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
    }
}
