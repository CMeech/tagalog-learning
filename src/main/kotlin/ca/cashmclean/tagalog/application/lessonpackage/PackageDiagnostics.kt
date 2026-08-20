package ca.cashmclean.tagalog.application.lessonpackage

import java.util.UUID

internal fun packageDiagnostic(
    filename: String,
    row: Long?,
    column: String?,
    value: String?,
    message: String,
    guidance: String,
) = PackageDiagnostic(filename, row, column, PackageDiagnostic.safeValue(value), message, guidance)

internal fun domainDiagnostics(filename: String, row: Long?, column: String?, id: UUID, createDomainEntity: () -> Any): List<PackageDiagnostic> = try {
    createDomainEntity()
    emptyList()
} catch (exception: IllegalArgumentException) {
    listOf(packageDiagnostic(filename, row, column, id.toString(), exception.message ?: "Invalid domain value", "Correct the value to satisfy the domain rule."))
}

internal fun dataRow(index: Int): Long = (index + 1).toLong()
