package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

data class StoredGrammar(
    val id: UUID,
    val name: String,
    val description: String,
    val formula: String,
    val lessonId: UUID?,
    val sourceId: UUID?,
)
