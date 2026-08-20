package ca.cashmclean.tagalog.application

import ca.cashmclean.tagalog.domain.GrammarConcept
import ca.cashmclean.tagalog.domain.Lesson
import ca.cashmclean.tagalog.domain.Sentence
import ca.cashmclean.tagalog.domain.Vocabulary
import ca.cashmclean.tagalog.domain.Source
import java.time.Instant
import java.util.UUID

enum class KnowledgeEntityType { VOCABULARY, SENTENCE, GRAMMAR }

data class LessonAssociationView(
    val lessonId: UUID,
    val lessonName: String,
    val sourceId: UUID?,
    val sourceName: String?,
)

data class KnowledgeReference(val id: UUID, val displayText: String)

data class LessonSummary(
    val lesson: Lesson,
    val sourceCount: Int,
    val vocabularyCount: Int,
    val sentenceCount: Int,
    val grammarCount: Int,
    val importRunCount: Int,
)

data class LessonEntityView(
    val id: UUID,
    val displayText: String,
    val sourceId: UUID?,
    val sourceName: String?,
)

data class ImportRunView(
    val id: UUID,
    val packageChecksum: String,
    val schemaVersion: Int,
    val importedAt: Instant,
    val inserted: Int,
    val updated: Int,
    val unchanged: Int,
    val newlyRelated: Int,
)

data class LessonDetail(
    val lesson: Lesson,
    val sources: List<Source>,
    val vocabulary: List<LessonEntityView>,
    val sentences: List<LessonEntityView>,
    val grammar: List<LessonEntityView>,
    val importRuns: List<ImportRunView>,
    val sentenceVocabulary: Map<UUID, List<KnowledgeReference>>,
    val sentenceGrammar: Map<UUID, List<KnowledgeReference>>,
)

/** Read boundary for navigating stored knowledge independently of importing and Anki export. */
interface KnowledgeGraphQueries {
    fun lessons(): List<LessonSummary>
    fun lessonDetail(id: UUID): LessonDetail?
    fun lesson(id: UUID): Lesson?
    fun vocabulary(id: UUID): Vocabulary?
    fun sentence(id: UUID): Sentence?
    fun grammar(id: UUID): GrammarConcept?
    fun lessonsFor(type: KnowledgeEntityType, entityId: UUID): List<LessonAssociationView>
    fun sentencesUsingVocabulary(vocabularyId: UUID): List<KnowledgeReference>
    fun sentencesUsingGrammar(grammarId: UUID): List<KnowledgeReference>
    fun vocabularyUsedBySentence(sentenceId: UUID): List<KnowledgeReference>
    fun grammarUsedBySentence(sentenceId: UUID): List<KnowledgeReference>
}
