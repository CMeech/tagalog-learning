package ca.cashmclean.tagalog.infrastructure.export.anki

import ca.cashmclean.tagalog.application.export.ExportDocument
import ca.cashmclean.tagalog.application.export.Exporter
import ca.cashmclean.tagalog.application.export.ExportRow
import ca.cashmclean.tagalog.domain.Vocabulary

/**
 * A text-file export foundation for Anki. File writing and note-type creation are
 * intentionally left to a later milestone.
 */
class AnkiVocabularyExporter : Exporter<Vocabulary> {
    override fun export(items: Iterable<Vocabulary>) = ExportDocument(
        columns = COLUMNS,
        rows = items.map { vocabulary ->
            ExportRow(
                listOf(
                    vocabulary.id.toString(),
                    vocabulary.tagalog,
                    vocabulary.englishMeaning,
                    vocabulary.rootWord.orEmpty(),
                    vocabulary.partOfSpeech.name,
                    vocabulary.difficulty.name,
                    vocabulary.frequencyRank?.toString().orEmpty(),
                ),
            )
        },
    )

    fun render(document: ExportDocument): String = buildString {
        appendLine("#separator:Tab")
        appendLine("#html:false")
        append("#columns:")
        appendLine(document.columns.joinToString("\t", transform = ::escape))
        document.rows.forEach { row ->
            appendLine(row.values.joinToString("\t", transform = ::escape))
        }
    }

    private fun escape(value: String): String {
        if (value.none { it == '\t' || it == '\r' || it == '\n' || it == '"' }) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }

    private companion object {
        val COLUMNS = listOf(
            "ID",
            "Tagalog",
            "English",
            "Root Word",
            "Part of Speech",
            "Difficulty",
            "Frequency Rank",
        )
    }
}
