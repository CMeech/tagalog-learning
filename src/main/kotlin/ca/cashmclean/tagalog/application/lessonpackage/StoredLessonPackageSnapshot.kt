package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

data class StoredLessonPackageSnapshot(
    val lessons: List<StoredLesson> = emptyList(),
    val sources: List<StoredSource> = emptyList(),
    val vocabulary: List<StoredVocabulary> = emptyList(),
    val sentences: List<StoredSentence> = emptyList(),
    val grammar: List<StoredGrammar> = emptyList(),
)

data class StoredLesson(val id: UUID, val name: String, val description: String?)
data class StoredSource(val id: UUID, val name: String, val type: String, val reference: String?)
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
data class StoredGrammar(
    val id: UUID,
    val name: String,
    val description: String,
    val formula: String,
    val lessonId: UUID?,
    val sourceId: UUID?,
)
