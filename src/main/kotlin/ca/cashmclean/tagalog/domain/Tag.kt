package ca.cashmclean.tagalog.domain

import java.util.UUID

data class Tag(
    val id: UUID,
    val name: String,
) {
    init {
        requireNotBlank(name, "name")
    }
}
