package ca.cashmclean.tagalog.cli

import ca.cashmclean.tagalog.application.lessonpackage.LessonPackageLoader
import ca.cashmclean.tagalog.application.lessonpackage.LessonPackageImportException
import ca.cashmclean.tagalog.application.lessonpackage.LessonPackageImporter
import ca.cashmclean.tagalog.application.lessonpackage.LessonPackageValidationResult
import ca.cashmclean.tagalog.application.lessonpackage.LessonPackageValidator
import ca.cashmclean.tagalog.application.lessonpackage.PackageDiagnostic
import ca.cashmclean.tagalog.infrastructure.database.DatabaseConfig
import ca.cashmclean.tagalog.infrastructure.database.LessonPackageSnapshotReader
import ca.cashmclean.tagalog.infrastructure.database.JdbcKnowledgeGraphQueries
import ca.cashmclean.tagalog.application.LessonDetail
import ca.cashmclean.tagalog.application.LessonEntityView
import com.fasterxml.jackson.databind.ObjectMapper
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.nio.file.Path
import java.util.UUID

@Command(
    name = "lesson",
    description = ["Manage lesson packages."],
    subcommands = [ValidateLessonCommand::class, ImportLessonCommand::class, ListLessonsCommand::class, ShowLessonCommand::class],
)
class LessonCommand

enum class OutputFormat { text, json }

@Command(name = "list", description = ["List lessons in the collection."])
class ListLessonsCommand : java.util.concurrent.Callable<Int> {
    @Option(names = ["--format"], defaultValue = "text")
    lateinit var format: OutputFormat

    override fun call(): Int {
        val lessons = JdbcKnowledgeGraphQueries(DatabaseConfig.fromEnvironment()).lessons()
        when (format) {
            OutputFormat.text -> if (lessons.isEmpty()) {
                println("No lessons found.")
            } else lessons.forEach {
                println("${it.lesson.id}  ${it.lesson.name}  (${it.vocabularyCount} vocabulary, ${it.sentenceCount} sentences, ${it.grammarCount} grammar, ${it.sourceCount} sources, ${it.importRunCount} imports)")
            }
            OutputFormat.json -> println(ObjectMapper().writeValueAsString(linkedMapOf(
                "lessons" to lessons.map { summary -> linkedMapOf(
                    "id" to summary.lesson.id.toString(), "name" to summary.lesson.name,
                    "description" to summary.lesson.description,
                    "counts" to linkedMapOf("sources" to summary.sourceCount, "vocabulary" to summary.vocabularyCount,
                        "sentences" to summary.sentenceCount, "grammar" to summary.grammarCount, "import_runs" to summary.importRunCount),
                ) },
            )))
        }
        return 0
    }
}

@Command(name = "show", description = ["Show a lesson and its stored knowledge."])
class ShowLessonCommand : java.util.concurrent.Callable<Int> {
    @Parameters(index = "0", paramLabel = "<lesson-id>")
    lateinit var lessonId: UUID

    @Option(names = ["--format"], defaultValue = "text")
    lateinit var format: OutputFormat

    override fun call(): Int {
        val detail = JdbcKnowledgeGraphQueries(DatabaseConfig.fromEnvironment()).lessonDetail(lessonId)
        if (detail == null) {
            val message = "Lesson not found: $lessonId"
            if (format == OutputFormat.json) System.err.println(ObjectMapper().writeValueAsString(linkedMapOf("found" to false, "lesson_id" to lessonId.toString(), "message" to message)))
            else System.err.println(message)
            return 2
        }
        when (format) {
            OutputFormat.text -> printLesson(detail)
            OutputFormat.json -> println(ObjectMapper().writeValueAsString(detail.toJson()))
        }
        return 0
    }

    private fun printLesson(detail: LessonDetail) {
        println("Lesson: ${detail.lesson.name} (${detail.lesson.id})")
        detail.lesson.description?.let { println("Description: $it") }
        println("Counts: ${detail.sources.size} sources, ${detail.vocabulary.size} vocabulary, ${detail.sentences.size} sentences, ${detail.grammar.size} grammar, ${detail.importRuns.size} imports")
        printEntities("Sources", detail.sources.map { "${it.id}  ${it.name} [${it.type}]${it.reference?.let { reference -> " — $reference" } ?: ""}" })
        printEntities("Vocabulary", detail.vocabulary.map(::entityText))
        printEntities("Sentences", detail.sentences.map { entity ->
            val vocabulary = detail.sentenceVocabulary.getValue(entity.id).joinToString { "${it.displayText} (${it.id})" }
            val grammar = detail.sentenceGrammar.getValue(entity.id).joinToString { "${it.displayText} (${it.id})" }
            "${entityText(entity)}${if (vocabulary.isNotEmpty()) "\n    Vocabulary: $vocabulary" else ""}${if (grammar.isNotEmpty()) "\n    Grammar: $grammar" else ""}"
        })
        printEntities("Grammar", detail.grammar.map(::entityText))
        printEntities("Import history", detail.importRuns.map { "${it.id}  ${it.importedAt}  checksum=${it.packageChecksum}  schema=${it.schemaVersion}  inserted=${it.inserted}, updated=${it.updated}, unchanged=${it.unchanged}, newly-related=${it.newlyRelated}" })
    }

    private fun printEntities(label: String, values: List<String>) {
        println("$label:")
        if (values.isEmpty()) println("  (none)") else values.forEach { println("  $it") }
    }

    private fun entityText(entity: LessonEntityView) = "${entity.id}  ${entity.displayText}  [source: ${entity.sourceName ?: "none"}${entity.sourceId?.let { " ($it)" } ?: ""}]"

    private fun LessonDetail.toJson(): Map<String, Any?> = linkedMapOf(
        "found" to true,
        "lesson" to linkedMapOf("id" to lesson.id.toString(), "name" to lesson.name, "description" to lesson.description),
        "counts" to linkedMapOf("sources" to sources.size, "vocabulary" to vocabulary.size, "sentences" to sentences.size, "grammar" to grammar.size, "import_runs" to importRuns.size),
        "sources" to sources.map { linkedMapOf("id" to it.id.toString(), "name" to it.name, "type" to it.type.name, "reference" to it.reference) },
        "vocabulary" to vocabulary.map { it.toJson() },
        "sentences" to sentences.map { entity -> entity.toJson() + linkedMapOf(
            "vocabulary" to sentenceVocabulary.getValue(entity.id).map { linkedMapOf("id" to it.id.toString(), "text" to it.displayText) },
            "grammar" to sentenceGrammar.getValue(entity.id).map { linkedMapOf("id" to it.id.toString(), "text" to it.displayText) },
        ) },
        "grammar" to grammar.map { it.toJson() },
        "import_history" to importRuns.map { linkedMapOf("id" to it.id.toString(), "package_checksum" to it.packageChecksum,
            "schema_version" to it.schemaVersion, "imported_at" to it.importedAt.toString(),
            "inserted" to it.inserted, "updated" to it.updated, "unchanged" to it.unchanged, "newly_related" to it.newlyRelated) },
    )

    private fun LessonEntityView.toJson() = linkedMapOf(
        "id" to id.toString(), "text" to displayText,
        "source" to sourceId?.let { linkedMapOf("id" to it.toString(), "name" to sourceName) },
    )
}

@Command(name = "validate", description = ["Validate a lesson package without modifying SQLite."])
class ValidateLessonCommand : java.util.concurrent.Callable<Int> {
    @Parameters(index = "0", paramLabel = "<package>")
    lateinit var packageDirectory: Path

    @Option(names = ["--format"], defaultValue = "text")
    lateinit var format: OutputFormat

    override fun call(): Int {
        val config = DatabaseConfig.fromEnvironment()
        val validator = LessonPackageValidator(LessonPackageLoader()) { LessonPackageSnapshotReader(config).read() }
        val result = validator.validate(packageDirectory)
        when (format) {
            OutputFormat.text -> printText(result)
            OutputFormat.json -> println(ObjectMapper().writeValueAsString(result.toJson()))
        }
        return if (result.isValid) 0 else 2
    }

    private fun printText(result: LessonPackageValidationResult) {
        val stream = if (result.isValid) System.out else System.err
        if (result.isValid) {
            stream.println("Lesson package is valid: ${result.lessonId}")
        } else {
            stream.println("Lesson package is invalid (${result.errors.size} errors).")
            result.errors.forEach { stream.println(it.asText()) }
        }
        stream.println("Records: ${result.inserts} insert, ${result.unchanged} unchanged, ${result.conflicts} conflict.")
        if (result.warnings.isNotEmpty()) stream.println("Warnings: ${result.warnings.size}")
    }

    private fun PackageDiagnostic.asText(): String = buildString {
        append(filename)
        row?.let { append(":").append(it) }
        column?.let { append(" [").append(it).append("]") }
        append(": ").append(message)
        value?.let { append(" Supplied: '").append(it).append("'.") }
        append(" ").append(guidance)
    }

    private fun LessonPackageValidationResult.toJson(): Map<String, Any?> = linkedMapOf(
        "valid" to isValid,
        "lesson_id" to lessonId?.toString(),
        "summary" to linkedMapOf(
            "inserts" to inserts,
            "updates" to updates,
            "unchanged" to unchanged,
            "conflicts" to conflicts,
            "warnings" to warnings.size,
            "errors" to errors.size,
        ),
        "records" to assessments.map { linkedMapOf("type" to it.type, "id" to it.id.toString(), "status" to it.disposition.name.lowercase()) },
        "errors" to errors.map { it.toJson() },
        "warnings" to warnings.map { it.toJson() },
    )

    private fun PackageDiagnostic.toJson(): Map<String, Any?> = linkedMapOf(
        "filename" to filename,
        "row" to row,
        "column" to column,
        "value" to value,
        "message" to message,
        "guidance" to guidance,
    )
}

@Command(name = "import", description = ["Validate and atomically import a lesson package."])
class ImportLessonCommand : java.util.concurrent.Callable<Int> {
    @Parameters(index = "0", paramLabel = "<package>")
    lateinit var packageDirectory: Path

    @Option(names = ["--update-existing"])
    var updateExisting: Boolean = false

    @Option(names = ["--format"], defaultValue = "text")
    lateinit var format: OutputFormat

    override fun call(): Int {
        return try {
            val result = LessonPackageImporter(DatabaseConfig.fromEnvironment()).importPackage(packageDirectory, updateExisting)
            when (format) {
                OutputFormat.text -> {
                    val action = if (result.exactRerun) "already imported" else "imported"
                    println("Lesson $action: ${result.lessonId} (run ${result.importRunId})")
                    println("Records: ${result.inserted} inserted, ${result.updated} updated, ${result.unchanged} unchanged, ${result.newlyRelated} newly related.")
                }
                OutputFormat.json -> println(
                    ObjectMapper().writeValueAsString(
                        linkedMapOf(
                            "success" to true,
                            "exact_rerun" to result.exactRerun,
                            "import_run_id" to result.importRunId.toString(),
                            "lesson_id" to result.lessonId.toString(),
                            "package_checksum" to result.packageChecksum,
                            "schema_version" to result.schemaVersion,
                            "imported_at" to result.importedAt,
                            "summary" to linkedMapOf(
                                "inserted" to result.inserted,
                                "updated" to result.updated,
                                "unchanged" to result.unchanged,
                                "newly_related" to result.newlyRelated,
                            ),
                        ),
                    ),
                )
            }
            0
        } catch (exception: LessonPackageImportException) {
            when (format) {
                OutputFormat.text -> {
                    System.err.println(exception.message)
                    exception.validation?.errors?.forEach { System.err.println(it.asText()) }
                }
                OutputFormat.json -> System.err.println(
                    ObjectMapper().writeValueAsString(
                        linkedMapOf(
                            "success" to false,
                            "message" to exception.message,
                            "errors" to exception.validation?.errors?.map { it.toJson() }.orEmpty(),
                        ),
                    ),
                )
            }
            2
        }
    }
}

private fun PackageDiagnostic.asText(): String = buildString {
    append(filename)
    row?.let { append(":").append(it) }
    column?.let { append(" [").append(it).append("]") }
    append(": ").append(message)
    value?.let { append(" Supplied: '").append(it).append("'.") }
    append(" ").append(guidance)
}

private fun PackageDiagnostic.toJson(): Map<String, Any?> = linkedMapOf(
    "filename" to filename,
    "row" to row,
    "column" to column,
    "value" to value,
    "message" to message,
    "guidance" to guidance,
)
