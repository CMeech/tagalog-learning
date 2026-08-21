package ca.cashmclean.tagalog.application.lessonpackage

internal data class VocabularyContent(
    val tagalog: String,
    val english: String,
    val rootWord: String?,
    val partOfSpeech: String,
    val difficulty: String,
    val frequencyRank: Int?,
    val tags: Set<String>,
)
