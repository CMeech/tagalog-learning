package ca.cashmclean.tagalog.cli

import ca.cashmclean.tagalog.domain.Difficulty
import ca.cashmclean.tagalog.domain.PartOfSpeech
import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(name = "vocabulary", description = ["Manage vocabulary."], subcommands = [AddVocabularyCommand::class])
class VocabularyCommand

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
