package ca.cashmclean.tagalog.infrastructure.export.anki

import ca.cashmclean.tagalog.application.export.Exporter
import ca.cashmclean.tagalog.domain.Difficulty
import ca.cashmclean.tagalog.domain.PartOfSpeech
import ca.cashmclean.tagalog.domain.Vocabulary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class AnkiVocabularyExporterTest {
    private val exporter = AnkiVocabularyExporter()

    @Test
    fun `exporter interface transforms vocabulary into a stable tabular model`() {
        val id = UUID.fromString("c6fabcd5-86ca-4a15-bf21-e45bb450ae52")
        val vocabulary = Vocabulary(
            id = id,
            tagalog = "kumain",
            englishMeaning = "to eat",
            rootWord = "kain",
            partOfSpeech = PartOfSpeech.VERB,
            difficulty = Difficulty.BEGINNER,
            frequencyRank = 42,
        )
        val interfaceReference: Exporter<Vocabulary> = exporter

        val document = interfaceReference.export(listOf(vocabulary))

        assertEquals(
            listOf("ID", "Tagalog", "English", "Root Word", "Part of Speech", "Difficulty", "Frequency Rank"),
            document.columns,
        )
        assertEquals(
            listOf(id.toString(), "kumain", "to eat", "kain", "VERB", "BEGINNER", "42"),
            document.rows.single().values,
        )
    }

    @Test
    fun `renderer emits Anki headers and escapes text fields`() {
        val vocabulary = Vocabulary(
            id = UUID.fromString("c6fabcd5-86ca-4a15-bf21-e45bb450ae52"),
            tagalog = "sabi \"niya\"",
            englishMeaning = "he said\nthat",
            partOfSpeech = PartOfSpeech.PHRASE,
            difficulty = Difficulty.INTERMEDIATE,
        )

        val text = exporter.render(exporter.export(listOf(vocabulary)))

        assertTrue(text.startsWith("#separator:Tab\n#html:false\n#columns:ID\tTagalog"))
        assertTrue(text.contains("\"sabi \"\"niya\"\"\""))
        assertTrue(text.contains("\"he said\nthat\""))
    }
}
