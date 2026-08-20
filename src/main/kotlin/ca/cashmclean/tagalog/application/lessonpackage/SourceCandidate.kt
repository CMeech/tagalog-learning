package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.SourceType
import java.util.UUID

data class SourceCandidate(val id: UUID, val name: String, val type: SourceType, val reference: String?)
