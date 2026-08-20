package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.Lesson

internal object LessonContentValidator {
    fun validate(candidate: LessonCandidate, stored: List<StoredLesson>, errors: MutableList<PackageDiagnostic>, assessments: MutableList<CandidateAssessment>) {
        errors += domainDiagnostics("lesson.json", null, "lesson", candidate.id) { Lesson(candidate.id, candidate.name, candidate.description) }
        CandidateMatcher.assess("lesson", "lesson.json", listOf(candidate), stored, LessonCandidate::id, StoredLesson::id,
            { LessonContent(it.name, it.description) }, { LessonContent(it.name, it.description) }, { null }, errors, assessments)
    }
}
