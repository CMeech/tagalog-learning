package ca.cashmclean.tagalog.application.lessonpackage

import java.nio.file.Files
import java.nio.file.Path

class LessonPackageLoader(
    private val metadataParser: LessonMetadataParser = LessonMetadataParser(),
    private val csvReader: LessonCsvReader = LessonCsvReader(),
    private val vocabularyParser: VocabularyCsvParser = VocabularyCsvParser(),
    private val sentenceParser: SentenceCsvParser = SentenceCsvParser(),
    private val grammarParser: GrammarCsvParser = GrammarCsvParser(),
) {
    fun load(packageDirectory: Path): LessonPackageCandidate {
        val result = read(packageDirectory)
        if (result.diagnostics.isNotEmpty()) {
            throw LessonPackageException(result.diagnostics.first().message, diagnostics = result.diagnostics)
        }
        return requireNotNull(result.lessonPackage)
    }

    fun read(packageDirectory: Path): LessonPackageReadResult {
        val diagnostics = mutableListOf<PackageDiagnostic>()
        return try {
            LessonPackageReadResult(readPackage(packageDirectory, diagnostics), diagnostics)
        } catch (exception: LessonPackageException) {
            LessonPackageReadResult(
                lessonPackage = null,
                diagnostics = diagnostics + exception.diagnostics.ifEmpty {
                    listOf(
                        PackageDiagnostic(
                            filename = filenameFrom(exception.message),
                            message = exception.message ?: "Invalid lesson package",
                            guidance = "Correct the package input and validate it again.",
                        ),
                    )
                },
            )
        }
    }

    private fun readPackage(
        packageDirectory: Path,
        diagnostics: MutableList<PackageDiagnostic>,
    ): LessonPackageCandidate {
        requirePackageDirectory(packageDirectory)
        val paths = PackageFiles.recognized.associateWith(packageDirectory::resolve)
        val metadataPath = paths.getValue(PackageFiles.METADATA)
        if (!Files.isRegularFile(metadataPath)) PackageValueParser.fail("missing required file: ${PackageFiles.METADATA}")

        val availableCsvFiles = PackageFiles.csvFiles.mapNotNull { paths.getValue(it).takeIf(Files::isRegularFile) }
        if (availableCsvFiles.isEmpty()) PackageValueParser.fail("package must contain at least one recognized CSV file")
        enforceFileLimits(listOf(metadataPath) + availableCsvFiles)

        val metadata = metadataParser.parse(metadataPath)
        return LessonPackageCandidate(
            schemaVersion = 1,
            lesson = metadata.lesson,
            sources = metadata.sources,
            defaultSourceId = metadata.defaultSourceId,
            vocabulary = readOptional(paths.getValue(PackageFiles.VOCABULARY), vocabularyParser, diagnostics)
                .map { it.copy(sourceId = it.sourceId ?: metadata.defaultSourceId) },
            sentences = readOptional(paths.getValue(PackageFiles.SENTENCES), sentenceParser, diagnostics)
                .map { it.copy(sourceId = it.sourceId ?: metadata.defaultSourceId) },
            grammar = readOptional(paths.getValue(PackageFiles.GRAMMAR), grammarParser, diagnostics)
                .map { it.copy(sourceId = it.sourceId ?: metadata.defaultSourceId) },
        )
    }

    private fun requirePackageDirectory(path: Path) {
        if (!Files.isDirectory(path)) PackageValueParser.fail("package must be a directory: $path")
    }

    private fun <T> readOptional(
        path: Path,
        parser: CsvRowParser<T>,
        diagnostics: MutableList<PackageDiagnostic>,
    ): List<T> = if (Files.isRegularFile(path)) csvReader.read(path, parser, diagnostics) else emptyList()

    private fun enforceFileLimits(paths: List<Path>) {
        var packageSize = 0L
        paths.forEach { path ->
            val fileSize = Files.size(path)
            if (fileSize > MAX_FILE_BYTES) PackageValueParser.fail("${path.fileName} exceeds the $MAX_FILE_BYTES byte limit")
            packageSize += fileSize
        }
        if (packageSize > MAX_PACKAGE_BYTES) PackageValueParser.fail("recognized package files exceed the $MAX_PACKAGE_BYTES byte limit")
    }

    private fun filenameFrom(message: String?): String =
        PackageFiles.recognized.firstOrNull { message?.contains(it) == true } ?: "package"

    companion object {
        const val MAX_FILE_BYTES = 10_485_760L
        const val MAX_PACKAGE_BYTES = 26_214_400L
        const val MAX_DATA_ROWS = LessonCsvReader.MAX_DATA_ROWS
        const val MAX_FIELD_CHARS = PackageValueParser.MAX_FIELD_CHARS
    }
}
