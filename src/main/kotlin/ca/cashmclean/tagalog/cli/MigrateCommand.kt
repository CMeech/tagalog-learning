package ca.cashmclean.tagalog.cli

import picocli.CommandLine.Command

@Command(name = "migrate", description = ["Apply database migrations."])
class MigrateCommand : DatabaseCommand() {
    override fun call(): Int {
        val executed = database.migrate()
        println("Database migration complete ($executed applied).")
        return 0
    }
}
