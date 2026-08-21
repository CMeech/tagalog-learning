package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.GrammarConcept

internal object GrammarContentValidator {
    fun validate(candidates: List<GrammarCandidate>, stored: List<StoredGrammar>, errors: MutableList<PackageDiagnostic>, assessments: MutableList<CandidateAssessment>, allowUpdates: Boolean) {
        candidates.forEachIndexed { index, concept -> errors += domainDiagnostics("$.grammar[$index]", concept.id) { GrammarConcept(concept.id, concept.name, concept.description, concept.formula) } }
        CandidateMatcher.assess("grammar", candidates, stored, GrammarCandidate::id, StoredGrammar::id,
            { GrammarContent(it.name, it.description, it.formula) }, { GrammarContent(it.name, it.description, it.formula) }, { "$.grammar[$it]" }, errors, assessments, allowUpdates)
    }
}
