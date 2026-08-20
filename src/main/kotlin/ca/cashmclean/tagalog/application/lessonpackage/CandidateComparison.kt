package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.GrammarConcept
import ca.cashmclean.tagalog.domain.Lesson
import ca.cashmclean.tagalog.domain.Sentence
import ca.cashmclean.tagalog.domain.Source
import ca.cashmclean.tagalog.domain.Vocabulary
import java.util.UUID

/** Normalized global entity content used for equality comparison. */
internal interface ComparisonContent

internal interface CandidateComparisonPolicy<C, S> {
    val type: String
    val filename: String
    fun candidateId(candidate: C): UUID
    fun storedId(stored: S): UUID
    fun candidateContent(candidate: C): ComparisonContent
    fun storedContent(stored: S): ComparisonContent
    fun diagnostics(candidate: C, row: Long?): List<PackageDiagnostic>
    fun row(index: Int): Long? = (index + 1).toLong()
}

internal fun interface CandidateComparison {
    fun validate(errors: MutableList<PackageDiagnostic>, assessments: MutableList<CandidateAssessment>)
}

internal class TypedCandidateComparison<C, S>(
    private val policy: CandidateComparisonPolicy<C, S>,
    private val candidates: List<C>,
    private val stored: List<S>,
    private val allowUpdates: Boolean = false,
) : CandidateComparison {
    override fun validate(errors: MutableList<PackageDiagnostic>, assessments: MutableList<CandidateAssessment>) {
        candidates.forEachIndexed { index, candidate -> errors += policy.diagnostics(candidate, policy.row(index)) }

        val storedById = stored.associateBy(policy::storedId)
        val storedIdsByContent = stored.groupBy(policy::storedContent, policy::storedId)
        candidates.groupBy(policy::candidateContent, policy::candidateId)
            .filterValues { it.distinct().size > 1 }
            .values
            .forEach { ids -> ids.forEach { id -> errors += duplicate(id, ids.first { it != id }) } }

        candidates.forEachIndexed { index, candidate ->
            val id = policy.candidateId(candidate)
            val content = policy.candidateContent(candidate)
            val existing = storedById[id]
            val disposition = when {
                existing == null -> CandidateDisposition.INSERT
                policy.storedContent(existing) == content -> CandidateDisposition.UNCHANGED
                allowUpdates -> CandidateDisposition.UPDATE
                else -> CandidateDisposition.CONFLICT
            }
            assessments += CandidateAssessment(policy.type, id, disposition)
            if (disposition == CandidateDisposition.CONFLICT) {
                errors += packageDiagnostic(
                    policy.filename, policy.row(index), "id", id.toString(),
                    "Stored UUID has different normalized content.",
                    "Re-import with --update-existing only if this is an intentional correction.",
                )
            }
            storedIdsByContent[content].orEmpty().filter { it != id }.forEach { errors += duplicate(id, it) }
        }
    }

    private fun duplicate(id: UUID, other: UUID) = packageDiagnostic(
        policy.filename, null, "id", id.toString(),
        "Content duplicates record $other under a different UUID.",
        "Reuse the established UUID or change the content if this is a distinct record.",
    )
}

internal object LessonComparison : CandidateComparisonPolicy<LessonCandidate, StoredLesson> {
    override val type = "lesson"
    override val filename = "lesson.json"
    override fun candidateId(candidate: LessonCandidate) = candidate.id
    override fun storedId(stored: StoredLesson) = stored.id
    override fun candidateContent(candidate: LessonCandidate): ComparisonContent = LessonComparisonContent(candidate.name, candidate.description)
    override fun storedContent(stored: StoredLesson): ComparisonContent = LessonComparisonContent(stored.name, stored.description)
    override fun row(index: Int): Long? = null
    override fun diagnostics(candidate: LessonCandidate, row: Long?) = domainDiagnostics(filename, row, "lesson", candidate.id) {
        Lesson(candidate.id, candidate.name, candidate.description)
    }
}

internal object SourceComparison : CandidateComparisonPolicy<SourceCandidate, StoredSource> {
    override val type = "source"
    override val filename = "lesson.json"
    override fun candidateId(candidate: SourceCandidate) = candidate.id
    override fun storedId(stored: StoredSource) = stored.id
    override fun candidateContent(candidate: SourceCandidate): ComparisonContent = SourceComparisonContent(candidate.name, candidate.type.name, candidate.reference)
    override fun storedContent(stored: StoredSource): ComparisonContent = SourceComparisonContent(stored.name, stored.type, stored.reference)
    override fun diagnostics(candidate: SourceCandidate, row: Long?) = domainDiagnostics(filename, row, "sources", candidate.id) {
        Source(candidate.id, candidate.name, candidate.type, candidate.reference)
    }
}

internal object VocabularyComparison : CandidateComparisonPolicy<VocabularyCandidate, StoredVocabulary> {
    override val type = "vocabulary"
    override val filename = "vocabulary.csv"
    override fun candidateId(candidate: VocabularyCandidate) = candidate.id
    override fun storedId(stored: StoredVocabulary) = stored.id
    override fun candidateContent(candidate: VocabularyCandidate): ComparisonContent = VocabularyComparisonContent(candidate.tagalog, candidate.english, candidate.rootWord, candidate.partOfSpeech.name, candidate.difficulty.name, candidate.frequencyRank, candidate.tags)
    override fun storedContent(stored: StoredVocabulary): ComparisonContent = VocabularyComparisonContent(stored.tagalog, stored.english, stored.rootWord, stored.partOfSpeech, stored.difficulty, stored.frequencyRank, stored.tags)
    override fun diagnostics(candidate: VocabularyCandidate, row: Long?): List<PackageDiagnostic> = buildList {
        addAll(domainDiagnostics(filename, row, null, candidate.id) {
            Vocabulary(candidate.id, candidate.tagalog, candidate.english, candidate.rootWord, candidate.partOfSpeech, candidate.difficulty, candidate.frequencyRank)
        })
        candidate.tags.filter { it.lowercase() != it || it.any(Char::isWhitespace) || '|' in it }.forEach { tag ->
            add(packageDiagnostic(filename, row, "tags", tag, "Tag '$tag' is not a lowercase token.", "Use lowercase text without whitespace or pipe characters; use :: for hierarchy."))
        }
    }
}

internal object SentenceComparison : CandidateComparisonPolicy<SentenceCandidate, StoredSentence> {
    override val type = "sentence"
    override val filename = "sentences.csv"
    override fun candidateId(candidate: SentenceCandidate) = candidate.id
    override fun storedId(stored: StoredSentence) = stored.id
    override fun candidateContent(candidate: SentenceCandidate): ComparisonContent = SentenceComparisonContent(candidate.text, candidate.translation, candidate.difficulty.name, candidate.vocabularyIds, candidate.grammarIds)
    override fun storedContent(stored: StoredSentence): ComparisonContent = SentenceComparisonContent(stored.text, stored.translation, stored.difficulty, stored.vocabularyIds, stored.grammarIds)
    override fun diagnostics(candidate: SentenceCandidate, row: Long?) = domainDiagnostics(filename, row, null, candidate.id) {
        Sentence(candidate.id, candidate.text, candidate.translation, candidate.difficulty)
    }
}

internal object GrammarComparison : CandidateComparisonPolicy<GrammarCandidate, StoredGrammar> {
    override val type = "grammar"
    override val filename = "grammar.csv"
    override fun candidateId(candidate: GrammarCandidate) = candidate.id
    override fun storedId(stored: StoredGrammar) = stored.id
    override fun candidateContent(candidate: GrammarCandidate): ComparisonContent = GrammarComparisonContent(candidate.name, candidate.description, candidate.formula)
    override fun storedContent(stored: StoredGrammar): ComparisonContent = GrammarComparisonContent(stored.name, stored.description, stored.formula)
    override fun diagnostics(candidate: GrammarCandidate, row: Long?) = domainDiagnostics(filename, row, null, candidate.id) {
        GrammarConcept(candidate.id, candidate.name, candidate.description, candidate.formula)
    }
}

private data class LessonComparisonContent(val name: String, val description: String?) : ComparisonContent
private data class SourceComparisonContent(val name: String, val type: String, val reference: String?) : ComparisonContent
private data class VocabularyComparisonContent(val tagalog: String, val english: String, val rootWord: String?, val partOfSpeech: String, val difficulty: String, val frequencyRank: Int?, val tags: Set<String>) : ComparisonContent
private data class SentenceComparisonContent(val text: String, val translation: String, val difficulty: String, val vocabularyIds: Set<UUID>, val grammarIds: Set<UUID>) : ComparisonContent
private data class GrammarComparisonContent(val name: String, val description: String, val formula: String) : ComparisonContent

private fun domainDiagnostics(filename: String, row: Long?, column: String?, id: UUID, create: () -> Any): List<PackageDiagnostic> = try {
    create()
    emptyList()
} catch (exception: IllegalArgumentException) {
    listOf(packageDiagnostic(filename, row, column, id.toString(), exception.message ?: "Invalid domain value", "Correct the value to satisfy the domain rule."))
}

internal fun packageDiagnostic(filename: String, row: Long?, column: String?, value: String?, message: String, guidance: String) =
    PackageDiagnostic(filename, row, column, PackageDiagnostic.safeValue(value), message, guidance)
