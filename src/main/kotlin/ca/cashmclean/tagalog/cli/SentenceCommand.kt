package ca.cashmclean.tagalog.cli

import ca.cashmclean.tagalog.domain.Difficulty
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import ca.cashmclean.tagalog.infrastructure.database.DatabaseConfig
import ca.cashmclean.tagalog.infrastructure.database.JdbcKnowledgeGraphQueries
import java.util.UUID

@Command(name = "sentence", description = ["Manage sentences."], subcommands = [AddSentenceCommand::class, ShowSentenceCommand::class, DeleteSentenceCommand::class])
class SentenceCommand

@Command(name = "show", description = ["Show a sentence and all relationships."])
class ShowSentenceCommand : java.util.concurrent.Callable<Int> {
    @Parameters(index = "0", paramLabel = "<id>") lateinit var id: UUID
    @Option(names = ["--format"], defaultValue = "text") lateinit var format: OutputFormat
    override fun call(): Int {
        val detail = JdbcKnowledgeGraphQueries(DatabaseConfig.fromEnvironment()).sentenceDetail(id)
            ?: return notFound("Sentence", id, format)
        if (format == OutputFormat.json) println(commandJson.writeValueAsString(linkedMapOf(
            "found" to true, "sentence" to linkedMapOf("id" to id.toString(), "tagalog" to detail.sentence.text,
                "english" to detail.sentence.translation, "difficulty" to detail.sentence.difficulty.name),
            "lessons" to detail.lessons.map { it.toJson() }, "vocabulary" to detail.vocabulary.map { it.toJson() },
            "grammar" to detail.grammar.map { it.toJson() },
        ))) else {
            println("Sentence: ${detail.sentence.text} ($id)"); println("English: ${detail.sentence.translation}")
            println("Difficulty: ${detail.sentence.difficulty}"); printAssociations(detail.lessons)
            println("Vocabulary: ${detail.vocabulary.joinToString { "${it.displayText} (${it.id})" }.ifEmpty { "(none)" }}")
            println("Grammar: ${detail.grammar.joinToString { "${it.displayText} (${it.id})" }.ifEmpty { "(none)" }}")
        }
        return 0
    }
}

@Command(name = "delete", description = ["Explicitly delete a sentence and its outgoing relationships."])
class DeleteSentenceCommand : java.util.concurrent.Callable<Int> {
    @Parameters(index = "0", paramLabel = "<id>") lateinit var id: UUID
    @Option(names = ["--format"], defaultValue = "text") lateinit var format: OutputFormat
    override fun call() = deleteEntity("sentence", id, format) { it.deleteSentence(id) }
}

@Command(name = "add", description = ["Add a sentence."])
class AddSentenceCommand : DatabaseCommand() {
    @Option(names = ["--text"], required = true)
    lateinit var text: String

    @Option(names = ["--translation"], required = true)
    lateinit var translation: String

    @Option(names = ["--difficulty"], defaultValue = "BEGINNER")
    lateinit var difficulty: Difficulty

    override fun call(): Int {
        val created = learningCollection.createSentence(text, translation, difficulty)
        println("Sentence created: ${created.id}")
        return 0
    }
}
