package ca.cashmclean.tagalog.application.lessonpackage

import java.nio.file.Files
import java.nio.file.Path

class LessonPackageLoader(private val jsonParser: LessonJsonParser = LessonJsonParser()) {
    fun load(lessonFile: Path): LessonPackageCandidate {
        val result = read(lessonFile)
        if (result.diagnostics.isNotEmpty()) {
            throw LessonPackageException(result.diagnostics.first().message, diagnostics = result.diagnostics)
        }
        return requireNotNull(result.lessonPackage)
    }

    fun read(lessonFile: Path): LessonPackageReadResult = try {
        requireLessonFile(lessonFile)
        val size = Files.size(lessonFile)
        if (size > MAX_FILE_BYTES) PackageValueParser.fail("lesson.json exceeds the $MAX_FILE_BYTES byte limit")
        val bytes = Files.readAllBytes(lessonFile)
        PackageValueParser.rejectByteOrderMark(bytes, "lesson.json")
        val text = PackageValueParser.decodeUtf8(bytes, "lesson.json")
        LessonPackageReadResult(jsonParser.parse(text), emptyList())
    } catch (exception: LessonPackageException) {
        LessonPackageReadResult(
            lessonPackage = null,
            diagnostics = exception.diagnostics.ifEmpty {
                listOf(PackageDiagnostic("lesson.json", "$", null, exception.message ?: "Invalid lesson package", "Correct lesson.json and validate it again."))
            },
        )
    }

    private fun requireLessonFile(path: Path) {
        if (!Files.isRegularFile(path)) PackageValueParser.fail("lesson input must be a regular JSON file: $path")
        if (path.fileName.toString() != "lesson.json") PackageValueParser.fail("lesson input file must be named lesson.json")
    }

    companion object {
        const val MAX_FILE_BYTES = 26_214_400L
    }
}
