package ca.cashmclean.tagalog.infrastructure.export.anki

import ca.cashmclean.tagalog.application.export.ExportAssociation
import ca.cashmclean.tagalog.application.export.ExportReference
import ca.cashmclean.tagalog.application.export.GrammarExportProjection
import ca.cashmclean.tagalog.application.export.SentenceExportProjection
import ca.cashmclean.tagalog.application.export.VocabularyExportProjection
import ca.cashmclean.tagalog.domain.Difficulty
import ca.cashmclean.tagalog.domain.PartOfSpeech
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class AnkiExportersTest {
    private val association = ExportAssociation(
        "Pagbati at pagpapakilala",
        "Usapang Tagalog, Aralin 1",
        "Kabanata 1, pahina 3–5",
    )

    @Test
    fun `Vocabulary renderer matches the canonical fixture`() {
        val items = listOf(
            vocabulary("0004", "po", "politeness marker", PartOfSpeech.PARTICLE, listOf("paggalang")),
            vocabulary("0002", "ako", "I", PartOfSpeech.PRONOUN, listOf("panghalip")),
            vocabulary("0001", "magandang umaga", "good morning", PartOfSpeech.PHRASE, listOf("pagbati", "oras")),
            vocabulary("0003", "ikaw", "you", PartOfSpeech.PRONOUN, listOf("panghalip")),
        )

        assertEquals(fixture("vocabulary.tsv"), AnkiVocabularyExporter().let { it.render(it.export(items)) })
    }

    @Test
    fun `Sentence renderer matches the canonical fixture and orders relationships by UUID`() {
        val vocab = mapOf("0001" to "magandang umaga", "0002" to "ako", "0003" to "ikaw", "0004" to "po")
        val grammar = mapOf("0001" to "Pangungusap na di-karaniwan", "0002" to "Magalang na po")
        fun refs(prefix: String, values: Map<String, String>, vararg ids: String) =
            ids.map { ExportReference(id(prefix, it), values.getValue(it)) }
        val items = listOf(
            SentenceExportProjection(id("50000000", "0003"), "Ikaw po si José?", "Are you José?", Difficulty.BEGINNER,
                refs("30000000", vocab, "0004", "0003"), refs("40000000", grammar, "0002", "0001"), association),
            SentenceExportProjection(id("50000000", "0001"), "Magandang umaga po.", "Good morning.", Difficulty.BEGINNER,
                refs("30000000", vocab, "0004", "0001"), refs("40000000", grammar, "0002"), association),
            SentenceExportProjection(id("50000000", "0002"), "Ako si María.", "I am María.", Difficulty.BEGINNER,
                refs("30000000", vocab, "0002"), refs("40000000", grammar, "0001"), association),
        )

        assertEquals(fixture("sentences.tsv"), AnkiSentenceExporter().let { it.render(it.export(items)) })
    }

    @Test
    fun `Grammar renderer matches the canonical fixture`() {
        val items = listOf(
            GrammarExportProjection(id("40000000", "0002"), "Magalang na po", "Adds po to show respect to the listener.",
                "Pahayag + po", listOf(
                    ExportReference(id("50000000", "0003"), "Ikaw po si José?"),
                    ExportReference(id("50000000", "0001"), "Magandang umaga po."),
                ), association),
            GrammarExportProjection(id("40000000", "0001"), "Pangungusap na di-karaniwan",
                "Introduces a predicate before the topic, linked by ay in the inverted form.",
                "Panaguri + ang/si + Paksa", listOf(
                    ExportReference(id("50000000", "0002"), "Ako si María."),
                    ExportReference(id("50000000", "0003"), "Ikaw po si José?"),
                ), association),
        )

        assertEquals(fixture("grammar.tsv"), AnkiGrammarExporter().let { it.render(it.export(items)) })
    }

    @Test
    fun `Renderer quotes tabs, quotes, and all newline forms while leaving HTML as plain text`() {
        val item = vocabulary("0001", "<b>sabi</b>\t\"niya\"", "line 1\r\nline 2", PartOfSpeech.PHRASE, emptyList())
        val rendered = AnkiVocabularyExporter().let { it.render(it.export(listOf(item))) }

        assert(rendered.contains("\"<b>sabi</b>\t\"\"niya\"\"\""))
        assert(rendered.contains("\"line 1\r\nline 2\""))
        assert(rendered.contains("#html:false"))
    }

    private fun vocabulary(suffix: String, tagalog: String, english: String, part: PartOfSpeech, tags: List<String>) =
        VocabularyExportProjection(id("30000000", suffix), tagalog, english, null, part, Difficulty.BEGINNER, tags, association)

    private fun id(prefix: String, suffix: String): UUID = UUID.fromString("$prefix-0000-4000-8000-00000000$suffix")

    private fun fixture(name: String): String = Files.readString(
        Path.of("examples", "lesson-package", "expected-anki", name),
    )
}
