package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

data class StoredVocabulary(
    val id: UUID,
    val tagalog: String,
    val english: String,
    val rootWord: String?,
    val partOfSpeech: String,
    val difficulty: String,
    val frequencyRank: Int?,
    val lessonId: UUID?,
    val sourceId: UUID?,
    val tags: Set<String>,
)
