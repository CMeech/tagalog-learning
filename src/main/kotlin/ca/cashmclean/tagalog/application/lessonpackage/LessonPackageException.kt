package ca.cashmclean.tagalog.application.lessonpackage

class LessonPackageException(
    message: String,
    cause: Throwable? = null,
    val diagnostics: List<PackageDiagnostic> = emptyList(),
) : IllegalArgumentException(message, cause)
