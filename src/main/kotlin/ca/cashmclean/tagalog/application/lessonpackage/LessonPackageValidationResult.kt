package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

data class LessonPackageValidationResult(
    val lessonId: UUID?,
    val errors: List<PackageDiagnostic>,
    val warnings: List<PackageDiagnostic>,
    val assessments: List<CandidateAssessment>,
) {
    val isValid: Boolean get() = errors.isEmpty()
    val inserts: Int get() = assessments.count { it.disposition == CandidateDisposition.INSERT }
    val unchanged: Int get() = assessments.count { it.disposition == CandidateDisposition.UNCHANGED }
    val conflicts: Int get() = assessments.count { it.disposition == CandidateDisposition.CONFLICT }
}
