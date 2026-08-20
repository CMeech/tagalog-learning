package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

internal object CandidateMatcher {
    fun <Candidate, Stored, Content> assess(
        type: String, filename: String, candidates: List<Candidate>, stored: List<Stored>,
        candidateId: (Candidate) -> UUID, storedId: (Stored) -> UUID,
        candidateContent: (Candidate) -> Content, storedContent: (Stored) -> Content,
        row: (Int) -> Long?, errors: MutableList<PackageDiagnostic>, assessments: MutableList<CandidateAssessment>,
    ) {
        val storedById = stored.associateBy(storedId)
        val storedIdsByContent = stored.groupBy(storedContent, storedId)
        candidates.groupBy(candidateContent, candidateId).filterValues { it.distinct().size > 1 }.values
            .forEach { ids -> ids.forEach { id -> errors += duplicate(filename, id, ids.first { it != id }) } }
        candidates.forEachIndexed { index, candidate ->
            val id = candidateId(candidate)
            val content = candidateContent(candidate)
            val existing = storedById[id]
            val disposition = when {
                existing == null -> CandidateDisposition.INSERT
                storedContent(existing) == content -> CandidateDisposition.UNCHANGED
                else -> CandidateDisposition.CONFLICT
            }
            assessments += CandidateAssessment(type, id, disposition)
            if (disposition == CandidateDisposition.CONFLICT) {
                errors += packageDiagnostic(filename, row(index), "id", id.toString(), "Stored UUID has different normalized content.", "Re-import with --update-existing only if this is an intentional correction.")
            }
            storedIdsByContent[content].orEmpty().filter { it != id }.forEach { errors += duplicate(filename, id, it) }
        }
    }

    private fun duplicate(filename: String, id: UUID, other: UUID) = packageDiagnostic(
        filename, null, "id", id.toString(), "Content duplicates record $other under a different UUID.",
        "Reuse the established UUID or change the content if this is a distinct record.",
    )
}
