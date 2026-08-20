package ca.cashmclean.tagalog.application.lessonpackage

fun interface LessonRepository {
    fun findAll(): List<StoredLesson>
}
