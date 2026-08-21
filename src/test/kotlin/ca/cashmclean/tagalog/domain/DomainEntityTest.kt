package ca.cashmclean.tagalog.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class DomainEntityTest {
    private val id = UUID.randomUUID()

    @Test
    fun `Valid entities retain their values`() {
        val vocabulary = Vocabulary(
            id = id,
            tagalog = "kumain",
            englishMeaning = "to eat",
            rootWord = "kain",
            partOfSpeech = PartOfSpeech.VERB,
            difficulty = Difficulty.BEGINNER,
            frequencyRank = 42,
        )

        assertEquals("kumain", vocabulary.tagalog)
        assertEquals("kain", vocabulary.rootWord)
        assertEquals(42, vocabulary.frequencyRank)
    }

    @Test
    fun `Required text fields reject blank values`() {
        val invalidObjects = listOf<() -> Any>(
            { validVocabulary(tagalog = " ") },
            { validVocabulary(englishMeaning = "\t") },
            { Sentence(id, "", "How are you?", Difficulty.BEGINNER) },
            { Sentence(id, "Kumusta ka?", "\n", Difficulty.BEGINNER) },
            { GrammarConcept(id, "", "description", "formula") },
            { GrammarConcept(id, "Focus", " ", "formula") },
            { GrammarConcept(id, "Focus", "description", " ") },
            { Lesson(id, "") },
            { Source(id, " ", SourceType.TEACHER) },
            { Tag(id, "\t") },
        )

        invalidObjects.forEach { create ->
            assertThrows(IllegalArgumentException::class.java) { create() }
        }
    }

    @Test
    fun `Optional text rejects blank values when present`() {
        assertThrows(IllegalArgumentException::class.java) { validVocabulary(rootWord = " ") }
        assertThrows(IllegalArgumentException::class.java) { Lesson(id, "Lesson 1", " ") }
        assertThrows(IllegalArgumentException::class.java) {
            Source(id, "Pimsleur", SourceType.COURSE, " ")
        }
    }

    @Test
    fun `Frequency rank must be positive when present`() {
        assertThrows(IllegalArgumentException::class.java) { validVocabulary(frequencyRank = 0) }
        assertThrows(IllegalArgumentException::class.java) { validVocabulary(frequencyRank = -1) }
    }

    @Test
    fun `Optional vocabulary metadata may be absent`() {
        val vocabulary = validVocabulary()

        assertEquals(null, vocabulary.rootWord)
        assertEquals(null, vocabulary.frequencyRank)
    }

    private fun validVocabulary(
        tagalog: String = "salamat",
        englishMeaning: String = "thank you",
        rootWord: String? = null,
        frequencyRank: Int? = null,
    ) = Vocabulary(
        id = id,
        tagalog = tagalog,
        englishMeaning = englishMeaning,
        rootWord = rootWord,
        partOfSpeech = PartOfSpeech.PHRASE,
        difficulty = Difficulty.BEGINNER,
        frequencyRank = frequencyRank,
    )
}
