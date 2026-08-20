package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.SourceType
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class LessonMetadataParser(
    schemaStream: () -> InputStream = {
        requireNotNull(LessonMetadataParser::class.java.getResourceAsStream("/lesson-package.schema.json"))
    },
) {
    private val objectMapper = ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
    private val schema = schemaStream().use { input ->
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
            .getSchema(objectMapper.readTree(input))
    }

    fun parse(path: Path): LessonMetadata {
        val bytes = Files.readAllBytes(path)
        PackageValueParser.rejectByteOrderMark(bytes, PackageFiles.METADATA)
        val root = readJson(PackageValueParser.decodeUtf8(bytes, PackageFiles.METADATA))
        requireSupportedSchemaVersion(root)
        normalizeText(root)
        val schemaErrors = schema.validate(root)
        if (schemaErrors.isNotEmpty()) {
            PackageValueParser.fail("${PackageFiles.METADATA} does not match schema: ${schemaErrors.map { it.message }.sorted().joinToString("; ")}")
        }
        enforceStringLimits(root)
        return toMetadata(root)
    }

    private fun readJson(text: String): JsonNode = try {
        objectMapper.readTree(text)
    } catch (exception: Exception) {
        throw LessonPackageException("${PackageFiles.METADATA} is not valid JSON", exception)
    }

    private fun requireSupportedSchemaVersion(root: JsonNode) {
        val version = root.get("schema_version")
        if (version == null || !version.isIntegralNumber || version.asInt() != 1) {
            PackageValueParser.fail("${PackageFiles.METADATA} schema_version must be the supported integer 1")
        }
    }

    private fun toMetadata(root: JsonNode): LessonMetadata {
        val lesson = root.get("lesson")
        return LessonMetadata(
            lesson = LessonCandidate(
                id = PackageValueParser.uuid(lesson.text("id"), "${PackageFiles.METADATA} lesson.id"),
                name = lesson.text("name"),
                description = lesson.optionalText("description"),
            ),
            sources = root.get("sources").map { source ->
                SourceCandidate(
                    id = PackageValueParser.uuid(source.text("id"), "${PackageFiles.METADATA} sources.id"),
                    name = source.text("name"),
                    type = PackageValueParser.enum<SourceType>(source.text("type"), "${PackageFiles.METADATA} sources.type"),
                    reference = source.optionalText("reference"),
                )
            },
            defaultSourceId = root.optionalText("default_source_id")?.let {
                PackageValueParser.uuid(it, "${PackageFiles.METADATA} default_source_id")
            },
        )
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

    private fun enforceStringLimits(node: JsonNode) {
        if (node.isTextual && node.asText().length > PackageValueParser.MAX_FIELD_CHARS) {
            PackageValueParser.fail("${PackageFiles.METADATA} contains a string exceeding ${PackageValueParser.MAX_FIELD_CHARS} characters")
        }
        node.forEach(::enforceStringLimits)
    }

    private fun JsonNode.text(name: String): String = get(name).asText()
    private fun JsonNode.optionalText(name: String): String? = get(name)?.asText()?.ifBlank { null }
}
