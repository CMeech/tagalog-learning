package ca.cashmclean.tagalog.application.lessonpackage

fun interface SourceRepository {
    fun findAll(): List<StoredSource>
}
