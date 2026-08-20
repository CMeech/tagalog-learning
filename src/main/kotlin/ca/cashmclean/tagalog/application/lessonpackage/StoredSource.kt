package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

data class StoredSource(val id: UUID, val name: String, val type: String, val reference: String?)
