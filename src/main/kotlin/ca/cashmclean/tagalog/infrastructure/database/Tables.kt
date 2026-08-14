package ca.cashmclean.tagalog.infrastructure.database

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object Lessons : Table("lesson") {
    val id = varchar("id", 36)
    val name = text("name")
    val description = text("description").nullable()
    override val primaryKey = PrimaryKey(id)
}

object Sources : Table("source") {
    val id = varchar("id", 36)
    val name = text("name")
    val type = varchar("type", 20)
    val reference = text("reference").nullable()
    override val primaryKey = PrimaryKey(id)
}

object Tags : Table("tag") {
    val id = varchar("id", 36)
    val name = text("name").uniqueIndex()
    override val primaryKey = PrimaryKey(id)
}

object VocabularyTable : Table("vocabulary") {
    val id = varchar("id", 36)
    val tagalog = text("tagalog")
    val englishMeaning = text("english_meaning")
    val rootWord = text("root_word").nullable()
    val partOfSpeech = varchar("part_of_speech", 20)
    val difficulty = varchar("difficulty", 20)
    val frequencyRank = integer("frequency_rank").nullable()
    val lessonId = optReference("lesson_id", Lessons.id, onDelete = ReferenceOption.SET_NULL)
    val sourceId = optReference("source_id", Sources.id, onDelete = ReferenceOption.SET_NULL)
    override val primaryKey = PrimaryKey(id)
}

object Sentences : Table("sentence") {
    val id = varchar("id", 36)
    val text = text("text")
    val translation = text("translation")
    val difficulty = varchar("difficulty", 20)
    val lessonId = optReference("lesson_id", Lessons.id, onDelete = ReferenceOption.SET_NULL)
    val sourceId = optReference("source_id", Sources.id, onDelete = ReferenceOption.SET_NULL)
    override val primaryKey = PrimaryKey(id)
}

object GrammarConcepts : Table("grammar_concept") {
    val id = varchar("id", 36)
    val name = text("name")
    val description = text("description")
    val formula = text("formula")
    val lessonId = optReference("lesson_id", Lessons.id, onDelete = ReferenceOption.SET_NULL)
    val sourceId = optReference("source_id", Sources.id, onDelete = ReferenceOption.SET_NULL)
    override val primaryKey = PrimaryKey(id)
}

object VocabularyTags : Table("vocabulary_tag") {
    val vocabularyId = reference("vocabulary_id", VocabularyTable.id, onDelete = ReferenceOption.CASCADE)
    val tagId = reference("tag_id", Tags.id, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(vocabularyId, tagId)
}

object SentenceVocabulary : Table("sentence_vocabulary") {
    val sentenceId = reference("sentence_id", Sentences.id, onDelete = ReferenceOption.CASCADE)
    val vocabularyId = reference("vocabulary_id", VocabularyTable.id, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(sentenceId, vocabularyId)
}

object SentenceGrammar : Table("sentence_grammar") {
    val sentenceId = reference("sentence_id", Sentences.id, onDelete = ReferenceOption.CASCADE)
    val grammarConceptId = reference("grammar_concept_id", GrammarConcepts.id, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(sentenceId, grammarConceptId)
}
