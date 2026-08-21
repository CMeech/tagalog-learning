package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.Source

internal object SourceContentValidator {
    fun validate(candidates: List<SourceCandidate>, stored: List<StoredSource>, errors: MutableList<PackageDiagnostic>, assessments: MutableList<CandidateAssessment>, allowUpdates: Boolean) {
        candidates.forEachIndexed { index, source -> errors += domainDiagnostics("$.sources[$index]", source.id) { Source(source.id, source.name, source.type, source.reference) } }
        CandidateMatcher.assess("source", candidates, stored, SourceCandidate::id, StoredSource::id,
            { SourceContent(it.name, it.type.name, it.reference) }, { SourceContent(it.name, it.type, it.reference) }, { "$.sources[$it]" }, errors, assessments, allowUpdates)
    }
}
