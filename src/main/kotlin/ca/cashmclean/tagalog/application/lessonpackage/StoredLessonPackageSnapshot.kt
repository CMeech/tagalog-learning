package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

data class StoredLessonPackageSnapshot(
    val lessons: List<StoredLesson> = emptyList(),
    val sources: List<StoredSource> = emptyList(),
    val vocabulary: List<StoredVocabulary> = emptyList(),
    val sentences: List<StoredSentence> = emptyList(),
    val grammar: List<StoredGrammar> = emptyList(),
)
