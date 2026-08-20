package ca.cashmclean.tagalog.cli

import ca.cashmclean.tagalog.application.lessonpackage.LessonPackageLoader
import ca.cashmclean.tagalog.application.lessonpackage.LessonPackageImportException
import ca.cashmclean.tagalog.application.lessonpackage.LessonPackageImporter
import ca.cashmclean.tagalog.application.lessonpackage.LessonPackageValidationResult
import ca.cashmclean.tagalog.application.lessonpackage.LessonPackageValidator
import ca.cashmclean.tagalog.application.lessonpackage.PackageDiagnostic
import ca.cashmclean.tagalog.infrastructure.database.DatabaseConfig
import ca.cashmclean.tagalog.infrastructure.database.LessonPackageSnapshotReader
import com.fasterxml.jackson.databind.ObjectMapper
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.nio.file.Path

@Command(
    name = "lesson",
    description = ["Manage lesson packages."],
    subcommands = [ValidateLessonCommand::class, ImportLessonCommand::class],
)
class LessonCommand

enum class OutputFormat { text, json }

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
