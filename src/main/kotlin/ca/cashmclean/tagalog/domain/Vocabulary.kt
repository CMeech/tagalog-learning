package ca.cashmclean.tagalog.domain

import java.util.UUID

data class Vocabulary(
    val id: UUID,
    val tagalog: String,
    val englishMeaning: String,
    val rootWord: String? = null,
    val partOfSpeech: PartOfSpeech,
    val difficulty: Difficulty,
    val frequencyRank: Int? = null,
) {
    init {
        requireNotBlank(tagalog, "tagalog")
        requireNotBlank(englishMeaning, "englishMeaning")
        requireNotBlankIfPresent(rootWord, "rootWord")
        require(frequencyRank == null || frequencyRank > 0) {
            "frequencyRank must be positive when provided"
        }
    }
}
