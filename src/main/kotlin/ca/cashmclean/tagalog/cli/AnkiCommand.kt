package ca.cashmclean.tagalog.cli

import ca.cashmclean.tagalog.application.export.LessonExportException
import ca.cashmclean.tagalog.application.export.LessonExportService
import ca.cashmclean.tagalog.infrastructure.database.DatabaseConfig
import ca.cashmclean.tagalog.infrastructure.database.JdbcLessonExportQueries
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.nio.file.Path
import java.util.UUID

@Command(name = "anki", description = ["Export files for manual Anki import."], subcommands = [ExportAnkiCommand::class])
class AnkiCommand

@Command(name = "export", description = ["Export one lesson to a new directory."])
class ExportAnkiCommand : java.util.concurrent.Callable<Int> {
    @Option(names = ["--lesson"], required = true) lateinit var lessonId: UUID
    @Option(names = ["--output"], required = true) lateinit var output: Path
    @Option(names = ["--format"], defaultValue = "text") lateinit var format: OutputFormat

    override fun call(): Int = try {
        val config = DatabaseConfig.fromEnvironment()
        val result = LessonExportService(JdbcLessonExportQueries(config)).export(lessonId, output)
        if (format == OutputFormat.json) println(commandJson.writeValueAsString(linkedMapOf(
            "success" to true, "lesson_id" to lessonId.toString(), "output" to result.output.toString(),
            "exported_at" to result.exportedAt.toString(),
            "files" to result.files.map { linkedMapOf("name" to it.name, "sha256" to it.sha256, "row_count" to it.rowCount) },
        ))) else {
            println("Exported lesson $lessonId to ${result.output}")
            result.files.forEach { println("  ${it.name}: ${it.rowCount} rows, sha256=${it.sha256}") }
            println("  export.json")
        }
        0
    } catch (exception: LessonExportException) {
        if (format == OutputFormat.json) System.err.println(commandJson.writeValueAsString(linkedMapOf(
            "success" to false, "lesson_id" to lessonId.toString(), "output" to output.toString(),
            "error" to exception.kind.name.lowercase(), "message" to exception.message,
        ))) else System.err.println(exception.message)
        2
    }
}
