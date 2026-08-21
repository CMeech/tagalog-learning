package ca.cashmclean.tagalog.application.lessonpackage

internal object PackageIdentityValidator {
    fun validate(candidate: LessonPackageCandidate, errors: MutableList<PackageDiagnostic>) {
        val ids = buildList {
            add(LocatedPackageId(candidate.lesson.id, "$.lesson.id"))
            candidate.sources.forEachIndexed { index, it -> add(LocatedPackageId(it.id, "$.sources[$index].id")) }
            candidate.vocabulary.forEachIndexed { index, it -> add(LocatedPackageId(it.id, "$.vocabulary[$index].id")) }
            candidate.sentences.forEachIndexed { index, it -> add(LocatedPackageId(it.id, "$.sentences[$index].id")) }
            candidate.grammar.forEachIndexed { index, it -> add(LocatedPackageId(it.id, "$.grammar[$index].id")) }
        }
        ids.groupBy { it.id }.filterValues { it.size > 1 }.values.flatten().forEach { located ->
            errors += packageDiagnostic(
                located.path, located.id.toString(),
                "UUID is used more than once in this package.",
                "Give each record its own stable UUID and update relationships accordingly.",
            )
        }
        candidate.defaultSourceId?.takeIf { default -> candidate.sources.none { it.id == default } }?.let { default ->
            errors += packageDiagnostic(
                "$.default_source_id", default.toString(),
                "Default source is not defined in this lesson document.",
                "Add the source to sources or remove default_source_id.",
            )
        }
    }
}
