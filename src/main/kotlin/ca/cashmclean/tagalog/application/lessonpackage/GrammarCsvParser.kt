package ca.cashmclean.tagalog.application.lessonpackage

import org.apache.commons.csv.CSVRecord

class GrammarCsvParser : CsvRowParser<GrammarCandidate> {
    override val filename = PackageFiles.GRAMMAR
    override val headers = "id,name,description,formula,source_id".split(',')

    override fun parse(record: CSVRecord) = GrammarCandidate(
        id = PackageValueParser.uuid(record.normalized(0), location(record, "id")),
        name = record.required(1, filename, "name"),
        description = record.required(2, filename, "description"),
        formula = record.required(3, filename, "formula"),
        sourceId = record.optional(4)?.let { PackageValueParser.uuid(it, location(record, "source_id")) },
    )

    private fun location(record: CSVRecord, column: String) = "$filename row ${record.recordNumber - 1} $column"
}
