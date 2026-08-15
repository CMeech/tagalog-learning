package ca.cashmclean.tagalog

import ca.cashmclean.tagalog.cli.TagalogCommand
import picocli.CommandLine
import kotlin.system.exitProcess

const val APPLICATION_VERSION = "0.1.0"

fun main(args: Array<String>) {
    val commandLine = CommandLine(TagalogCommand())
    commandLine.executionExceptionHandler = CommandLine.IExecutionExceptionHandler { exception, command, _ ->
        command.err.println("Error: ${exception.message ?: exception::class.simpleName}")
        1
    }
    exitProcess(commandLine.execute(*args))
}
