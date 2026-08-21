package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.Difficulty
import ca.cashmclean.tagalog.domain.PartOfSpeech
import ca.cashmclean.tagalog.domain.SourceType
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import java.io.InputStream

class LessonJsonParser(
    schemaStream: () -> InputStream = {
        requireNotNull(LessonJsonParser::class.java.getResourceAsStream("/lesson-package.schema.json"))
    },
) {
    private val objectMapper = ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
    private val schema = schemaStream().use { input ->
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(objectMapper.readTree(input))
    }

    fun parse(text: String): LessonPackageCandidate {
        val root = readJson(text)
        requireSupportedVersion(root)
        normalizeText(root)
        val diagnostics = schema.validate(root).map { error ->
            PackageDiagnostic(
                filename = "lesson.json",
                path = jsonPath(error.instanceLocation.toString()),
                message = error.message,
                guidance = "Correct this value to match the version 2 lesson schema.",
            )
        } + duplicateArrayDiagnostics(root)
        if (diagnostics.isNotEmpty()) {
            throw LessonPackageException("lesson.json does not match the version 2 schema", diagnostics = diagnostics.distinct())
        }
        return toCandidate(root)
    }

    private fun readJson(text: String): JsonNode = try {
        objectMapper.readTree(text) ?: PackageValueParser.fail("lesson.json must contain a JSON object")
    } catch (exception: LessonPackageException) {
        throw exception
    } catch (exception: Exception) {
        throw LessonPackageException("lesson.json is not valid JSON", exception)
    }

    private fun requireSupportedVersion(root: JsonNode) {
        val version = root.get("schema_version")
        if (version == null || !version.isIntegralNumber || version.asInt() != 2) {
            throw LessonPackageException(
                "lesson.json schema_version must be the supported integer 2",
                diagnostics = listOf(PackageDiagnostic("lesson.json", "$.schema_version", version?.toString(), "schema_version must be the supported integer 2", "Set schema_version to 2.")),
            )
        }
    }

    private fun toCandidate(root: JsonNode): LessonPackageCandidate {
        val defaultSourceId = root.optionalText("default_source_id")?.let { PackageValueParser.uuid(it, "$.default_source_id") }
        return LessonPackageCandidate(
            schemaVersion = 2,
            lesson = root.get("lesson").let { lesson ->
                LessonCandidate(PackageValueParser.uuid(lesson.text("id"), "$.lesson.id"), lesson.text("name"), lesson.optionalText("description"))
            },
            sources = root.get("sources").mapIndexed { index, source ->
                SourceCandidate(
                    PackageValueParser.uuid(source.text("id"), "$.sources[$index].id"), source.text("name"),
                    PackageValueParser.enum<SourceType>(source.text("type"), "$.sources[$index].type"), source.optionalText("reference"),
                )
            },
            defaultSourceId = defaultSourceId,
            vocabulary = root.get("vocabulary").mapIndexed { index, word ->
                VocabularyCandidate(
                    id = PackageValueParser.uuid(word.text("id"), "$.vocabulary[$index].id"),
                    tagalog = word.text("tagalog"), english = word.text("english"), rootWord = word.optionalText("root_word"),
                    partOfSpeech = PackageValueParser.enum<PartOfSpeech>(word.text("part_of_speech"), "$.vocabulary[$index].part_of_speech"),
                    difficulty = word.optionalText("difficulty")?.let { PackageValueParser.enum<Difficulty>(it, "$.vocabulary[$index].difficulty") } ?: Difficulty.BEGINNER,
                    frequencyRank = word.get("frequency_rank")?.asInt(),
                    sourceId = word.optionalText("source_id")?.let { PackageValueParser.uuid(it, "$.vocabulary[$index].source_id") } ?: defaultSourceId,
                    tags = word.get("tags").map { it.asText() }.toSet(),
                )
            },
            sentences = root.get("sentences").mapIndexed { index, sentence ->
                SentenceCandidate(
                    id = PackageValueParser.uuid(sentence.text("id"), "$.sentences[$index].id"),
                    text = sentence.text("text"), translation = sentence.text("translation"),
                    difficulty = sentence.optionalText("difficulty")?.let { PackageValueParser.enum<Difficulty>(it, "$.sentences[$index].difficulty") } ?: Difficulty.BEGINNER,
                    sourceId = sentence.optionalText("source_id")?.let { PackageValueParser.uuid(it, "$.sentences[$index].source_id") } ?: defaultSourceId,
                    vocabularyIds = sentence.uuidSet("vocabulary_ids", "$.sentences[$index].vocabulary_ids"),
                    grammarIds = sentence.uuidSet("grammar_ids", "$.sentences[$index].grammar_ids"),
                )
            },
            grammar = root.get("grammar").mapIndexed { index, grammar ->
                GrammarCandidate(
                    PackageValueParser.uuid(grammar.text("id"), "$.grammar[$index].id"), grammar.text("name"),
                    grammar.text("description"), grammar.text("formula"),
                    grammar.optionalText("source_id")?.let { PackageValueParser.uuid(it, "$.grammar[$index].source_id") } ?: defaultSourceId,
                )
            },
        )
    }

    private fun duplicateArrayDiagnostics(root: JsonNode): List<PackageDiagnostic> = buildList {
        root.get("vocabulary")?.forEachIndexed { index, node -> duplicates(node.get("tags"), "$.vocabulary[$index].tags").forEach(::add) }
        root.get("sentences")?.forEachIndexed { index, node ->
            duplicates(node.get("vocabulary_ids"), "$.sentences[$index].vocabulary_ids").forEach(::add)
            duplicates(node.get("grammar_ids"), "$.sentences[$index].grammar_ids").forEach(::add)
        }
    }

    private fun duplicates(array: JsonNode?, path: String): List<PackageDiagnostic> {
        if (array == null || !array.isArray) return emptyList()
        val values = array.map { it.asText() }
        val repeated = values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        return values.mapIndexedNotNull { index, value ->
            value.takeIf(repeated::contains)?.let {
                PackageDiagnostic("lesson.json", "$path[$index]", PackageDiagnostic.safeValue(value), "Array item is duplicated.", "Remove the duplicate item.")
            }
        }
    }

    private fun normalizeText(node: JsonNode) {
        when {
            node.isObject -> {
                node.fields().forEachRemaining { (_, child) -> if (!child.isTextual) normalizeText(child) }
                val objectNode = node as com.fasterxml.jackson.databind.node.ObjectNode
                objectNode.fields().asSequence().filter { it.value.isTextual }.toList().forEach { (key, value) ->
                    objectNode.put(key, PackageValueParser.normalize(value.asText()))
                }
            }
            node.isArray -> node.forEach(::normalizeText)
        }
    }

    private fun JsonNode.text(name: String): String = get(name).asText()
    private fun JsonNode.optionalText(name: String): String? = get(name)?.asText()?.ifBlank { null }
    private fun JsonNode.uuidSet(name: String, path: String) = get(name).mapIndexed { index, node -> PackageValueParser.uuid(node.asText(), "$path[$index]") }.toSet()

    private fun jsonPath(location: String): String = when {
        location.isBlank() || location == "/" -> "$"
        location.startsWith("$") -> location
        else -> "$" + location.split('/').drop(1).joinToString("") { part ->
            part.toIntOrNull()?.let { "[$it]" } ?: ".$part"
        }
    }
}
