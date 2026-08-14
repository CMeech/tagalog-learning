package ca.cashmclean.tagalog.domain

import java.util.UUID

data class Sentence(
    val id: UUID,
    val text: String,
    val translation: String,
    val difficulty: Difficulty,
) {
    init {
        requireNotBlank(text, "text")
        requireNotBlank(translation, "translation")
    }
}
