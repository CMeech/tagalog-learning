package ca.cashmclean.tagalog.cli

import picocli.CommandLine.Command

@Command(name = "validate", description = ["Validate the learning collection."])
class ValidateCommand : DatabaseCommand() {
    override fun call(): Int {
        database.validate()
        println("Database migrations are valid.")
        return 0
    }
}
