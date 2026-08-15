package ca.cashmclean.tagalog.application.export

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ExportModelTest {
    @Test
    fun `document rejects rows whose width differs from its columns`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExportDocument(listOf("Front", "Back"), listOf(ExportRow(listOf("only one"))))
        }
    }
}
