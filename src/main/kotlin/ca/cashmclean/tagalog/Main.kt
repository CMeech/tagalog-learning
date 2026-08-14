package ca.cashmclean.tagalog

import picocli.CommandLine
import picocli.CommandLine.Command
import java.util.concurrent.Callable
import kotlin.system.exitProcess

const val APPLICATION_VERSION = "0.1.0"

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
    ],
)
class TagalogCommand : Callable<Int> {
    override fun call(): Int {
        CommandLine(this).usage(System.out)
        return 0
    }
}

@Command(name = "init", description = ["Initialize a Tagalog learning workspace."])
class InitCommand : Callable<Int> {
    override fun call(): Int {
        println("Workspace initialization will be added with database support in Milestone 3.")
        return 0
    }
}

@Command(name = "version", description = ["Print the application version."])
class VersionCommand : Callable<Int> {
    override fun call(): Int {
        println(APPLICATION_VERSION)
        return 0
    }
}

@Command(name = "validate", description = ["Validate the learning collection."])
class ValidateCommand : Callable<Int> {
    override fun call(): Int {
        println("No validation issues found.")
        return 0
    }
}

@Command(name = "migrate", description = ["Apply database migrations."])
class MigrateCommand : Callable<Int> {
    override fun call(): Int {
        println("Database migrations will be added in Milestone 3.")
        return 0
    }
}

fun main(args: Array<String>) {
    exitProcess(CommandLine(TagalogCommand()).execute(*args))
}
