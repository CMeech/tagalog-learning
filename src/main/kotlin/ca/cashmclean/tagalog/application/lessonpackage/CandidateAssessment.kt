package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

data class CandidateAssessment(val type: String, val id: UUID, val disposition: CandidateDisposition)
