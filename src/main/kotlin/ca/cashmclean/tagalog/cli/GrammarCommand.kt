package ca.cashmclean.tagalog.cli

import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import ca.cashmclean.tagalog.infrastructure.database.DatabaseConfig
import ca.cashmclean.tagalog.infrastructure.database.JdbcKnowledgeGraphQueries
import java.util.UUID

@Command(name = "grammar", description = ["Manage grammar concepts."], subcommands = [AddGrammarCommand::class, ShowGrammarCommand::class, DeleteGrammarCommand::class])
class GrammarCommand

@Command(name = "show", description = ["Show a grammar concept and all relationships."])
class ShowGrammarCommand : java.util.concurrent.Callable<Int> {
    @Parameters(index = "0", paramLabel = "<id>") lateinit var id: UUID
    @Option(names = ["--format"], defaultValue = "text") lateinit var format: OutputFormat
    override fun call(): Int {
        val detail = JdbcKnowledgeGraphQueries(DatabaseConfig.fromEnvironment()).grammarDetail(id)
            ?: return notFound("Grammar", id, format)
        if (format == OutputFormat.json) println(commandJson.writeValueAsString(linkedMapOf(
            "found" to true, "grammar" to linkedMapOf("id" to id.toString(), "name" to detail.grammar.name,
                "description" to detail.grammar.description, "formula" to detail.grammar.formula),
            "lessons" to detail.lessons.map { it.toJson() }, "example_sentences" to detail.exampleSentences.map { it.toJson() },
        ))) else {
            println("Grammar: ${detail.grammar.name} ($id)"); println("Description: ${detail.grammar.description}")
            println("Formula: ${detail.grammar.formula}"); printAssociations(detail.lessons)
            println("Example sentences: ${detail.exampleSentences.joinToString { "${it.displayText} (${it.id})" }.ifEmpty { "(none)" }}")
        }
        return 0
    }
}

@Command(name = "delete", description = ["Explicitly delete an unreferenced grammar concept."])
class DeleteGrammarCommand : java.util.concurrent.Callable<Int> {
    @Parameters(index = "0", paramLabel = "<id>") lateinit var id: UUID
    @Option(names = ["--format"], defaultValue = "text") lateinit var format: OutputFormat
    override fun call() = deleteEntity("grammar", id, format) { it.deleteGrammar(id) }
}

@Command(name = "add", description = ["Add a grammar concept."])
class AddGrammarCommand : DatabaseCommand() {
    @Option(names = ["--name"], required = true)
    lateinit var name: String

    @Option(names = ["--description"], required = true)
    lateinit var description: String

    @Option(names = ["--formula"], required = true)
    lateinit var formula: String

    override fun call(): Int {
        val created = learningCollection.createGrammarConcept(name, description, formula)
        println("Grammar concept created: ${created.id}")
        return 0
    }
}
