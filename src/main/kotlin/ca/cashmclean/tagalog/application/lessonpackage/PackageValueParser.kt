package ca.cashmclean.tagalog.application.lessonpackage

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.UUID

internal object PackageValueParser {
    private val canonicalUuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

    fun normalize(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFC)

    fun uuid(value: String, location: String): UUID {
        if (!canonicalUuid.matches(value)) fail("$location must be a lowercase canonical UUID")
        return try {
            UUID.fromString(value)
        } catch (_: IllegalArgumentException) {
            fail("$location must be a valid UUID")
        }
    }

    inline fun <reified T : Enum<T>> enum(value: String, location: String): T =
        enumValues<T>().firstOrNull { it.name == value }
            ?: fail("$location has unsupported value '$value'")

    fun decodeUtf8(bytes: ByteArray, filename: String): String = try {
        strictUtf8Decoder().decode(ByteBuffer.wrap(bytes)).toString()
    } catch (exception: Exception) {
        throw LessonPackageException("$filename is not valid UTF-8", exception)
    }

    fun strictUtf8Decoder() = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)

    fun rejectByteOrderMark(bytes: ByteArray, filename: String) {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            fail("$filename must not contain a UTF-8 byte-order mark")
        }
    }

    fun fail(message: String): Nothing = throw LessonPackageException(message)
}
