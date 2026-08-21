package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

internal fun packageDiagnostic(
    path: String?,
    value: String?,
    message: String,
    guidance: String,
) = PackageDiagnostic("lesson.json", path, PackageDiagnostic.safeValue(value), message, guidance)

internal fun domainDiagnostics(path: String, id: UUID, createDomainEntity: () -> Any): List<PackageDiagnostic> = try {
    createDomainEntity()
    emptyList()
} catch (exception: IllegalArgumentException) {
    listOf(packageDiagnostic(path, id.toString(), exception.message ?: "Invalid domain value", "Correct the value to satisfy the domain rule."))
}
