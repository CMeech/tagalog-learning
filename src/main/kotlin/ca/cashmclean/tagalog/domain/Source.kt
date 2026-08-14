package ca.cashmclean.tagalog.domain

import java.util.UUID

enum class SourceType {
    TEACHER,
    COURSE,
    BOOK,
    SONG,
    VIDEO,
    WEBSITE,
    OTHER,
}

data class Source(
    val id: UUID,
    val name: String,
    val type: SourceType,
    val reference: String? = null,
) {
    init {
        requireNotBlank(name, "name")
        requireNotBlankIfPresent(reference, "reference")
    }
}
