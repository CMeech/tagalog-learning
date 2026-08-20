package ca.cashmclean.tagalog.application.export

import ca.cashmclean.tagalog.domain.Difficulty
import ca.cashmclean.tagalog.domain.PartOfSpeech
import java.util.UUID

/** Lesson-scoped provenance used to render an entity without relying on legacy ownership columns. */
data class ExportAssociation(
    val lessonName: String,
    val sourceName: String? = null,
    val sourceReference: String? = null,
) {
    val sourceDisplay: String
        get() = when {
            sourceName == null -> ""
            sourceReference == null -> sourceName
            else -> "$sourceName — $sourceReference"
        }
}

/** A resolved relationship value. Its UUID remains available for deterministic ordering. */
data class ExportReference(val id: UUID, val displayText: String)

data class VocabularyExportProjection(
    val id: UUID,
    val tagalog: String,
    val english: String,
    val rootWord: String?,
    val partOfSpeech: PartOfSpeech,
    val difficulty: Difficulty,
    val tags: List<String>,
    val association: ExportAssociation,
)

data class SentenceExportProjection(
    val id: UUID,
    val tagalog: String,
    val english: String,
    val difficulty: Difficulty,
    val vocabulary: List<ExportReference>,
    val grammar: List<ExportReference>,
    val association: ExportAssociation,
)

data class GrammarExportProjection(
    val id: UUID,
    val name: String,
    val description: String,
    val formula: String,
    /** All globally related sentences, not only sentences in the exported lesson. */
    val examples: List<ExportReference>,
    val association: ExportAssociation,
)
