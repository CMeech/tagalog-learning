package ca.cashmclean.tagalog

import ca.cashmclean.tagalog.cli.TagalogCommand
import picocli.CommandLine
import kotlin.system.exitProcess

const val APPLICATION_VERSION = "0.1.0"

fun main(args: Array<String>) {
    exitProcess(CommandLine(TagalogCommand()).execute(*args))
}
