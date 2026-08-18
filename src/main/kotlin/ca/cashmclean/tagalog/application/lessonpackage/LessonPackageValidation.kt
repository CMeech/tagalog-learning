package ca.cashmclean.tagalog.application.lessonpackage

import java.nio.file.Path
import java.util.UUID

enum class CandidateDisposition { INSERT, UNCHANGED, CONFLICT }

data class CandidateAssessment(val type: String, val id: UUID, val disposition: CandidateDisposition)

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

class LessonPackageValidator(
    private val loader: LessonPackageLoader,
    private val snapshotProvider: () -> StoredLessonPackageSnapshot,
) {
    fun validate(packageDirectory: Path): LessonPackageValidationResult {
        val loaded = loader.loadForValidation(packageDirectory)
        val candidate = loaded.candidate
            ?: return LessonPackageValidationResult(null, loaded.errors, emptyList(), emptyList())
        return validate(candidate, snapshotProvider(), loaded.errors)
    }

    fun validate(
        candidate: LessonPackageCandidate,
        snapshot: StoredLessonPackageSnapshot = snapshotProvider(),
        loadErrors: List<PackageDiagnostic> = emptyList(),
    ): LessonPackageValidationResult {
        val errors = loadErrors.toMutableList()
        val assessments = mutableListOf<CandidateAssessment>()

        PackageIdentityValidator.validate(candidate, errors)
        PackageRelationshipValidator.validate(candidate, snapshot, errors)
        comparisons(candidate, snapshot).forEach { it.validate(errors, assessments) }

        return LessonPackageValidationResult(candidate.lesson.id, errors, emptyList(), assessments)
    }

    private fun comparisons(
        candidate: LessonPackageCandidate,
        snapshot: StoredLessonPackageSnapshot,
    ): List<CandidateComparison> = listOf(
        TypedCandidateComparison(LessonComparison, listOf(candidate.lesson), snapshot.lessons),
        TypedCandidateComparison(SourceComparison, candidate.sources, snapshot.sources),
        TypedCandidateComparison(VocabularyComparison, candidate.vocabulary, snapshot.vocabulary),
        TypedCandidateComparison(SentenceComparison, candidate.sentences, snapshot.sentences),
        TypedCandidateComparison(GrammarComparison, candidate.grammar, snapshot.grammar),
    )
}
