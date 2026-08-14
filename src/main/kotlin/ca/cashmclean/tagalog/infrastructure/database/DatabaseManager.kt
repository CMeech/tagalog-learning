package ca.cashmclean.tagalog.infrastructure.database

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import java.nio.file.Files

class DatabaseManager(private val config: DatabaseConfig) {
    fun migrate(): Int {
        config.path.toAbsolutePath().parent?.let(Files::createDirectories)
        return flyway().migrate().migrationsExecuted
    }

    fun validate() {
        flyway().validate()
    }

    fun connect(): Database = Database.connect(config.jdbcUrl, driver = "org.sqlite.JDBC")

    private fun flyway(): Flyway = Flyway.configure()
        .dataSource(config.jdbcUrl, null, null)
        .load()
}
