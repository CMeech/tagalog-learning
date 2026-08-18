package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.Difficulty
import ca.cashmclean.tagalog.domain.PartOfSpeech
import ca.cashmclean.tagalog.domain.SourceType
import java.util.UUID

data class LessonPackageCandidate(
    val schemaVersion: Int,
    val lesson: LessonCandidate,
    val sources: List<SourceCandidate>,
    val defaultSourceId: UUID?,
    val vocabulary: List<VocabularyCandidate>,
    val sentences: List<SentenceCandidate>,
    val grammar: List<GrammarCandidate>,
)

data class LessonCandidate(val id: UUID, val name: String, val description: String?)

data class SourceCandidate(
    val id: UUID,
    val name: String,
    val type: SourceType,
    val reference: String?,
)

data class VocabularyCandidate(
    val id: UUID,
    val tagalog: String,
    val english: String,
    val rootWord: String?,
    val partOfSpeech: PartOfSpeech,
    val difficulty: Difficulty,
    val frequencyRank: Int?,
    val sourceId: UUID?,
    val tags: Set<String>,
)

data class SentenceCandidate(
    val id: UUID,
    val text: String,
    val translation: String,
    val difficulty: Difficulty,
    val sourceId: UUID?,
    val vocabularyIds: Set<UUID>,
    val grammarIds: Set<UUID>,
)

data class GrammarCandidate(
    val id: UUID,
    val name: String,
    val description: String,
    val formula: String,
    val sourceId: UUID?,
)
