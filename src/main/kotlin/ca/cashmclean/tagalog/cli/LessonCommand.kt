package ca.cashmclean.tagalog.cli

import ca.cashmclean.tagalog.application.lessonpackage.LessonPackageLoader
import ca.cashmclean.tagalog.application.lessonpackage.LessonPackageValidationResult
import ca.cashmclean.tagalog.application.lessonpackage.LessonPackageValidator
import ca.cashmclean.tagalog.application.lessonpackage.PackageDiagnostic
import ca.cashmclean.tagalog.infrastructure.database.DatabaseConfig
import ca.cashmclean.tagalog.infrastructure.database.SqliteKnowledgeRepositories
import com.fasterxml.jackson.databind.ObjectMapper
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.nio.file.Path

@Command(name = "lesson", description = ["Manage lesson packages."], subcommands = [ValidateLessonCommand::class])
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
        val validator = LessonPackageValidator(LessonPackageLoader(), SqliteKnowledgeRepositories(config).create())
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
