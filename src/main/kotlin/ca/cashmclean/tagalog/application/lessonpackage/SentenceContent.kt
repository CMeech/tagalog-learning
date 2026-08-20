package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

internal data class SentenceContent(
    val text: String,
    val translation: String,
    val difficulty: String,
    val vocabularyIds: Set<UUID>,
    val grammarIds: Set<UUID>,
)
