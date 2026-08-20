package ca.cashmclean.tagalog.application.lessonpackage

data class LessonPackageCandidate(
    val schemaVersion: Int,
    val lesson: LessonCandidate,
    val sources: List<SourceCandidate>,
    val defaultSourceId: java.util.UUID?,
    val vocabulary: List<VocabularyCandidate>,
    val sentences: List<SentenceCandidate>,
    val grammar: List<GrammarCandidate>,
)
