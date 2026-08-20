package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.Difficulty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class LessonPackageLoaderTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    private val loader = LessonPackageLoader()

    @Test
    fun `loads canonical package into typed candidates`() {
        val candidate = loader.load(Path.of("examples/lesson-package"))

        assertEquals(1, candidate.schemaVersion)
        assertEquals("Pagbati at pagpapakilala", candidate.lesson.name)
        assertEquals(1, candidate.sources.size)
        assertEquals(4, candidate.vocabulary.size)
        assertEquals(3, candidate.sentences.size)
        assertEquals(2, candidate.grammar.size)
        assertEquals(Difficulty.BEGINNER, candidate.vocabulary.first().difficulty)
        assertEquals(setOf("pagbati", "oras"), candidate.vocabulary.first().tags)
        assertEquals(candidate.defaultSourceId, candidate.vocabulary.first().sourceId)
        assertEquals(2, candidate.sentences.first().vocabularyIds.size)
    }

    @Test
    fun `normalizes and trims strings once while preserving internal content`() {
        writeMinimumMetadata(name = "  Cafe\u0301 lesson  ")
        Files.writeString(
            temporaryDirectory.resolve("sentences.csv"),
            "id,text,translation,difficulty,source_id,vocabulary_ids,grammar_ids\r\n" +
                "50000000-0000-4000-8000-000000000001,\"  Kumusta,\nkaibigan?  \",  Hello friend  ,,,,\r\n",
        )

        val candidate = loader.load(temporaryDirectory)

        assertEquals("Café lesson", candidate.lesson.name)
        assertEquals("Kumusta,\nkaibigan?", candidate.sentences.single().text)
        assertEquals("Hello friend", candidate.sentences.single().translation)
        assertEquals(Difficulty.BEGINNER, candidate.sentences.single().difficulty)
    }

    @Test
    fun `ignores unknown files but requires a recognized csv`() {
        writeMinimumMetadata()
        Files.writeString(temporaryDirectory.resolve("notes.md"), "working notes")

        val error = assertThrows(LessonPackageException::class.java) { loader.load(temporaryDirectory) }

        assertTrue(error.message!!.contains("at least one recognized CSV"))
    }

    @Test
    fun `validates schema version before other metadata fields`() {
        Files.writeString(temporaryDirectory.resolve("lesson.json"), """{"schema_version":2}""")
        writeHeaderOnlyVocabulary()

        val error = assertThrows(LessonPackageException::class.java) { loader.load(temporaryDirectory) }

        assertEquals("lesson.json schema_version must be the supported integer 1", error.message)
    }

    @Test
    fun `rejects byte order marks explicitly`() {
        writeMinimumMetadata()
        val csv = "id,tagalog,english,root_word,part_of_speech,difficulty,frequency_rank,source_id,tags\n"
        Files.write(temporaryDirectory.resolve("vocabulary.csv"), byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + csv.toByteArray())

        val error = assertThrows(LessonPackageException::class.java) { loader.load(temporaryDirectory) }

        assertTrue(error.message!!.contains("byte-order mark"))
    }

    @Test
    fun `parses quoted commas quotes crlf lists enums and integers`() {
        writeMinimumMetadata()
        Files.writeString(
            temporaryDirectory.resolve("vocabulary.csv"),
            "id,tagalog,english,root_word,part_of_speech,difficulty,frequency_rank,source_id,tags\r\n" +
                "30000000-0000-4000-8000-000000000001,\"oo, \"\"talaga\"\"\",yes,,OTHER,ADVANCED,12,,one|two\r\n",
        )

        val row = loader.load(temporaryDirectory).vocabulary.single()

        assertEquals("oo, \"talaga\"", row.tagalog)
        assertEquals(Difficulty.ADVANCED, row.difficulty)
        assertEquals(12, row.frequencyRank)
        assertEquals(setOf("one", "two"), row.tags)
    }

    @Test
    fun `rejects duplicate and blank list items`() {
        writeMinimumMetadata()
        Files.writeString(
            temporaryDirectory.resolve("vocabulary.csv"),
            "id,tagalog,english,root_word,part_of_speech,difficulty,frequency_rank,source_id,tags\n" +
                "30000000-0000-4000-8000-000000000001,oo,yes,,OTHER,,,,one|one\n",
        )
        assertTrue(assertThrows(LessonPackageException::class.java) { loader.load(temporaryDirectory) }.message!!.contains("duplicate"))

        Files.writeString(
            temporaryDirectory.resolve("vocabulary.csv"),
            "id,tagalog,english,root_word,part_of_speech,difficulty,frequency_rank,source_id,tags\n" +
                "30000000-0000-4000-8000-000000000001,oo,yes,,OTHER,,,,one||two\n",
        )
        assertTrue(assertThrows(LessonPackageException::class.java) { loader.load(temporaryDirectory) }.message!!.contains("blank list item"))
    }

    @Test
    fun `rejects malformed uuid enum integer and utf8 input`() {
        writeMinimumMetadata()
        fun vocabulary(id: String, part: String = "OTHER", rank: String = "") =
            "id,tagalog,english,root_word,part_of_speech,difficulty,frequency_rank,source_id,tags\n" +
                "$id,oo,yes,,$part,,$rank,,\n"

        Files.writeString(temporaryDirectory.resolve("vocabulary.csv"), vocabulary("NOT-A-UUID"))
        assertThrows(LessonPackageException::class.java) { loader.load(temporaryDirectory) }
        Files.writeString(temporaryDirectory.resolve("vocabulary.csv"), vocabulary("30000000-0000-4000-8000-000000000001", "other"))
        assertThrows(LessonPackageException::class.java) { loader.load(temporaryDirectory) }
        Files.writeString(temporaryDirectory.resolve("vocabulary.csv"), vocabulary("30000000-0000-4000-8000-000000000001", rank = "-1"))
        assertThrows(LessonPackageException::class.java) { loader.load(temporaryDirectory) }
        Files.write(temporaryDirectory.resolve("vocabulary.csv"), byteArrayOf(0xC3.toByte(), 0x28))
        assertThrows(LessonPackageException::class.java) { loader.load(temporaryDirectory) }
    }

    @Test
    fun `validation reports every kind of header problem before parsing rows`() {
        writeMinimumMetadata()
        Files.writeString(
            temporaryDirectory.resolve("grammar.csv"),
            "id,Name,description,source_id,source_id,unexpected\n" +
                "bad-id,,,,,\n",
        )

        val result = loader.read(temporaryDirectory)

        assertTrue(result.diagnostics.any { it.message.contains("incorrect case") && it.guidance.contains("name") })
        assertTrue(result.diagnostics.any { it.message.contains("Missing column 'formula'") })
        assertTrue(result.diagnostics.any { it.message.contains("Duplicate column 'source_id'") })
        assertTrue(result.diagnostics.any { it.message.contains("Unexpected column 'unexpected'") })
        assertTrue(result.diagnostics.all { it.row == 0L })
        assertTrue(result.diagnostics.none { it.message.contains("UUID") }, "Rows must not be parsed after an invalid header")
    }

    @Test
    fun `validation collects independent malformed rows`() {
        writeMinimumMetadata()
        Files.writeString(
            temporaryDirectory.resolve("vocabulary.csv"),
            "id,tagalog,english,root_word,part_of_speech,difficulty,frequency_rank,source_id,tags\n" +
                "bad-id,oo,yes,,OTHER,,,,\n" +
                "30000000-0000-4000-8000-000000000001,hindi,no,,wrong-enum,,,,\n",
        )

        val result = loader.read(temporaryDirectory)

        assertEquals(listOf(1L, 2L), result.diagnostics.map { it.row })
        assertTrue(result.diagnostics.all { it.filename == "vocabulary.csv" })
    }

    private fun writeMinimumMetadata(name: String = "Lesson") {
        Files.writeString(
            temporaryDirectory.resolve("lesson.json"),
            """{"schema_version":1,"lesson":{"id":"10000000-0000-4000-8000-000000000001","name":"$name"},"sources":[]}""",
        )
    }

    private fun writeHeaderOnlyVocabulary() {
        Files.writeString(
            temporaryDirectory.resolve("vocabulary.csv"),
            "id,tagalog,english,root_word,part_of_speech,difficulty,frequency_rank,source_id,tags\n",
        )
    }
}
