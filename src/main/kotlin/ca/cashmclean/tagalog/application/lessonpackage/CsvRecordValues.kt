package ca.cashmclean.tagalog.application.lessonpackage

import org.apache.commons.csv.CSVRecord

internal fun CSVRecord.normalized(index: Int): String = PackageValueParser.normalize(get(index))

internal fun CSVRecord.optional(index: Int): String? = normalized(index).ifBlank { null }

internal fun CSVRecord.required(index: Int, filename: String, column: String): String =
    normalized(index).also {
        if (it.isBlank()) PackageValueParser.fail("$filename row ${recordNumber - 1} $column must not be blank")
    }
