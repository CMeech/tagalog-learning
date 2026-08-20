package ca.cashmclean.tagalog.application.lessonpackage

internal object PackageContentValidator {
    fun validate(candidate: LessonPackageCandidate, stored: StoredLessonPackageSnapshot, errors: MutableList<PackageDiagnostic>, assessments: MutableList<CandidateAssessment>) {
        LessonContentValidator.validate(candidate.lesson, stored.lessons, errors, assessments)
        SourceContentValidator.validate(candidate.sources, stored.sources, errors, assessments)
        VocabularyContentValidator.validate(candidate.vocabulary, stored.vocabulary, errors, assessments)
        SentenceContentValidator.validate(candidate.sentences, stored.sentences, errors, assessments)
        GrammarContentValidator.validate(candidate.grammar, stored.grammar, errors, assessments)
    }
}
