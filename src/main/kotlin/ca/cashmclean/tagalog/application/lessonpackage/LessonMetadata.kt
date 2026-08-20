package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

data class LessonMetadata(
    val lesson: LessonCandidate,
    val sources: List<SourceCandidate>,
    val defaultSourceId: UUID?,
)
