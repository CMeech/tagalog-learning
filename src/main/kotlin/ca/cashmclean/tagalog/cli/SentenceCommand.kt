package ca.cashmclean.tagalog.cli

import ca.cashmclean.tagalog.domain.Difficulty
import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(name = "sentence", description = ["Manage sentences."], subcommands = [AddSentenceCommand::class])
class SentenceCommand

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
