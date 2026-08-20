package ca.cashmclean.tagalog.application

import ca.cashmclean.tagalog.domain.GrammarConcept
import ca.cashmclean.tagalog.domain.Lesson
import ca.cashmclean.tagalog.domain.Sentence
import ca.cashmclean.tagalog.domain.Vocabulary
import java.util.UUID

enum class KnowledgeEntityType { VOCABULARY, SENTENCE, GRAMMAR }

data class LessonAssociationView(
    val lessonId: UUID,
    val lessonName: String,
    val sourceId: UUID?,
    val sourceName: String?,
)

data class KnowledgeReference(val id: UUID, val displayText: String)

/** Read boundary for navigating stored knowledge independently of importing and Anki export. */
interface KnowledgeGraphQueries {
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
