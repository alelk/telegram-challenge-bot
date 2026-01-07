package io.github.alelk.apps.challengetgbot.db

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private val logger = KotlinLogging.logger {}

/**
 * Database service for managing database connections and schema
 */
object DatabaseService {
    private lateinit var database: Database

    /**
     * Initialize database connection and create schema
     */
    fun init(databasePath: String) {
        logger.info { "Initializing database at: $databasePath" }

        database = Database.connect(
            url = "jdbc:h2:file:$databasePath;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
            driver = "org.h2.Driver"
        )

        transaction(database) {
            SchemaUtils.create(Challenges, PollAnswers, PollOptionConfigs)
            logger.info { "Database schema initialized" }
        }
    }

    /**
     * Initialize in-memory database for testing
     */
    fun initForTest() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_${System.currentTimeMillis()};DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver"
        )

        transaction(database) {
            SchemaUtils.create(Challenges, PollAnswers, PollOptionConfigs)
        }
    }

    /**
     * Drop all tables (for testing cleanup)
     */
    fun dropAllTables() {
        transaction(database) {
            SchemaUtils.drop(PollAnswers, PollOptionConfigs, Challenges)
        }
    }

    /**
     * Execute a database transaction
     */
    fun <T> query(block: () -> T): T {
        return transaction(database) {
            block()
        }
    }
}

