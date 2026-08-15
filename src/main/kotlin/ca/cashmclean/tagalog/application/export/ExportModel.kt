package ca.cashmclean.tagalog.application.export

data class ExportDocument(
    val columns: List<String>,
    val rows: List<ExportRow>,
) {
    init {
        require(columns.isNotEmpty()) { "columns must not be empty" }
        require(columns.all { it.isNotBlank() }) { "column names must not be blank" }
        require(columns.distinct().size == columns.size) { "column names must be unique" }
        require(rows.all { it.values.size == columns.size }) {
            "every row must contain exactly ${columns.size} values"
        }
    }
}

data class ExportRow(val values: List<String>)
