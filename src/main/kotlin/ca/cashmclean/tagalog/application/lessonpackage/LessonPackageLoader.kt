package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.Difficulty
import ca.cashmclean.tagalog.domain.PartOfSpeech
import ca.cashmclean.tagalog.domain.SourceType
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.text.Normalizer
import java.util.UUID

class LessonPackageException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

class LessonPackageLoader(
    schemaStream: () -> java.io.InputStream = {
        requireNotNull(LessonPackageLoader::class.java.getResourceAsStream("/lesson-package.schema.json"))
    },
) {
    private val objectMapper = ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
    private val manifestSchema = schemaStream().use { input ->
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
            .getSchema(objectMapper.readTree(input))
    }

    fun load(packageDirectory: Path): LessonPackageCandidate {
        if (!Files.isDirectory(packageDirectory)) fail("package must be a directory: $packageDirectory")

        val files = RECOGNIZED_FILES.associateWith(packageDirectory::resolve)
        val manifestPath = files.getValue(MANIFEST)
        if (!Files.isRegularFile(manifestPath)) fail("missing required file: $MANIFEST")

        val csvPaths = CSV_FILES.mapNotNull { name -> files.getValue(name).takeIf(Files::isRegularFile) }
        if (csvPaths.isEmpty()) fail("package must contain at least one recognized CSV file")
        enforceFileLimits(listOf(manifestPath) + csvPaths)

        val manifest = parseManifest(manifestPath)
        return LessonPackageCandidate(
            schemaVersion = 1,
            lesson = manifest.lesson,
            sources = manifest.sources,
            defaultSourceId = manifest.defaultSourceId,
            vocabulary = parseOptional(files.getValue(VOCABULARY), VOCABULARY_HEADERS, ::parseVocabulary)
                .map { it.copy(sourceId = it.sourceId ?: manifest.defaultSourceId) },
            sentences = parseOptional(files.getValue(SENTENCES), SENTENCE_HEADERS, ::parseSentence)
                .map { it.copy(sourceId = it.sourceId ?: manifest.defaultSourceId) },
            grammar = parseOptional(files.getValue(GRAMMAR), GRAMMAR_HEADERS, ::parseGrammar)
                .map { it.copy(sourceId = it.sourceId ?: manifest.defaultSourceId) },
        )
    }

    private data class ManifestCandidate(
        val lesson: LessonCandidate,
        val sources: List<SourceCandidate>,
        val defaultSourceId: UUID?,
    )

    private fun parseManifest(path: Path): ManifestCandidate {
        val bytes = Files.readAllBytes(path)
        rejectBom(bytes, MANIFEST)
        val text = decodeUtf8(bytes, MANIFEST)
        val root = try {
            objectMapper.readTree(text)
        } catch (exception: Exception) {
            throw LessonPackageException("$MANIFEST is not valid JSON", exception)
        }

        val version = root.get("schema_version")
        if (version == null || !version.isIntegralNumber || version.asInt() != 1) {
            fail("$MANIFEST schema_version must be the supported integer 1")
        }

        normalizeTextNodes(root)
        val schemaErrors = manifestSchema.validate(root)
        if (schemaErrors.isNotEmpty()) {
            fail("$MANIFEST does not match schema: ${schemaErrors.map { it.message }.sorted().joinToString("; ")}")
        }
        enforceStringLimits(root, MANIFEST)

        val lesson = root.get("lesson")
        val sources = root.get("sources").map { source ->
            SourceCandidate(
                id = uuid(source.text("id"), "$MANIFEST sources.id"),
                name = source.text("name"),
                type = enumValue<SourceType>(source.text("type"), "$MANIFEST sources.type"),
                reference = source.optionalText("reference"),
            )
        }
        return ManifestCandidate(
            lesson = LessonCandidate(
                id = uuid(lesson.text("id"), "$MANIFEST lesson.id"),
                name = lesson.text("name"),
                description = lesson.optionalText("description"),
            ),
            sources = sources,
            defaultSourceId = root.optionalText("default_source_id")?.let { uuid(it, "$MANIFEST default_source_id") },
        )
    }

    private fun <T> parseOptional(
        path: Path,
        expectedHeaders: List<String>,
        convert: (CSVRecord, String) -> T,
    ): List<T> = if (Files.isRegularFile(path)) parseCsv(path, expectedHeaders, convert) else emptyList()

    private fun <T> parseCsv(path: Path, expectedHeaders: List<String>, convert: (CSVRecord, String) -> T): List<T> {
        val bytes = Files.readAllBytes(path)
        rejectBom(bytes, path.fileName.toString())
        val reader = InputStreamReader(bytes.inputStream(), strictUtf8Decoder())
        try {
            CSVFormat.RFC4180.builder().get().parse(reader).use { parser ->
                val iterator = parser.iterator()
                if (!iterator.hasNext()) fail("${path.fileName} is empty")
                val headers = iterator.next().toList()
                if (headers != expectedHeaders) fail("${path.fileName} header does not match the package contract")

                val result = ArrayList<T>()
                while (iterator.hasNext()) {
                    if (result.size >= MAX_DATA_ROWS) fail("${path.fileName} exceeds $MAX_DATA_ROWS data rows")
                    val record = iterator.next()
                    if (record.size() != expectedHeaders.size) {
                        fail("${path.fileName} row ${record.recordNumber} has ${record.size()} fields; expected ${expectedHeaders.size}")
                    }
                    record.forEachIndexed { index, value ->
                        if (value.length > MAX_FIELD_CHARS) {
                            fail("${path.fileName} row ${record.recordNumber} field ${expectedHeaders[index]} exceeds $MAX_FIELD_CHARS characters")
                        }
                    }
                    result += convert(record, path.fileName.toString())
                }
                return result
            }
        } catch (exception: LessonPackageException) {
            throw exception
        } catch (exception: Exception) {
            throw LessonPackageException("${path.fileName} is not valid UTF-8 CSV", exception)
        }
    }

    private fun parseVocabulary(row: CSVRecord, filename: String) = VocabularyCandidate(
        id = uuid(row.scalar(0), "$filename row ${row.recordNumber} id"),
        tagalog = row.required(1, filename, "tagalog"),
        english = row.required(2, filename, "english"),
        rootWord = row.optional(3),
        partOfSpeech = enumValue(row.scalar(4), "$filename row ${row.recordNumber} part_of_speech"),
        difficulty = row.optional(5)?.let { enumValue(it, "$filename row ${row.recordNumber} difficulty") } ?: Difficulty.BEGINNER,
        frequencyRank = row.optional(6)?.let { positiveInteger(it, "$filename row ${row.recordNumber} frequency_rank") },
        sourceId = row.optional(7)?.let { uuid(it, "$filename row ${row.recordNumber} source_id") },
        tags = list(row.scalar(8), "$filename row ${row.recordNumber} tags") { it },
    )

    private fun parseSentence(row: CSVRecord, filename: String) = SentenceCandidate(
        id = uuid(row.scalar(0), "$filename row ${row.recordNumber} id"),
        text = row.required(1, filename, "text"),
        translation = row.required(2, filename, "translation"),
        difficulty = row.optional(3)?.let { enumValue(it, "$filename row ${row.recordNumber} difficulty") } ?: Difficulty.BEGINNER,
        sourceId = row.optional(4)?.let { uuid(it, "$filename row ${row.recordNumber} source_id") },
        vocabularyIds = list(row.scalar(5), "$filename row ${row.recordNumber} vocabulary_ids") { uuid(it, "vocabulary_ids") },
        grammarIds = list(row.scalar(6), "$filename row ${row.recordNumber} grammar_ids") { uuid(it, "grammar_ids") },
    )

    private fun parseGrammar(row: CSVRecord, filename: String) = GrammarCandidate(
        id = uuid(row.scalar(0), "$filename row ${row.recordNumber} id"),
        name = row.required(1, filename, "name"),
        description = row.required(2, filename, "description"),
        formula = row.required(3, filename, "formula"),
        sourceId = row.optional(4)?.let { uuid(it, "$filename row ${row.recordNumber} source_id") },
    )

    private fun enforceFileLimits(paths: List<Path>) {
        var total = 0L
        paths.forEach { path ->
            val size = Files.size(path)
            if (size > MAX_FILE_BYTES) fail("${path.fileName} exceeds the $MAX_FILE_BYTES byte limit")
            total += size
        }
        if (total > MAX_PACKAGE_BYTES) fail("recognized package files exceed the $MAX_PACKAGE_BYTES byte limit")
    }

    private fun normalizeTextNodes(node: JsonNode) {
        if (node.isObject) {
            node.fields().forEachRemaining { (_, child) ->
                if (!child.isTextual) normalizeTextNodes(child)
            }
            val objectNode = node as com.fasterxml.jackson.databind.node.ObjectNode
            objectNode.fields().asSequence().filter { it.value.isTextual }.toList().forEach { (key, value) ->
                objectNode.put(key, normalize(value.asText()))
            }
        } else if (node.isArray) {
            node.forEach(::normalizeTextNodes)
        }
    }

    private fun enforceStringLimits(node: JsonNode, location: String) {
        if (node.isTextual && node.asText().length > MAX_FIELD_CHARS) fail("$location contains a string exceeding $MAX_FIELD_CHARS characters")
        node.forEach { enforceStringLimits(it, location) }
    }

    private fun CSVRecord.scalar(index: Int): String = normalize(get(index))
    private fun CSVRecord.optional(index: Int): String? = scalar(index).ifBlank { null }
    private fun CSVRecord.required(index: Int, file: String, column: String): String =
        scalar(index).also { if (it.isBlank()) fail("$file row $recordNumber $column must not be blank") }

    private fun JsonNode.text(name: String): String = get(name).asText()
    private fun JsonNode.optionalText(name: String): String? = get(name)?.asText()?.ifBlank { null }

    private fun normalize(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFC)

    private fun uuid(value: String, location: String): UUID {
        if (!CANONICAL_UUID.matches(value)) fail("$location must be a lowercase canonical UUID")
        return try { UUID.fromString(value) } catch (_: IllegalArgumentException) { fail("$location must be a valid UUID") }
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String, location: String): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fail("$location has unsupported value '$value'")

    private fun positiveInteger(value: String, location: String): Int {
        if (!POSITIVE_INTEGER.matches(value)) fail("$location must be a positive base-10 integer")
        return value.toIntOrNull()?.takeIf { it > 0 } ?: fail("$location is outside the supported integer range")
    }

    private fun <T> list(value: String, location: String, convert: (String) -> T): Set<T> {
        val normalized = normalize(value)
        if (normalized.isBlank()) return emptySet()
        val items = normalized.split('|').map(::normalize)
        if (items.any(String::isBlank)) fail("$location contains a blank list item")
        val converted = items.map(convert)
        if (converted.toSet().size != converted.size) fail("$location contains a duplicate list item")
        return converted.toSet()
    }

    private fun decodeUtf8(bytes: ByteArray, filename: String): String = try {
        strictUtf8Decoder().decode(ByteBuffer.wrap(bytes)).toString()
    } catch (exception: Exception) {
        throw LessonPackageException("$filename is not valid UTF-8", exception)
    }

    private fun strictUtf8Decoder() = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)

    private fun rejectBom(bytes: ByteArray, filename: String) {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            fail("$filename must not contain a UTF-8 byte-order mark")
        }
    }

    companion object {
        const val MAX_FILE_BYTES = 10_485_760L
        const val MAX_PACKAGE_BYTES = 26_214_400L
        const val MAX_DATA_ROWS = 100_000
        const val MAX_FIELD_CHARS = 1_048_576

        private const val MANIFEST = "lesson.json"
        private const val VOCABULARY = "vocabulary.csv"
        private const val SENTENCES = "sentences.csv"
        private const val GRAMMAR = "grammar.csv"
        private val CSV_FILES = listOf(VOCABULARY, SENTENCES, GRAMMAR)
        private val RECOGNIZED_FILES = listOf(MANIFEST) + CSV_FILES
        private val CANONICAL_UUID = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        private val POSITIVE_INTEGER = Regex("^[0-9]+$")

        private val VOCABULARY_HEADERS = "id,tagalog,english,root_word,part_of_speech,difficulty,frequency_rank,source_id,tags".split(',')
        private val SENTENCE_HEADERS = "id,text,translation,difficulty,source_id,vocabulary_ids,grammar_ids".split(',')
        private val GRAMMAR_HEADERS = "id,name,description,formula,source_id".split(',')

        private fun fail(message: String): Nothing = throw LessonPackageException(message)
    }
}
