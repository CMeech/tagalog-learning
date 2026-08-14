package ca.cashmclean.tagalog.cli

import ca.cashmclean.tagalog.infrastructure.database.DatabaseConfig
import ca.cashmclean.tagalog.infrastructure.database.DatabaseManager
import java.util.concurrent.Callable

abstract class DatabaseCommand : Callable<Int> {
    protected val database: DatabaseManager by lazy {
        DatabaseManager(DatabaseConfig.fromEnvironment())
    }
}
