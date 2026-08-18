package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

internal object PackageRelationshipValidator {
    fun validate(
        candidate: LessonPackageCandidate,
        snapshot: StoredLessonPackageSnapshot,
        errors: MutableList<PackageDiagnostic>,
    ) {
        val sourceIds = candidate.sources.mapTo(mutableSetOf()) { it.id }.apply { addAll(snapshot.sources.map { it.id }) }
        val vocabularyIds = candidate.vocabulary.mapTo(mutableSetOf()) { it.id }.apply { addAll(snapshot.vocabulary.map { it.id }) }
        val grammarIds = candidate.grammar.mapTo(mutableSetOf()) { it.id }.apply { addAll(snapshot.grammar.map { it.id }) }

        candidate.vocabulary.forEachIndexed { index, item -> checkSource(item.sourceId, "vocabulary.csv", index, sourceIds, errors) }
        candidate.grammar.forEachIndexed { index, item -> checkSource(item.sourceId, "grammar.csv", index, sourceIds, errors) }
        candidate.sentences.forEachIndexed { index, item ->
            val row = (index + 1).toLong()
            checkSource(item.sourceId, "sentences.csv", index, sourceIds, errors)
            checkReferences(item.vocabularyIds, vocabularyIds, "vocabulary_ids", "Vocabulary", row, errors)
            checkReferences(item.grammarIds, grammarIds, "grammar_ids", "Grammar", row, errors)
        }
    }

    private fun checkSource(
        sourceId: UUID?,
        file: String,
        index: Int,
        knownIds: Set<UUID>,
        errors: MutableList<PackageDiagnostic>,
    ) {
        if (sourceId != null && sourceId !in knownIds) {
            errors += packageDiagnostic(
                file, (index + 1).toLong(), "source_id", sourceId.toString(),
                "Source UUID does not exist.",
                "Define this source in lesson.json or use an existing stored source UUID.",
            )
        }
    }

    private fun checkReferences(
        supplied: Set<UUID>,
        known: Set<UUID>,
        column: String,
        type: String,
        row: Long,
        errors: MutableList<PackageDiagnostic>,
    ) {
        supplied.filterNot(known::contains).forEach { id ->
            errors += packageDiagnostic(
                "sentences.csv", row, column, id.toString(), "$type UUID does not exist.",
                "Define it in this package or reference a stored ${type.lowercase()} UUID.",
            )
        }
    }
}
