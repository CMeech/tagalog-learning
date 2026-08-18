package ca.cashmclean.tagalog.application.lessonpackage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.UUID

class LessonPackageValidatorTest {
    @Test
    fun `canonical package is valid and all records are inserts against an empty database`() {
        val result = validator().validate(Path.of("examples/lesson-package"))

        assertTrue(result.isValid)
        assertEquals(11, result.inserts)
        assertEquals(0, result.unchanged)
        assertEquals(0, result.conflicts)
    }

    @Test
    fun `identical stored record is unchanged and changed UUID content is a conflict`() {
        val loaded = LessonPackageLoader().load(Path.of("examples/lesson-package"))
        val lesson = loaded.lesson
        val unchanged = validator(
            StoredLessonPackageSnapshot(lessons = listOf(StoredLesson(lesson.id, lesson.name, lesson.description))),
        ).validate(Path.of("examples/lesson-package"))
        assertEquals(1, unchanged.unchanged)

        val conflict = validator(
            StoredLessonPackageSnapshot(lessons = listOf(StoredLesson(lesson.id, "Changed", lesson.description))),
        ).validate(Path.of("examples/lesson-package"))
        assertFalse(conflict.isValid)
        assertEquals(1, conflict.conflicts)
        assertTrue(conflict.errors.any { it.message.contains("different normalized content") })
    }

    @Test
    fun `unresolved cross-package relationships are reported with actionable locations`() {
        val packagePath = Path.of("examples/lesson-package")
        val loaded = LessonPackageLoader().load(packagePath)
        val withoutVocabulary = loaded.copy(vocabulary = emptyList())

        val result = validator().validate(withoutVocabulary)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.filename == "sentences.csv" && it.column == "vocabulary_ids" && it.row == 1L })
    }

    @Test
    fun `duplicate content under another stored uuid and reused package uuid are errors`() {
        val candidate = LessonPackageLoader().load(Path.of("examples/lesson-package"))
        val duplicateLesson = StoredLesson(UUID.randomUUID(), candidate.lesson.name, candidate.lesson.description)
        val reused = candidate.copy(sources = listOf(candidate.sources.first().copy(id = candidate.lesson.id)))

        val result = validator(StoredLessonPackageSnapshot(lessons = listOf(duplicateLesson))).validate(reused)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.message.contains("duplicates record") })
        assertTrue(result.errors.count { it.message.contains("used more than once") } >= 2)
    }

    @Test
    fun `reusing global knowledge in another lesson or source is unchanged`() {
        val candidate = LessonPackageLoader().load(Path.of("examples/lesson-package"))
        val vocabulary = candidate.vocabulary.first()
        val stored = StoredVocabulary(
            id = vocabulary.id,
            tagalog = vocabulary.tagalog,
            english = vocabulary.english,
            rootWord = vocabulary.rootWord,
            partOfSpeech = vocabulary.partOfSpeech.name,
            difficulty = vocabulary.difficulty.name,
            frequencyRank = vocabulary.frequencyRank,
            lessonId = UUID.randomUUID(),
            sourceId = UUID.randomUUID(),
            tags = vocabulary.tags,
        )

        val result = validator(StoredLessonPackageSnapshot(vocabulary = listOf(stored))).validate(candidate)

        assertTrue(result.isValid)
        assertTrue(result.assessments.any {
            it.type == "vocabulary" && it.id == vocabulary.id && it.disposition == CandidateDisposition.UNCHANGED
        })
    }

    private fun validator(snapshot: StoredLessonPackageSnapshot = StoredLessonPackageSnapshot()) =
        LessonPackageValidator(LessonPackageLoader()) { snapshot }
}
