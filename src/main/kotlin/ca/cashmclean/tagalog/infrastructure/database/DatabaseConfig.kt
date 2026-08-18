package ca.cashmclean.tagalog.infrastructure.database

import java.nio.file.Path
import kotlin.io.path.absolute

data class DatabaseConfig(val path: Path) {
    val jdbcUrl: String = "jdbc:sqlite:${path.absolute()}?foreign_keys=on"
    val readOnlyJdbcUrl: String = "jdbc:sqlite:file:${path.absolute()}?mode=ro&foreign_keys=on"

    companion object {
        private const val DATABASE_PATH_ENV = "TAGALOG_DB_PATH"
        private const val DATABASE_PATH_PROPERTY = "tagalog.db.path"

        fun fromEnvironment(environment: Map<String, String> = System.getenv()): DatabaseConfig =
            DatabaseConfig(
                Path.of(
                    System.getProperty(DATABASE_PATH_PROPERTY)
                        ?: environment[DATABASE_PATH_ENV]
                        ?: "data/tagalog.db",
                ),
            )
    }
}
