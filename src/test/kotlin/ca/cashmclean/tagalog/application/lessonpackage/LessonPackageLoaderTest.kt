package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.Difficulty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class LessonPackageLoaderTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    private val loader = LessonPackageLoader()

    @Test
    fun `loads canonical JSON into typed candidates`() {
        val candidate = loader.load(canonicalLesson())

        assertEquals(2, candidate.schemaVersion)
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
    fun `normalizes strings and applies defaults once`() {
        val file = writeLesson(
            minimalJson(
                lessonName = "  Cafe\u0301 lesson  ",
                vocabulary = """[{"id":"30000000-0000-4000-8000-000000000001","tagalog":"  oo  ","english":"  yes  ","part_of_speech":"OTHER","tags":[]}]""",
                sentences = """[{"id":"50000000-0000-4000-8000-000000000001","text":"  Kumusta,\nkaibigan?  ","translation":"  Hello friend  ","vocabulary_ids":[],"grammar_ids":[]}]""",
            ),
        )

        val candidate = loader.load(file)

        assertEquals("Café lesson", candidate.lesson.name)
        assertEquals("oo", candidate.vocabulary.single().tagalog)
        assertEquals(Difficulty.BEGINNER, candidate.vocabulary.single().difficulty)
        assertEquals("Kumusta,\nkaibigan?".replace("\\n", "\n"), candidate.sentences.single().text)
    }

    @Test
    fun `validates schema version before the rest of the document`() {
        val file = writeLesson("""{"schema_version":1}""")

        val error = assertThrows(LessonPackageException::class.java) { loader.load(file) }

        assertTrue(error.message!!.contains("supported integer 2"))
        assertEquals("$.schema_version", error.diagnostics.single().path)
    }

    @Test
    fun `rejects syntax duplicate properties unknown properties and null`() {
        listOf(
            "{",
            minimalJson().replace("\"name\":\"Lesson\"", "\"name\":\"Lesson\",\"name\":\"Again\""),
            minimalJson().dropLast(1) + ",\"unexpected\":true}",
            minimalJson().replace("\"name\":\"Lesson\"", "\"name\":null"),
            minimalJson().replace("\"sources\":[],", ""),
        ).forEachIndexed { index, json ->
            val file = temporaryDirectory.resolve("case-$index").also(Files::createDirectories).resolve("lesson.json")
            Files.writeString(file, json)
            assertTrue(assertThrows(LessonPackageException::class.java) { loader.load(file) }.diagnostics.isNotEmpty())
        }
    }

    @Test
    fun `reports every duplicate array item with a JSON path`() {
        val json = minimalJson(
            vocabulary = """[{"id":"30000000-0000-4000-8000-000000000001","tagalog":"oo","english":"yes","part_of_speech":"OTHER","tags":["one","one"]}]""",
        )

        val result = loader.read(writeLesson(json))

        assertTrue(result.diagnostics.count { it.path?.startsWith("$.vocabulary[0].tags[") == true } >= 2)
    }

    @Test
    fun `rejects malformed UUID enum integer BOM and UTF-8`() {
        val invalidValues = listOf(
            minimalJson(vocabulary = """[{"id":"bad","tagalog":"oo","english":"yes","part_of_speech":"OTHER","tags":[]}]"""),
            minimalJson(vocabulary = """[{"id":"30000000-0000-4000-8000-000000000001","tagalog":"oo","english":"yes","part_of_speech":"other","tags":[]}]"""),
            minimalJson(vocabulary = """[{"id":"30000000-0000-4000-8000-000000000001","tagalog":"oo","english":"yes","part_of_speech":"OTHER","frequency_rank":-1,"tags":[]}]"""),
        )
        invalidValues.forEachIndexed { index, json ->
            val file = temporaryDirectory.resolve("invalid-$index").also(Files::createDirectories).resolve("lesson.json")
            Files.writeString(file, json)
            assertThrows(LessonPackageException::class.java) { loader.load(file) }
        }

        val bom = temporaryDirectory.resolve("bom").also(Files::createDirectories).resolve("lesson.json")
        Files.write(bom, byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + minimalJson().toByteArray())
        assertTrue(assertThrows(LessonPackageException::class.java) { loader.load(bom) }.message!!.contains("byte-order mark"))

        val invalidUtf8 = temporaryDirectory.resolve("utf8").also(Files::createDirectories).resolve("lesson.json")
        Files.write(invalidUtf8, byteArrayOf(0xC3.toByte(), 0x28))
        assertTrue(assertThrows(LessonPackageException::class.java) { loader.load(invalidUtf8) }.message!!.contains("UTF-8"))
    }

    @Test
    fun `rejects directories and CSV files`() {
        assertThrows(LessonPackageException::class.java) { loader.load(temporaryDirectory) }
        val csv = temporaryDirectory.resolve("vocabulary.csv")
        Files.writeString(csv, "id,tagalog")
        assertThrows(LessonPackageException::class.java) { loader.load(csv) }
    }

    @Test
    fun `rejects an oversized JSON file before reading it`() {
        val file = temporaryDirectory.resolve("lesson.json")
        Files.newByteChannel(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            channel.position(LessonPackageLoader.MAX_FILE_BYTES)
            channel.write(java.nio.ByteBuffer.wrap(byteArrayOf(0)))
        }

        assertTrue(assertThrows(LessonPackageException::class.java) { loader.load(file) }.message!!.contains("byte limit"))
    }

    private fun canonicalLesson() = Path.of("examples/lesson-package/lesson.json")

    private fun writeLesson(json: String): Path = temporaryDirectory.resolve("lesson.json").also { Files.writeString(it, json) }

    private fun minimalJson(
        lessonName: String = "Lesson",
        vocabulary: String = "[]",
        sentences: String = "[]",
        grammar: String = """[{"id":"40000000-0000-4000-8000-000000000001","name":"Concept","description":"Description","formula":"Formula"}]""",
    ) = """{"schema_version":2,"lesson":{"id":"10000000-0000-4000-8000-000000000001","name":"$lessonName"},"sources":[],"vocabulary":$vocabulary,"sentences":$sentences,"grammar":$grammar}"""
}
