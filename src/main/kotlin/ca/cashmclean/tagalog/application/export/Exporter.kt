package ca.cashmclean.tagalog.application.export

fun interface Exporter<in T> {
    fun export(items: Iterable<T>): ExportDocument
}
