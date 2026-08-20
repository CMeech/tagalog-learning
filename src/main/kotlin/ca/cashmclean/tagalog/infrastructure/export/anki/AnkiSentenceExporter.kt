package ca.cashmclean.tagalog.infrastructure.export.anki

import ca.cashmclean.tagalog.application.export.ExportDocument
import ca.cashmclean.tagalog.application.export.Exporter
import ca.cashmclean.tagalog.application.export.ExportRow
import ca.cashmclean.tagalog.application.export.SentenceExportProjection

class AnkiSentenceExporter : Exporter<SentenceExportProjection> {
    override fun export(items: Iterable<SentenceExportProjection>) = ExportDocument(
        COLUMNS,
        items.sortedBy { it.id }.map { sentence ->
            ExportRow(listOf(
                sentence.id.toString(), sentence.tagalog, sentence.english, sentence.difficulty.name,
                sentence.vocabulary.sortedBy { it.id }.joinToString("; ") { it.displayText },
                sentence.grammar.sortedBy { it.id }.joinToString("; ") { it.displayText },
                sentence.association.lessonName, sentence.association.sourceDisplay,
            ))
        },
    )

    fun render(document: ExportDocument) = AnkiTsvRenderer("Tagalog Sentence").render(document)

    private companion object {
        val COLUMNS = listOf("ID", "Tagalog", "English", "Difficulty", "Vocabulary", "Grammar", "Lesson", "Source")
    }
}
