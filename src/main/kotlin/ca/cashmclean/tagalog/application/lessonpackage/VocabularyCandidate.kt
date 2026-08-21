package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.Difficulty
import ca.cashmclean.tagalog.domain.PartOfSpeech
import java.util.UUID

data class VocabularyCandidate(
    val id: UUID,
    val tagalog: String,
    val english: String,
    val rootWord: String?,
    val partOfSpeech: PartOfSpeech,
    val difficulty: Difficulty,
    val frequencyRank: Int?,
    val sourceId: UUID?,
    val tags: Set<String>,
)
