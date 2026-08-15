package ca.cashmclean.tagalog.cli

import ca.cashmclean.tagalog.application.LearningCollectionService
import ca.cashmclean.tagalog.infrastructure.database.DatabaseConfig
import ca.cashmclean.tagalog.infrastructure.database.DatabaseManager
import java.util.concurrent.Callable

abstract class DatabaseCommand : Callable<Int> {
    protected val database: DatabaseManager by lazy {
        DatabaseManager(DatabaseConfig.fromEnvironment())
    }

    protected val learningCollection: LearningCollectionService by lazy {
        LearningCollectionService(database)
    }
}
