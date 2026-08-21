package ca.cashmclean.tagalog.cli

import ca.cashmclean.tagalog.domain.Difficulty
import ca.cashmclean.tagalog.domain.PartOfSpeech
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import ca.cashmclean.tagalog.infrastructure.database.DatabaseConfig
import ca.cashmclean.tagalog.infrastructure.database.JdbcKnowledgeGraphQueries
import java.util.UUID

@Command(name = "vocabulary", description = ["Manage vocabulary."], subcommands = [AddVocabularyCommand::class, ShowVocabularyCommand::class, DeleteVocabularyCommand::class])
class VocabularyCommand

@Command(name = "show", description = ["Show a vocabulary entry and all relationships."])
class ShowVocabularyCommand : java.util.concurrent.Callable<Int> {
    @Parameters(index = "0", paramLabel = "<id>") lateinit var id: UUID
    @Option(names = ["--format"], defaultValue = "text") lateinit var format: OutputFormat
    override fun call(): Int {
        val detail = JdbcKnowledgeGraphQueries(DatabaseConfig.fromEnvironment()).vocabularyDetail(id)
            ?: return notFound("Vocabulary", id, format)
        if (format == OutputFormat.json) println(commandJson.writeValueAsString(linkedMapOf(
            "found" to true, "vocabulary" to linkedMapOf("id" to id.toString(), "tagalog" to detail.vocabulary.tagalog,
                "english" to detail.vocabulary.englishMeaning, "root_word" to detail.vocabulary.rootWord,
                "part_of_speech" to detail.vocabulary.partOfSpeech.name, "difficulty" to detail.vocabulary.difficulty.name,
                "frequency_rank" to detail.vocabulary.frequencyRank),
            "tags" to detail.tags.map { it.toJson() }, "lessons" to detail.lessons.map { it.toJson() },
            "used_by_sentences" to detail.usedBySentences.map { it.toJson() },
        ))) else {
            println("Vocabulary: ${detail.vocabulary.tagalog} ($id)")
            println("English: ${detail.vocabulary.englishMeaning}")
            println("Root word: ${detail.vocabulary.rootWord ?: ""}")
            println("Part of speech: ${detail.vocabulary.partOfSpeech}; Difficulty: ${detail.vocabulary.difficulty}; Frequency rank: ${detail.vocabulary.frequencyRank ?: ""}")
            println("Tags: ${detail.tags.joinToString { it.displayText }.ifEmpty { "(none)" }}")
            printAssociations(detail.lessons)
            println("Used by sentences: ${detail.usedBySentences.joinToString { "${it.displayText} (${it.id})" }.ifEmpty { "(none)" }}")
        }
        return 0
    }
}

@Command(name = "delete", description = ["Explicitly delete an unreferenced vocabulary entry."])
class DeleteVocabularyCommand : java.util.concurrent.Callable<Int> {
    @Parameters(index = "0", paramLabel = "<id>") lateinit var id: UUID
    @Option(names = ["--format"], defaultValue = "text") lateinit var format: OutputFormat
    override fun call() = deleteEntity("vocabulary", id, format) { it.deleteVocabulary(id) }
}

@Command(name = "add", description = ["Add a vocabulary entry."])
class AddVocabularyCommand : DatabaseCommand() {
    @Option(names = ["--tagalog"], required = true)
    lateinit var tagalog: String

    @Option(names = ["--english"], required = true)
    lateinit var english: String

    @Option(names = ["--root"])
    var rootWord: String? = null

    @Option(names = ["--part-of-speech"], required = true)
    lateinit var partOfSpeech: PartOfSpeech

    @Option(names = ["--difficulty"], defaultValue = "BEGINNER")
    lateinit var difficulty: Difficulty

    @Option(names = ["--frequency-rank"])
    var frequencyRank: Int? = null

    override fun call(): Int {
        val created = learningCollection.createVocabulary(
            tagalog, english, rootWord, partOfSpeech, difficulty, frequencyRank,
        )
        println("Vocabulary created: ${created.id}")
        return 0
    }
}
