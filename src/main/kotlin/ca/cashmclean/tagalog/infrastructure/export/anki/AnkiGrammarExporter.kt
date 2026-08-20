package ca.cashmclean.tagalog.infrastructure.export.anki

import ca.cashmclean.tagalog.application.export.ExportDocument
import ca.cashmclean.tagalog.application.export.Exporter
import ca.cashmclean.tagalog.application.export.ExportRow
import ca.cashmclean.tagalog.application.export.GrammarExportProjection

class AnkiGrammarExporter : Exporter<GrammarExportProjection> {
    override fun export(items: Iterable<GrammarExportProjection>) = ExportDocument(
        COLUMNS,
        items.sortedBy { it.id }.map { grammar ->
            ExportRow(listOf(
                grammar.id.toString(), grammar.name, grammar.description, grammar.formula,
                grammar.examples.sortedBy { it.id }.joinToString("; ") { it.displayText },
                grammar.association.lessonName, grammar.association.sourceDisplay,
            ))
        },
    )

    fun render(document: ExportDocument) = AnkiTsvRenderer("Tagalog Grammar").render(document)

    private companion object {
        val COLUMNS = listOf("ID", "Name", "Description", "Formula", "Examples", "Lesson", "Source")
    }
}
