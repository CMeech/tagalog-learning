package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.Difficulty
import org.apache.commons.csv.CSVRecord

class SentenceCsvParser : CsvRowParser<SentenceCandidate> {
    override val filename = PackageFiles.SENTENCES
    override val headers = "id,text,translation,difficulty,source_id,vocabulary_ids,grammar_ids".split(',')

    override fun parse(record: CSVRecord) = SentenceCandidate(
        id = PackageValueParser.uuid(record.normalized(0), location(record, "id")),
        text = record.required(1, filename, "text"),
        translation = record.required(2, filename, "translation"),
        difficulty = record.optional(3)?.let { PackageValueParser.enum<Difficulty>(it, location(record, "difficulty")) }
            ?: Difficulty.BEGINNER,
        sourceId = record.optional(4)?.let { PackageValueParser.uuid(it, location(record, "source_id")) },
        vocabularyIds = PackageValueParser.pipeSeparated(record.normalized(5), location(record, "vocabulary_ids")) {
            PackageValueParser.uuid(it, "vocabulary_ids")
        },
        grammarIds = PackageValueParser.pipeSeparated(record.normalized(6), location(record, "grammar_ids")) {
            PackageValueParser.uuid(it, "grammar_ids")
        },
    )

    private fun location(record: CSVRecord, column: String) = "$filename row ${record.recordNumber - 1} $column"
}
