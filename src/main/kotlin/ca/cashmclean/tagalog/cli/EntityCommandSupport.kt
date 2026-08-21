package ca.cashmclean.tagalog.cli

import ca.cashmclean.tagalog.application.DeleteResult
import ca.cashmclean.tagalog.application.KnowledgeCollectionEditor
import ca.cashmclean.tagalog.application.KnowledgeReference
import ca.cashmclean.tagalog.application.LessonAssociationView
import ca.cashmclean.tagalog.infrastructure.database.DatabaseConfig
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID

internal val commandJson = ObjectMapper()

internal fun LessonAssociationView.toJson() = linkedMapOf(
    "lesson_id" to lessonId.toString(), "lesson_name" to lessonName,
    "source" to sourceId?.let { linkedMapOf("id" to it.toString(), "name" to sourceName, "reference" to sourceReference) },
)

internal fun KnowledgeReference.toJson() = linkedMapOf("id" to id.toString(), "text" to displayText)

internal fun printAssociations(associations: List<LessonAssociationView>) {
    println("Lessons:")
    if (associations.isEmpty()) println("  (none)") else associations.forEach {
        val source = when {
            it.sourceName == null -> "none"
            it.sourceReference == null -> it.sourceName
            else -> "${it.sourceName} — ${it.sourceReference}"
        }
        println("  ${it.lessonId}  ${it.lessonName}  [source: $source${it.sourceId?.let { id -> " ($id)" } ?: ""}]")
    }
}

internal fun deleteEntity(type: String, id: UUID, format: OutputFormat, operation: (KnowledgeCollectionEditor) -> DeleteResult): Int {
    return when (val result = operation(KnowledgeCollectionEditor(DatabaseConfig.fromEnvironment()))) {
        DeleteResult.Deleted -> {
            val notice = "Delete the matching $type note from Anki manually; TSV import cannot remove notes."
            if (format == OutputFormat.json) println(commandJson.writeValueAsString(linkedMapOf(
                "deleted" to true, "type" to type, "id" to id.toString(), "anki_manual_removal_required" to true, "message" to notice,
            ))) else { println("Deleted $type: $id"); println(notice) }
            0
        }
        DeleteResult.NotFound -> {
            val message = "${type.replaceFirstChar(Char::uppercase)} not found: $id"
            if (format == OutputFormat.json) System.err.println(commandJson.writeValueAsString(linkedMapOf(
                "deleted" to false, "type" to type, "id" to id.toString(), "message" to message,
            ))) else System.err.println(message)
            2
        }
        is DeleteResult.Referenced -> {
            val message = "Cannot delete $type $id because it is referenced by ${result.references.size} sentence(s)."
            if (format == OutputFormat.json) System.err.println(commandJson.writeValueAsString(linkedMapOf(
                "deleted" to false, "type" to type, "id" to id.toString(), "message" to message,
                "referencing_sentences" to result.references.map { it.toJson() },
            ))) else { System.err.println(message); result.references.forEach { System.err.println("  ${it.id}  ${it.displayText}") } }
            3
        }
    }
}

internal fun notFound(type: String, id: UUID, format: OutputFormat): Int {
    val message = "$type not found: $id"
    if (format == OutputFormat.json) System.err.println(commandJson.writeValueAsString(linkedMapOf(
        "found" to false, "type" to type.lowercase(), "id" to id.toString(), "message" to message,
    ))) else System.err.println(message)
    return 2
}
