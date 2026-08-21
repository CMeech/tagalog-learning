package ca.cashmclean.tagalog.application.lessonpackage

fun interface SentenceRepository {
    fun findAll(): List<StoredSentence>
}
