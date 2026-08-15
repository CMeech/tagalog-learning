package ca.cashmclean.tagalog.cli

import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(name = "grammar", description = ["Manage grammar concepts."], subcommands = [AddGrammarCommand::class])
class GrammarCommand

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
