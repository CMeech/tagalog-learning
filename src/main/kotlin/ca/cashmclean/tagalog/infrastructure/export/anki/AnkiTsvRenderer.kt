package ca.cashmclean.tagalog.infrastructure.export.anki

import ca.cashmclean.tagalog.application.export.ExportDocument

internal class AnkiTsvRenderer(
    private val noteType: String,
    private val tagsColumn: Int? = null,
) {
    fun render(document: ExportDocument): String = buildString {
        appendLine("#separator:Tab")
        appendLine("#html:false")
        appendLine("#notetype:$noteType")
        tagsColumn?.let { appendLine("#tags column:$it") }
        append("#columns:")
        appendLine(document.columns.joinToString("\t", transform = ::escape))
        document.rows.forEach { row -> appendLine(row.values.joinToString("\t", transform = ::escape)) }
    }

    private fun escape(value: String): String {
        if (value.none { it == '\t' || it == '\r' || it == '\n' || it == '"' }) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }
}
