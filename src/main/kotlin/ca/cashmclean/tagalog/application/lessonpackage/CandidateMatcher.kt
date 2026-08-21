package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

internal object CandidateMatcher {
    fun <Candidate, Stored, Content> assess(
        type: String, candidates: List<Candidate>, stored: List<Stored>,
        candidateId: (Candidate) -> UUID, storedId: (Stored) -> UUID,
        candidateContent: (Candidate) -> Content, storedContent: (Stored) -> Content,
        path: (Int) -> String, errors: MutableList<PackageDiagnostic>, assessments: MutableList<CandidateAssessment>,
        allowUpdates: Boolean,
    ) {
        val storedById = stored.associateBy(storedId)
        val storedIdsByContent = stored.groupBy(storedContent, storedId)
        val candidateIndexes = candidates.mapIndexed { index, candidate -> candidateId(candidate) to index }.toMap()
        candidates.groupBy(candidateContent, candidateId).filterValues { it.distinct().size > 1 }.values
            .forEach { ids -> ids.forEach { id -> errors += duplicate("${path(candidateIndexes.getValue(id))}.id", id, ids.first { it != id }) } }
        candidates.forEachIndexed { index, candidate ->
            val id = candidateId(candidate)
            val content = candidateContent(candidate)
            val existing = storedById[id]
            val disposition = when {
                existing == null -> CandidateDisposition.INSERT
                storedContent(existing) == content -> CandidateDisposition.UNCHANGED
                allowUpdates -> CandidateDisposition.UPDATE
                else -> CandidateDisposition.CONFLICT
            }
            assessments += CandidateAssessment(type, id, disposition)
            if (disposition == CandidateDisposition.CONFLICT) {
                errors += packageDiagnostic("${path(index)}.id", id.toString(), "Stored UUID has different normalized content.", "Re-import with --update-existing only if this is an intentional correction.")
            }
            storedIdsByContent[content].orEmpty().filter { it != id }.forEach { errors += duplicate("${path(index)}.id", id, it) }
        }
    }

    private fun duplicate(path: String, id: UUID, other: UUID) = packageDiagnostic(
        path, id.toString(), "Content duplicates record $other under a different UUID.",
        "Reuse the established UUID or change the content if this is a distinct record.",
    )
}
