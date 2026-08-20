package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.infrastructure.database.DatabaseConfig
import ca.cashmclean.tagalog.infrastructure.database.DatabaseManager
import ca.cashmclean.tagalog.infrastructure.database.JdbcKnowledgeGraphQueries
import ca.cashmclean.tagalog.application.KnowledgeEntityType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.util.UUID

class LessonPackageImporterTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `imports complete package atomically and exact rerun returns original run`() {
        migrate()
        val importer = importer()

        val first = importer.importPackage(samplePackage())
        val rerun = importer.importPackage(samplePackage())

        assertEquals(11, first.inserted)
        assertEquals(0, first.updated)
        assertEquals(10, first.newlyRelated)
        assertFalse(first.exactRerun)
        assertTrue(rerun.exactRerun)
        assertEquals(first.importRunId, rerun.importRunId)
        assertEquals(1, count("import_run"))
        assertEquals(4, count("vocabulary"))
        assertEquals(3, count("sentence"))
        assertEquals(2, count("grammar_concept"))
        assertEquals(10, count("lesson_source") + count("lesson_vocabulary") + count("lesson_sentence") + count("lesson_grammar"))
    }

    @Test
    fun `conflict rolls back and explicit update replaces package-owned content`() {
        migrate()
        val importer = importer()
        importer.importPackage(samplePackage())
        val corrected = copySample("corrected")
        val sentenceFile = corrected.resolve("sentences.csv")
        Files.writeString(
            sentenceFile,
            Files.readString(sentenceFile)
                .replace("Magandang umaga po.", "Magandang umaga po!")
                .replace(",40000000-0000-4000-8000-000000000002\n", ",\n"),
        )
        val vocabularyFile = corrected.resolve("vocabulary.csv")
        Files.writeString(vocabularyFile, Files.readString(vocabularyFile).replace("pagbati|oras", "pagbati"))

        val conflict = assertThrows(LessonPackageImportException::class.java) {
            importer.importPackage(corrected)
        }
        assertTrue(conflict.validation?.conflicts == 2)
        assertEquals(1, count("import_run"))
        assertEquals(0, scalarInt("SELECT count(*) FROM sentence WHERE text = 'Magandang umaga po!'"))

        val updated = importer.importPackage(corrected, updateExisting = true)

        assertEquals(2, updated.updated)
        assertEquals(2, count("import_run"))
        assertEquals(1, scalarInt("SELECT count(*) FROM sentence WHERE text = 'Magandang umaga po!'"))
        assertEquals(0, scalarInt("SELECT count(*) FROM sentence_grammar WHERE sentence_id = '50000000-0000-4000-8000-000000000001'"))
        assertEquals(1, scalarInt("SELECT count(*) FROM vocabulary_tag WHERE vocabulary_id = '30000000-0000-4000-8000-000000000001'"))
    }

    @Test
    fun `global entities can join another lesson with different provenance`() {
        migrate()
        val importer = importer()
        importer.importPackage(samplePackage())
        val second = temporaryDirectory.resolve("second-package")
        Files.createDirectories(second)
        Files.writeString(
            second.resolve("lesson.json"),
            """{
              "schema_version": 1,
              "lesson": {"id":"10000000-0000-4000-8000-000000000002","name":"Second lesson"},
              "sources": [{"id":"20000000-0000-4000-8000-000000000002","name":"Second source","type":"TEACHER"}],
              "default_source_id":"20000000-0000-4000-8000-000000000002"
            }""".trimIndent(),
        )
        Files.writeString(
            second.resolve("vocabulary.csv"),
            """id,tagalog,english,root_word,part_of_speech,difficulty,frequency_rank,source_id,tags
            30000000-0000-4000-8000-000000000001,magandang umaga,good morning,,PHRASE,BEGINNER,42,,oras|pagbati
            """.trimIndent(),
        )
        Files.writeString(
            second.resolve("grammar.csv"),
            """id,name,description,formula,source_id
            40000000-0000-4000-8000-000000000001,Pangungusap na di-karaniwan,"Introduces a predicate before the topic, linked by ay in the inverted form.",Panaguri + ang/si + Paksa,
            """.trimIndent(),
        )
        Files.writeString(
            second.resolve("sentences.csv"),
            """id,text,translation,difficulty,source_id,vocabulary_ids,grammar_ids
            50000000-0000-4000-8000-000000000002,Ako si María.,I am María.,BEGINNER,,30000000-0000-4000-8000-000000000002,40000000-0000-4000-8000-000000000001
            """.trimIndent(),
        )

        val result = importer.importPackage(second)

        assertEquals(2, result.inserted)
        assertEquals(3, result.unchanged)
        assertEquals(4, result.newlyRelated)
        assertEquals(4, count("vocabulary"))
        assertEquals(
            2,
            scalarInt(
                "SELECT count(*) FROM lesson_vocabulary WHERE vocabulary_id = '30000000-0000-4000-8000-000000000001'",
            ),
        )
        val queries = JdbcKnowledgeGraphQueries(config())
        assertEquals(2, queries.lessonsFor(KnowledgeEntityType.VOCABULARY, UUID.fromString("30000000-0000-4000-8000-000000000001")).size)
        assertEquals(2, queries.lessonsFor(KnowledgeEntityType.SENTENCE, UUID.fromString("50000000-0000-4000-8000-000000000002")).size)
        assertEquals(2, queries.lessonsFor(KnowledgeEntityType.GRAMMAR, UUID.fromString("40000000-0000-4000-8000-000000000001")).size)
        assertEquals(
            "20000000-0000-4000-8000-000000000002",
            scalarString(
                """SELECT source_id FROM lesson_vocabulary
                   WHERE lesson_id = '10000000-0000-4000-8000-000000000002'
                   AND vocabulary_id = '30000000-0000-4000-8000-000000000001'""".trimIndent(),
            ),
        )
    }

    @Test
    fun `later sentence resolves relationships to previously imported knowledge`() {
        migrate()
        val importer = importer()
        importer.importPackage(samplePackage())
        val second = temporaryDirectory.resolve("relationship-package")
        Files.createDirectories(second)
        Files.writeString(
            second.resolve("lesson.json"),
            """{"schema_version":1,"lesson":{"id":"10000000-0000-4000-8000-000000000003","name":"Cross week"},"sources":[]}""",
        )
        Files.writeString(
            second.resolve("sentences.csv"),
            """id,text,translation,difficulty,source_id,vocabulary_ids,grammar_ids
            50000000-0000-4000-8000-000000000099,Ako po.,I (polite).,BEGINNER,,30000000-0000-4000-8000-000000000002|30000000-0000-4000-8000-000000000004,40000000-0000-4000-8000-000000000002
            """.trimIndent(),
        )

        importer.importPackage(second)

        assertEquals(2, scalarInt("SELECT count(*) FROM sentence_vocabulary WHERE sentence_id = '50000000-0000-4000-8000-000000000099'"))
        assertEquals(1, scalarInt("SELECT count(*) FROM sentence_grammar WHERE sentence_id = '50000000-0000-4000-8000-000000000099'"))
    }

    @Test
    fun `association provenance conflict rolls back earlier writes in the import transaction`() {
        migrate()
        val importer = importer()
        importer.importPackage(samplePackage())
        val correction = temporaryDirectory.resolve("provenance-conflict")
        Files.createDirectories(correction)
        Files.writeString(
            correction.resolve("lesson.json"),
            """{
              "schema_version":1,
              "lesson":{"id":"10000000-0000-4000-8000-000000000001","name":"Pagbati at pagpapakilala","description":"Mga pangunahing pagbati at magalang na pagpapakilala."},
              "sources":[{"id":"20000000-0000-4000-8000-000000000099","name":"Corrected source","type":"TEACHER"}],
              "default_source_id":"20000000-0000-4000-8000-000000000099"
            }""".trimIndent(),
        )
        Files.writeString(
            correction.resolve("vocabulary.csv"),
            """id,tagalog,english,root_word,part_of_speech,difficulty,frequency_rank,source_id,tags
            30000000-0000-4000-8000-000000000001,magandang umaga,good morning,,PHRASE,BEGINNER,42,,pagbati|oras
            """.trimIndent(),
        )

        assertThrows(LessonPackageImportException::class.java) { importer.importPackage(correction) }

        assertEquals(1, count("import_run"))
        assertEquals(0, scalarInt("SELECT count(*) FROM source WHERE id = '20000000-0000-4000-8000-000000000099'"))
        assertEquals(
            "20000000-0000-4000-8000-000000000001",
            scalarString(
                """SELECT source_id FROM lesson_vocabulary
                   WHERE lesson_id = '10000000-0000-4000-8000-000000000001'
                   AND vocabulary_id = '30000000-0000-4000-8000-000000000001'""".trimIndent(),
            ),
        )

        val corrected = importer.importPackage(correction, updateExisting = true)
        assertEquals(1, corrected.inserted)
        assertEquals(2, count("import_run"))
        assertEquals(
            "20000000-0000-4000-8000-000000000099",
            scalarString(
                """SELECT source_id FROM lesson_vocabulary
                   WHERE lesson_id = '10000000-0000-4000-8000-000000000001'
                   AND vocabulary_id = '30000000-0000-4000-8000-000000000001'""".trimIndent(),
            ),
        )
    }

    @Test
    fun `knowledge graph queries navigate semantic relationships in both directions`() {
        migrate()
        importer().importPackage(samplePackage())
        val queries = JdbcKnowledgeGraphQueries(config())
        val sentenceId = UUID.fromString("50000000-0000-4000-8000-000000000001")
        val vocabularyId = UUID.fromString("30000000-0000-4000-8000-000000000004")
        val grammarId = UUID.fromString("40000000-0000-4000-8000-000000000002")

        assertTrue(queries.sentencesUsingVocabulary(vocabularyId).any { it.id == sentenceId })
        assertTrue(queries.sentencesUsingGrammar(grammarId).any { it.id == sentenceId })
        assertTrue(queries.vocabularyUsedBySentence(sentenceId).any { it.id == vocabularyId })
        assertTrue(queries.grammarUsedBySentence(sentenceId).any { it.id == grammarId })
        assertEquals(1, queries.lessonsFor(KnowledgeEntityType.VOCABULARY, vocabularyId).size)
        assertEquals("po", queries.vocabulary(vocabularyId)?.tagalog)
        assertEquals("Magandang umaga po.", queries.sentence(sentenceId)?.text)
        assertEquals("Magalang na po", queries.grammar(grammarId)?.name)
        assertEquals("Pagbati at pagpapakilala", queries.lesson(UUID.fromString("10000000-0000-4000-8000-000000000001"))?.name)
    }

    private fun migrate() = DatabaseManager(config()).migrate()
    private fun importer() = LessonPackageImporter(config())
    private fun config() = DatabaseConfig(temporaryDirectory.resolve("tagalog.db"))
    private fun samplePackage() = Path.of("examples/lesson-package").toAbsolutePath()

    private fun copySample(name: String): Path {
        val target = temporaryDirectory.resolve(name)
        Files.createDirectories(target)
        Files.list(samplePackage()).use { paths ->
            paths.filter(Files::isRegularFile).forEach { Files.copy(it, target.resolve(it.fileName)) }
        }
        return target
    }

    private fun count(table: String) = scalarInt("SELECT count(*) FROM $table")

    private fun scalarInt(sql: String): Int = DriverManager.getConnection(config().jdbcUrl).use { connection ->
        connection.createStatement().use { statement -> statement.executeQuery(sql).use { it.next(); it.getInt(1) } }
    }

    private fun scalarString(sql: String): String = DriverManager.getConnection(config().jdbcUrl).use { connection ->
        connection.createStatement().use { statement -> statement.executeQuery(sql).use { it.next(); it.getString(1) } }
    }
}
