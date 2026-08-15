package ca.cashmclean.tagalog.application

import ca.cashmclean.tagalog.domain.Difficulty
import ca.cashmclean.tagalog.domain.GrammarConcept
import ca.cashmclean.tagalog.domain.Lesson
import ca.cashmclean.tagalog.domain.PartOfSpeech
import ca.cashmclean.tagalog.domain.Sentence
import ca.cashmclean.tagalog.domain.Source
import ca.cashmclean.tagalog.domain.SourceType
import ca.cashmclean.tagalog.domain.Tag
import ca.cashmclean.tagalog.domain.Vocabulary
import ca.cashmclean.tagalog.infrastructure.database.DatabaseManager
import ca.cashmclean.tagalog.infrastructure.database.GrammarConcepts
import ca.cashmclean.tagalog.infrastructure.database.Lessons
import ca.cashmclean.tagalog.infrastructure.database.Sentences
import ca.cashmclean.tagalog.infrastructure.database.Sources
import ca.cashmclean.tagalog.infrastructure.database.Tags
import ca.cashmclean.tagalog.infrastructure.database.VocabularyTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class LearningCollectionService(private val databaseManager: DatabaseManager) {
    fun createVocabulary(
        tagalog: String,
        englishMeaning: String,
        rootWord: String?,
        partOfSpeech: PartOfSpeech,
        difficulty: Difficulty,
        frequencyRank: Int?,
    ): Vocabulary {
        val vocabulary = Vocabulary(
            id = UUID.randomUUID(),
            tagalog = tagalog.trim(),
            englishMeaning = englishMeaning.trim(),
            rootWord = rootWord?.trim(),
            partOfSpeech = partOfSpeech,
            difficulty = difficulty,
            frequencyRank = frequencyRank,
        )
        transaction(databaseManager.connect()) {
            VocabularyTable.insert {
                it[id] = vocabulary.id.toString()
                it[VocabularyTable.tagalog] = vocabulary.tagalog
                it[VocabularyTable.englishMeaning] = vocabulary.englishMeaning
                it[VocabularyTable.rootWord] = vocabulary.rootWord
                it[VocabularyTable.partOfSpeech] = vocabulary.partOfSpeech.name
                it[VocabularyTable.difficulty] = vocabulary.difficulty.name
                it[VocabularyTable.frequencyRank] = vocabulary.frequencyRank
            }
        }
        return vocabulary
    }

    fun createSentence(text: String, translation: String, difficulty: Difficulty): Sentence {
        val sentence = Sentence(UUID.randomUUID(), text.trim(), translation.trim(), difficulty)
        transaction(databaseManager.connect()) {
            Sentences.insert {
                it[id] = sentence.id.toString()
                it[Sentences.text] = sentence.text
                it[Sentences.translation] = sentence.translation
                it[Sentences.difficulty] = sentence.difficulty.name
            }
        }
        return sentence
    }

    fun createGrammarConcept(name: String, description: String, formula: String): GrammarConcept {
        val concept = GrammarConcept(UUID.randomUUID(), name.trim(), description.trim(), formula.trim())
        transaction(databaseManager.connect()) {
            GrammarConcepts.insert {
                it[id] = concept.id.toString()
                it[GrammarConcepts.name] = concept.name
                it[GrammarConcepts.description] = concept.description
                it[GrammarConcepts.formula] = concept.formula
            }
        }
        return concept
    }

    fun validate(): ValidationResult {
        databaseManager.validate()
        val errors = transaction(databaseManager.connect()) {
            buildList {
                VocabularyTable.selectAll().forEach { row -> validateRow("vocabulary", row[VocabularyTable.id]) { row.toVocabulary() } }
                Sentences.selectAll().forEach { row -> validateRow("sentence", row[Sentences.id]) { row.toSentence() } }
                GrammarConcepts.selectAll().forEach { row -> validateRow("grammar concept", row[GrammarConcepts.id]) { row.toGrammarConcept() } }
                Lessons.selectAll().forEach { row -> validateRow("lesson", row[Lessons.id]) { row.toLesson() } }
                Sources.selectAll().forEach { row -> validateRow("source", row[Sources.id]) { row.toSource() } }
                Tags.selectAll().forEach { row -> validateRow("tag", row[Tags.id]) { row.toTag() } }
            }
        }
        return ValidationResult(errors)
    }

    private fun MutableList<String>.validateRow(type: String, id: String, create: () -> Any) {
        try {
            create()
        } catch (exception: IllegalArgumentException) {
            add("Invalid $type '$id': ${exception.message}")
        }
    }

    private fun ResultRow.toVocabulary() = Vocabulary(
        id = UUID.fromString(this[VocabularyTable.id]),
        tagalog = this[VocabularyTable.tagalog],
        englishMeaning = this[VocabularyTable.englishMeaning],
        rootWord = this[VocabularyTable.rootWord],
        partOfSpeech = enumValueOf(this[VocabularyTable.partOfSpeech]),
        difficulty = enumValueOf(this[VocabularyTable.difficulty]),
        frequencyRank = this[VocabularyTable.frequencyRank],
    )

    private fun ResultRow.toSentence() = Sentence(
        id = UUID.fromString(this[Sentences.id]),
        text = this[Sentences.text],
        translation = this[Sentences.translation],
        difficulty = enumValueOf(this[Sentences.difficulty]),
    )

    private fun ResultRow.toGrammarConcept() = GrammarConcept(
        id = UUID.fromString(this[GrammarConcepts.id]),
        name = this[GrammarConcepts.name],
        description = this[GrammarConcepts.description],
        formula = this[GrammarConcepts.formula],
    )

    private fun ResultRow.toLesson() = Lesson(
        id = UUID.fromString(this[Lessons.id]),
        name = this[Lessons.name],
        description = this[Lessons.description],
    )

    private fun ResultRow.toSource() = Source(
        id = UUID.fromString(this[Sources.id]),
        name = this[Sources.name],
        type = enumValueOf<SourceType>(this[Sources.type]),
        reference = this[Sources.reference],
    )

    private fun ResultRow.toTag() = Tag(
        id = UUID.fromString(this[Tags.id]),
        name = this[Tags.name],
    )
}

data class ValidationResult(val errors: List<String>) {
    val isValid: Boolean get() = errors.isEmpty()
}
