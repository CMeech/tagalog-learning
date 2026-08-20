package ca.cashmclean.tagalog.application.lessonpackage

data class KnowledgeRepositories(
    val lessons: LessonRepository,
    val sources: SourceRepository,
    val vocabulary: VocabularyRepository,
    val sentences: SentenceRepository,
    val grammar: GrammarRepository,
) {
    fun readStoredKnowledge() = StoredLessonPackageSnapshot(
        lessons = lessons.findAll(),
        sources = sources.findAll(),
        vocabulary = vocabulary.findAll(),
        sentences = sentences.findAll(),
        grammar = grammar.findAll(),
    )

    companion object {
        fun containing(stored: StoredLessonPackageSnapshot) = KnowledgeRepositories(
            lessons = LessonRepository { stored.lessons },
            sources = SourceRepository { stored.sources },
            vocabulary = VocabularyRepository { stored.vocabulary },
            sentences = SentenceRepository { stored.sentences },
            grammar = GrammarRepository { stored.grammar },
        )
    }
}
