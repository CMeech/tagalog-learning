package ca.cashmclean.tagalog.application.lessonpackage

import org.apache.commons.csv.CSVRecord

interface CsvRowParser<T> {
    val filename: String
    val headers: List<String>
    fun parse(record: CSVRecord): T
}
