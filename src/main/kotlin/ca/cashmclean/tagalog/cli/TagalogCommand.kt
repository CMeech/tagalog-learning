package ca.cashmclean.tagalog.cli

import ca.cashmclean.tagalog.APPLICATION_VERSION
import picocli.CommandLine
import picocli.CommandLine.Command
import java.util.concurrent.Callable

@Command(
    name = "tagalog",
    description = ["Manage a structured Tagalog learning collection."],
    mixinStandardHelpOptions = true,
    version = [APPLICATION_VERSION],
    subcommands = [
        InitCommand::class,
        VersionCommand::class,
        ValidateCommand::class,
        MigrateCommand::class,
        VocabularyCommand::class,
        SentenceCommand::class,
        GrammarCommand::class,
    ],
)
class TagalogCommand : Callable<Int> {
    override fun call(): Int {
        CommandLine(this).usage(System.out)
        return 0
    }
}
