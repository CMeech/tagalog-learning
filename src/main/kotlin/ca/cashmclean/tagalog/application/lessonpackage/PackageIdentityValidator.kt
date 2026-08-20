package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

internal object PackageIdentityValidator {
    private data class LocatedId(val id: UUID, val file: String, val row: Long?, val column: String)

    fun validate(candidate: LessonPackageCandidate, errors: MutableList<PackageDiagnostic>) {
        val ids = buildList {
            add(LocatedId(candidate.lesson.id, "lesson.json", null, "lesson.id"))
            candidate.sources.forEachIndexed { index, it -> add(LocatedId(it.id, "lesson.json", (index + 1).toLong(), "sources.id")) }
            candidate.vocabulary.forEachIndexed { index, it -> add(LocatedId(it.id, "vocabulary.csv", (index + 1).toLong(), "id")) }
            candidate.sentences.forEachIndexed { index, it -> add(LocatedId(it.id, "sentences.csv", (index + 1).toLong(), "id")) }
            candidate.grammar.forEachIndexed { index, it -> add(LocatedId(it.id, "grammar.csv", (index + 1).toLong(), "id")) }
        }
        ids.groupBy { it.id }.filterValues { it.size > 1 }.values.flatten().forEach { located ->
            errors += packageDiagnostic(
                located.file, located.row, located.column, located.id.toString(),
                "UUID is used more than once in this package.",
                "Give each record its own stable UUID and update relationships accordingly.",
            )
        }
        candidate.defaultSourceId?.takeIf { default -> candidate.sources.none { it.id == default } }?.let { default ->
            errors += packageDiagnostic(
                "lesson.json", null, "default_source_id", default.toString(),
                "Default source is not defined in this lesson metadata.",
                "Add the source to sources or remove default_source_id.",
            )
        }
    }
}
