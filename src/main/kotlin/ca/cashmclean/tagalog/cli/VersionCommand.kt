package ca.cashmclean.tagalog.cli

import ca.cashmclean.tagalog.APPLICATION_VERSION
import picocli.CommandLine.Command
import java.util.concurrent.Callable

@Command(name = "version", description = ["Print the application version."])
class VersionCommand : Callable<Int> {
    override fun call(): Int {
        println(APPLICATION_VERSION)
        return 0
    }
}
