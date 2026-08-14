package ca.cashmclean.tagalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import picocli.CommandLine
import java.io.ByteArrayOutputStream
import java.io.PrintStream

@ResourceLock("SYSTEM_OUT")
class TagalogCommandTest {
    @Test
    fun `root command displays help`() {
        val output = captureOutput { CommandLine(TagalogCommand()).execute() }

        assertEquals(0, output.exitCode)
        assertTrue(output.text.contains("Usage: tagalog"))
        assertTrue(output.text.contains("validate"))
    }

    @Test
    fun `version command prints application version`() {
        val output = captureOutput { CommandLine(TagalogCommand()).execute("version") }

        assertEquals(0, output.exitCode)
        assertEquals(APPLICATION_VERSION, output.text.trim())
    }

    @Test
    fun `all milestone one commands execute successfully`() {
        listOf("init", "validate", "migrate").forEach { command ->
            val output = captureOutput { CommandLine(TagalogCommand()).execute(command) }
            assertEquals(0, output.exitCode, "Expected '$command' to succeed")
        }
    }

    private fun captureOutput(action: () -> Int): CommandOutput {
        val originalOut = System.out
        val bytes = ByteArrayOutputStream()
        return try {
            System.setOut(PrintStream(bytes))
            CommandOutput(action(), bytes.toString(Charsets.UTF_8))
        } finally {
            System.setOut(originalOut)
        }
    }

    private data class CommandOutput(val exitCode: Int, val text: String)
}
