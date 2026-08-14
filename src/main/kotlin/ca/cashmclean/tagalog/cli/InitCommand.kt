package ca.cashmclean.tagalog.cli

import picocli.CommandLine.Command

@Command(name = "init", description = ["Initialize a Tagalog learning workspace."])
class InitCommand : DatabaseCommand() {
    override fun call(): Int {
        val executed = database.migrate()
        println(if (executed == 0) "Database is up to date." else "Database initialized ($executed migration applied).")
        return 0
    }
}
