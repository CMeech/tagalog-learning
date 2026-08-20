package ca.cashmclean.tagalog.application.lessonpackage

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path

class LessonCsvReader {
    fun <T> read(path: Path, rowParser: CsvRowParser<T>, diagnostics: MutableList<PackageDiagnostic>): List<T> {
        val bytes = Files.readAllBytes(path)
        PackageValueParser.rejectByteOrderMark(bytes, rowParser.filename)
        val reader = InputStreamReader(bytes.inputStream(), PackageValueParser.strictUtf8Decoder())
        try {
            CSVFormat.RFC4180.builder().get().parse(reader).use { csv ->
                val records = csv.iterator()
                if (!records.hasNext()) PackageValueParser.fail("${rowParser.filename} is empty")
                val suppliedHeaders = records.next().toList()
                if (suppliedHeaders != rowParser.headers) {
                    diagnostics += headerDiagnostics(rowParser.filename, suppliedHeaders, rowParser.headers)
                    return emptyList()
                }
                return readRows(records, rowParser, diagnostics)
            }
        } catch (exception: LessonPackageException) {
            throw exception
        } catch (exception: Exception) {
            throw LessonPackageException("${rowParser.filename} is not valid UTF-8 CSV", exception)
        }
    }

    private fun <T> readRows(
        records: Iterator<CSVRecord>,
        rowParser: CsvRowParser<T>,
        diagnostics: MutableList<PackageDiagnostic>,
    ): List<T> = buildList {
        while (records.hasNext()) {
            if (size >= MAX_DATA_ROWS) PackageValueParser.fail("${rowParser.filename} exceeds $MAX_DATA_ROWS data rows")
            val record = records.next()
            if (!hasExpectedFieldCount(record, rowParser, diagnostics)) continue
            if (hasOversizedField(record, rowParser, diagnostics)) continue
            try {
                add(rowParser.parse(record))
            } catch (exception: LessonPackageException) {
                diagnostics += rowDiagnostic(rowParser, record, exception)
            }
        }
    }

    private fun <T> hasExpectedFieldCount(
        record: CSVRecord,
        rowParser: CsvRowParser<T>,
        diagnostics: MutableList<PackageDiagnostic>,
    ): Boolean {
        if (record.size() == rowParser.headers.size) return true
        diagnostics += PackageDiagnostic(
            filename = rowParser.filename,
            row = record.recordNumber - 1,
            message = "${rowParser.filename} row ${record.recordNumber - 1} has ${record.size()} fields; expected ${rowParser.headers.size}",
            guidance = "Add or remove fields so the row matches the complete header.",
        )
        return false
    }

    private fun <T> hasOversizedField(
        record: CSVRecord,
        rowParser: CsvRowParser<T>,
        diagnostics: MutableList<PackageDiagnostic>,
    ): Boolean {
        var oversized = false
        record.forEachIndexed { index, value ->
            if (value.length > PackageValueParser.MAX_FIELD_CHARS) {
                val column = rowParser.headers[index]
                diagnostics += PackageDiagnostic(
                    filename = rowParser.filename,
                    row = record.recordNumber - 1,
                    column = column,
                    message = "${rowParser.filename} row ${record.recordNumber - 1} field $column exceeds ${PackageValueParser.MAX_FIELD_CHARS} characters",
                    guidance = "Shorten this field to the documented maximum.",
                )
                oversized = true
            }
        }
        return oversized
    }

    private fun <T> rowDiagnostic(
        rowParser: CsvRowParser<T>,
        record: CSVRecord,
        exception: LessonPackageException,
    ): PackageDiagnostic {
        val column = rowParser.headers.firstOrNull { exception.message?.contains(it) == true }
        val index = column?.let(rowParser.headers::indexOf)?.takeIf { it in 0 until record.size() }
        return PackageDiagnostic(
            filename = rowParser.filename,
            row = record.recordNumber - 1,
            column = column,
            value = PackageDiagnostic.safeValue(index?.let(record::get)),
            message = exception.message ?: "Invalid row value.",
            guidance = "Correct this field to match the documented package contract.",
        )
    }

    private fun headerDiagnostics(filename: String, actual: List<String>, expected: List<String>): List<PackageDiagnostic> {
        val duplicates = actual.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        val supplied = actual.toSet()
        val missing = expected.filterNot(supplied::contains)
        val extra = actual.filterNot(expected.toSet()::contains)
        return buildList {
            duplicates.forEach { add(headerDiagnostic(filename, it, "Duplicate column '$it'.", "Include each documented column exactly once.")) }
            missing.forEach { header ->
                val caseVariant = actual.firstOrNull { it.equals(header, ignoreCase = true) }
                if (caseVariant != null) {
                    add(headerDiagnostic(filename, caseVariant, "Column '$caseVariant' has incorrect case.", "Rename it to '$header'."))
                } else {
                    add(headerDiagnostic(filename, header, "Missing column '$header'.", "Add '$header' in the documented position."))
                }
            }
            extra.filterNot { suppliedHeader -> expected.any { it.equals(suppliedHeader, ignoreCase = true) } }
                .forEach { add(headerDiagnostic(filename, it, "Unexpected column '$it'.", "Remove this column.")) }
            if (actual.size == expected.size && actual.toSet() == expected.toSet() && actual != expected) {
                add(headerDiagnostic(filename, null, "Columns are in the wrong order.", "Use: ${expected.joinToString(",")}"))
            }
        }
    }

    private fun headerDiagnostic(filename: String, value: String?, message: String, guidance: String) = PackageDiagnostic(
        filename = filename,
        row = 0,
        column = value,
        value = PackageDiagnostic.safeValue(value),
        message = message,
        guidance = guidance,
    )

    companion object {
        const val MAX_DATA_ROWS = 100_000
    }
}
