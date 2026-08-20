package ca.cashmclean.tagalog.infrastructure.export.anki

import ca.cashmclean.tagalog.application.export.ExportDocument
import ca.cashmclean.tagalog.application.export.Exporter
import ca.cashmclean.tagalog.application.export.ExportRow
import ca.cashmclean.tagalog.application.export.VocabularyExportProjection

class AnkiVocabularyExporter : Exporter<VocabularyExportProjection> {
    override fun export(items: Iterable<VocabularyExportProjection>) = ExportDocument(
        columns = COLUMNS,
        rows = items.sortedBy { it.id }.map { vocabulary ->
            ExportRow(
                listOf(
                    vocabulary.id.toString(),
                    vocabulary.tagalog,
                    vocabulary.english,
                    vocabulary.rootWord.orEmpty(),
                    vocabulary.partOfSpeech.name,
                    vocabulary.difficulty.name,
                    vocabulary.tags.distinct().sortedWith(UNICODE_COMPARATOR).joinToString(" "),
                    vocabulary.association.lessonName,
                    vocabulary.association.sourceDisplay,
                ),
            )
        },
    )

    fun render(document: ExportDocument): String = AnkiTsvRenderer("Tagalog Vocabulary", tagsColumn = 7).render(document)

    private companion object {
        val COLUMNS = listOf(
            "ID",
            "Tagalog",
            "English",
            "Root Word",
            "Part of Speech",
            "Difficulty",
            "Tags",
            "Lesson",
            "Source",
        )

        val UNICODE_COMPARATOR = Comparator<String> { left, right ->
            val leftPoints = left.codePoints().toArray()
            val rightPoints = right.codePoints().toArray()
            for (index in 0 until minOf(leftPoints.size, rightPoints.size)) {
                if (leftPoints[index] != rightPoints[index]) return@Comparator leftPoints[index].compareTo(rightPoints[index])
            }
            leftPoints.size.compareTo(rightPoints.size)
        }
    }
}
