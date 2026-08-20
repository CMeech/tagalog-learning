package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

data class StoredSentence(
    val id: UUID,
    val text: String,
    val translation: String,
    val difficulty: String,
    val lessonId: UUID?,
    val sourceId: UUID?,
    val vocabularyIds: Set<UUID>,
    val grammarIds: Set<UUID>,
)
