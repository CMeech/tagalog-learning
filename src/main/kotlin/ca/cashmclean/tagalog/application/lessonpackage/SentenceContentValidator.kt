package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.Sentence

internal object SentenceContentValidator {
    fun validate(candidates: List<SentenceCandidate>, stored: List<StoredSentence>, errors: MutableList<PackageDiagnostic>, assessments: MutableList<CandidateAssessment>) {
        candidates.forEachIndexed { index, sentence -> errors += domainDiagnostics("$.sentences[$index]", sentence.id) { Sentence(sentence.id, sentence.text, sentence.translation, sentence.difficulty) } }
        CandidateMatcher.assess("sentence", candidates, stored, SentenceCandidate::id, StoredSentence::id,
            { SentenceContent(it.text, it.translation, it.difficulty.name, it.vocabularyIds, it.grammarIds) },
            { SentenceContent(it.text, it.translation, it.difficulty, it.vocabularyIds, it.grammarIds) }, { "$.sentences[$it]" }, errors, assessments)
    }
}
