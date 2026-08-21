package ca.cashmclean.tagalog.application.lessonpackage

fun interface VocabularyRepository {
    fun findAll(): List<StoredVocabulary>
}
