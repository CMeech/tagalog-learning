package ca.cashmclean.tagalog.application.lessonpackage

internal object PackageFiles {
    const val METADATA = "lesson.json"
    const val VOCABULARY = "vocabulary.csv"
    const val SENTENCES = "sentences.csv"
    const val GRAMMAR = "grammar.csv"
    val csvFiles = listOf(VOCABULARY, SENTENCES, GRAMMAR)
    val recognized = listOf(METADATA) + csvFiles
}
