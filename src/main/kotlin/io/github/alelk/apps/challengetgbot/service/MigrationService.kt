package io.github.alelk.apps.challengetgbot.service

import io.github.alelk.apps.challengetgbot.config.AppConfig
import io.github.alelk.apps.challengetgbot.repository.ChallengeRepository
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Service for handling database migrations, such as group name changes
 */
class MigrationService(
    private val appConfig: AppConfig,
    private val repository: ChallengeRepository
) {

    /**
     * Run all migrations.
     * This should be called at application startup.
     */
    fun runMigrations() {
        migrateGroupNames()
    }

    /**
     * Migrate group names for groups that have oldName specified in config.
     * Updates all database records from oldName to the new name.
     */
    private fun migrateGroupNames() {
        appConfig.groups.forEach { group ->
            val oldName = group.oldName
            if (!oldName.isNullOrBlank() && oldName != group.name) {
                logger.info { "Migrating group name from '$oldName' to '${group.name}'..." }
                val updatedCount = repository.migrateGroupName(oldName, group.name)
                if (updatedCount > 0) {
                    logger.info { "Successfully migrated $updatedCount challenge(s) from '$oldName' to '${group.name}'" }
                } else {
                    logger.info { "No challenges found with old group name '$oldName'" }
                }
            }
        }
    }
}

