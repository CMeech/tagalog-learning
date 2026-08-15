package ca.cashmclean.tagalog.cli

import picocli.CommandLine.Command

@Command(name = "validate", description = ["Validate the learning collection."])
class ValidateCommand : DatabaseCommand() {
    override fun call(): Int {
        val result = learningCollection.validate()
        if (result.isValid) {
            println("Learning collection is valid.")
            return 0
        }
        result.errors.forEach(System.err::println)
        return 1
    }
}
