package ca.cashmclean.tagalog.application.lessonpackage

internal object PackageContentValidator {
    fun validate(candidate: LessonPackageCandidate, stored: StoredLessonPackageSnapshot, errors: MutableList<PackageDiagnostic>, assessments: MutableList<CandidateAssessment>, allowUpdates: Boolean) {
        LessonContentValidator.validate(candidate.lesson, stored.lessons, errors, assessments, allowUpdates)
        SourceContentValidator.validate(candidate.sources, stored.sources, errors, assessments, allowUpdates)
        VocabularyContentValidator.validate(candidate.vocabulary, stored.vocabulary, errors, assessments, allowUpdates)
        SentenceContentValidator.validate(candidate.sentences, stored.sentences, errors, assessments, allowUpdates)
        GrammarContentValidator.validate(candidate.grammar, stored.grammar, errors, assessments, allowUpdates)
    }
}
