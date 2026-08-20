package ca.cashmclean.tagalog.application.lessonpackage

data class PackageDiagnostic(
    val filename: String,
    val path: String? = null,
    val value: String? = null,
    val message: String,
    val guidance: String,
) {
    companion object {
        fun safeValue(value: String?): String? = value?.let {
            val escaped = it.replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t")
            if (escaped.length <= 80) escaped else escaped.take(77) + "..."
        }
    }
}
