package ca.cashmclean.tagalog.application.lessonpackage

data class LessonPackageReadResult(
    val lessonPackage: LessonPackageCandidate?,
    val diagnostics: List<PackageDiagnostic>,
)
