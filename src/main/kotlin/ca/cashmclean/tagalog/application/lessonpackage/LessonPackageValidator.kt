package ca.cashmclean.tagalog.application.lessonpackage

import java.nio.file.Path

class LessonPackageValidator(
    private val loader: LessonPackageLoader,
    private val repositories: KnowledgeRepositories,
) {
    fun validate(lessonFile: Path, allowUpdates: Boolean = false): LessonPackageValidationResult {
        val loaded = loader.read(lessonFile)
        val candidate = loaded.lessonPackage
            ?: return LessonPackageValidationResult(null, loaded.diagnostics, emptyList(), emptyList())
        return validate(candidate, repositories.readStoredKnowledge(), loaded.diagnostics, allowUpdates)
    }

    fun validate(
        candidate: LessonPackageCandidate,
        storedKnowledge: StoredLessonPackageSnapshot = repositories.readStoredKnowledge(),
        loadErrors: List<PackageDiagnostic> = emptyList(),
        allowUpdates: Boolean = false,
    ): LessonPackageValidationResult {
        val errors = loadErrors.toMutableList()
        val assessments = mutableListOf<CandidateAssessment>()

        PackageIdentityValidator.validate(candidate, errors)
        PackageRelationshipValidator.validate(candidate, storedKnowledge, errors)
        PackageContentValidator.validate(candidate, storedKnowledge, errors, assessments, allowUpdates)

        return LessonPackageValidationResult(candidate.lesson.id, errors, emptyList(), assessments)
    }
}
