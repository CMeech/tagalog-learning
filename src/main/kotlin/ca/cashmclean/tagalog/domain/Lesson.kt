package ca.cashmclean.tagalog.domain

import java.util.UUID

data class Lesson(
    val id: UUID,
    val name: String,
    val description: String? = null,
) {
    init {
        requireNotBlank(name, "name")
        requireNotBlankIfPresent(description, "description")
    }
}
