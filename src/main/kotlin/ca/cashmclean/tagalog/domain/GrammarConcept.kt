package ca.cashmclean.tagalog.domain

import java.util.UUID

data class GrammarConcept(
    val id: UUID,
    val name: String,
    val description: String,
    val formula: String,
) {
    init {
        requireNotBlank(name, "name")
        requireNotBlank(description, "description")
        requireNotBlank(formula, "formula")
    }
}
