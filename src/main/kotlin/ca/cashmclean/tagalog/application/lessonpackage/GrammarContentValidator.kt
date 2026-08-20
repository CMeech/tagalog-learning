package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.GrammarConcept

internal object GrammarContentValidator {
    fun validate(candidates: List<GrammarCandidate>, stored: List<StoredGrammar>, errors: MutableList<PackageDiagnostic>, assessments: MutableList<CandidateAssessment>) {
        candidates.forEachIndexed { index, concept -> errors += domainDiagnostics("grammar.csv", dataRow(index), null, concept.id) { GrammarConcept(concept.id, concept.name, concept.description, concept.formula) } }
        CandidateMatcher.assess("grammar", "grammar.csv", candidates, stored, GrammarCandidate::id, StoredGrammar::id,
            { GrammarContent(it.name, it.description, it.formula) }, { GrammarContent(it.name, it.description, it.formula) }, ::dataRow, errors, assessments)
    }
}
