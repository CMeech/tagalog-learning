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

        candidate.vocabulary.forEachIndexed { index, item -> checkSource(item.sourceId, "$.vocabulary[$index].source_id", sourceIds, errors) }
        candidate.grammar.forEachIndexed { index, item -> checkSource(item.sourceId, "$.grammar[$index].source_id", sourceIds, errors) }
        candidate.sentences.forEachIndexed { index, item ->
            checkSource(item.sourceId, "$.sentences[$index].source_id", sourceIds, errors)
            checkReferences(item.vocabularyIds, vocabularyIds, "$.sentences[$index].vocabulary_ids", "Vocabulary", errors)
            checkReferences(item.grammarIds, grammarIds, "$.sentences[$index].grammar_ids", "Grammar", errors)
        }
    }

    private fun checkSource(
        sourceId: UUID?,
        path: String,
        knownIds: Set<UUID>,
        errors: MutableList<PackageDiagnostic>,
    ) {
        if (sourceId != null && sourceId !in knownIds) {
            errors += packageDiagnostic(
                path, sourceId.toString(),
                "Source UUID does not exist.",
                "Define this source in lesson.json or use an existing stored source UUID.",
            )
        }
    }

    private fun checkReferences(
        supplied: Set<UUID>,
        known: Set<UUID>,
        path: String,
        type: String,
        errors: MutableList<PackageDiagnostic>,
    ) {
        supplied.forEachIndexed { index, id -> if (id !in known) {
            errors += packageDiagnostic(
                "$path[$index]", id.toString(), "$type UUID does not exist.",
                "Define it in this package or reference a stored ${type.lowercase()} UUID.",
            )
        } }
    }
}
