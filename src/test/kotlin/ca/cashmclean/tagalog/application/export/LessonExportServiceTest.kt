package ca.cashmclean.tagalog.application.export

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID

class LessonExportServiceTest {
    @TempDir lateinit var temporaryDirectory: Path

    @Test
    fun `read or rendering preparation failure leaves destination unchanged`() {
        val destination = temporaryDirectory.resolve("export")
        val service = LessonExportService(LessonExportQueries { error("simulated read failure") })

        val exception = assertThrows(LessonExportException::class.java) {
            service.export(UUID.randomUUID(), destination)
        }

        assertEquals(LessonExportException.Kind.WRITE_FAILED, exception.kind)
        assertFalse(java.nio.file.Files.exists(destination))
        assertEquals(emptyList<String>(), java.nio.file.Files.list(temporaryDirectory).use { it.map { path -> path.fileName.toString() }.toList() })
    }
}
