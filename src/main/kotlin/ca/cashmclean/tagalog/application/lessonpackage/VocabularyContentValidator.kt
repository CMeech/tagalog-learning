package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.Vocabulary

internal object VocabularyContentValidator {
    fun validate(candidates: List<VocabularyCandidate>, stored: List<StoredVocabulary>, errors: MutableList<PackageDiagnostic>, assessments: MutableList<CandidateAssessment>) {
        candidates.forEachIndexed { index, word ->
            errors += domainDiagnostics("vocabulary.csv", dataRow(index), null, word.id) { Vocabulary(word.id, word.tagalog, word.english, word.rootWord, word.partOfSpeech, word.difficulty, word.frequencyRank) }
            word.tags.filter { it.lowercase() != it || it.any(Char::isWhitespace) || '|' in it }.forEach { tag ->
                errors += packageDiagnostic("vocabulary.csv", dataRow(index), "tags", tag, "Tag '$tag' is not a lowercase token.", "Use lowercase text without whitespace or pipe characters; use :: for hierarchy.")
            }
        }
        CandidateMatcher.assess("vocabulary", "vocabulary.csv", candidates, stored, VocabularyCandidate::id, StoredVocabulary::id,
            { VocabularyContent(it.tagalog, it.english, it.rootWord, it.partOfSpeech.name, it.difficulty.name, it.frequencyRank, it.tags) },
            { VocabularyContent(it.tagalog, it.english, it.rootWord, it.partOfSpeech, it.difficulty, it.frequencyRank, it.tags) }, ::dataRow, errors, assessments)
    }
}
