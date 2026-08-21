package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

data class GrammarCandidate(val id: UUID, val name: String, val description: String, val formula: String, val sourceId: UUID?)
