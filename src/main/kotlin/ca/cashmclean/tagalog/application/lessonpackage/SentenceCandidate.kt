package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.Difficulty
import java.util.UUID

data class SentenceCandidate(
    val id: UUID,
    val text: String,
    val translation: String,
    val difficulty: Difficulty,
    val sourceId: UUID?,
    val vocabularyIds: Set<UUID>,
    val grammarIds: Set<UUID>,
)
