package ca.cashmclean.tagalog.application.lessonpackage

fun interface GrammarRepository {
    fun findAll(): List<StoredGrammar>
}
