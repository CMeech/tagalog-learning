package ca.cashmclean.tagalog.application.export

import java.util.UUID

data class LessonExport(
    val lessonId: UUID,
    val vocabulary: List<VocabularyExportProjection>,
    val sentences: List<SentenceExportProjection>,
    val grammar: List<GrammarExportProjection>,
)

fun interface LessonExportQueries {
    fun lessonExport(lessonId: UUID): LessonExport?
}
