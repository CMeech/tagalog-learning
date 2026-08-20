package ca.cashmclean.tagalog.application.lessonpackage

import ca.cashmclean.tagalog.domain.Difficulty
import ca.cashmclean.tagalog.domain.PartOfSpeech
import org.apache.commons.csv.CSVRecord

class VocabularyCsvParser : CsvRowParser<VocabularyCandidate> {
    override val filename = PackageFiles.VOCABULARY
    override val headers = "id,tagalog,english,root_word,part_of_speech,difficulty,frequency_rank,source_id,tags".split(',')

    override fun parse(record: CSVRecord) = VocabularyCandidate(
        id = PackageValueParser.uuid(record.normalized(0), location(record, "id")),
        tagalog = record.required(1, filename, "tagalog"),
        english = record.required(2, filename, "english"),
        rootWord = record.optional(3),
        partOfSpeech = PackageValueParser.enum<PartOfSpeech>(record.normalized(4), location(record, "part_of_speech")),
        difficulty = record.optional(5)?.let { PackageValueParser.enum<Difficulty>(it, location(record, "difficulty")) }
            ?: Difficulty.BEGINNER,
        frequencyRank = record.optional(6)?.let { PackageValueParser.positiveInteger(it, location(record, "frequency_rank")) },
        sourceId = record.optional(7)?.let { PackageValueParser.uuid(it, location(record, "source_id")) },
        tags = PackageValueParser.pipeSeparated(record.normalized(8), location(record, "tags")) { it },
    )

    private fun location(record: CSVRecord, column: String) = "$filename row ${record.recordNumber - 1} $column"
}
