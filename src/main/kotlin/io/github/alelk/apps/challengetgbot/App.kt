package io.github.alelk.apps.challengetgbot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import io.github.alelk.apps.challengetgbot.config.AppConfig
import io.github.alelk.apps.challengetgbot.db.DatabaseService
import io.github.alelk.apps.challengetgbot.repository.ChallengeRepository
import io.github.alelk.apps.challengetgbot.scheduler.ChallengeScheduler
import io.github.alelk.apps.challengetgbot.telegram.TelegramBotService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Main application entry point
 */
suspend fun main(args: Array<String>) {
    logger.info { "Starting Telegram Challenge Bot..." }

    // Load configuration
    val configPath = args.firstOrNull() ?: System.getenv("CONFIG_PATH") ?: "config.yaml"
    logger.info { "Loading configuration from: $configPath" }

    val config = try {
        ConfigLoaderBuilder.default()
            .addFileSource(File(configPath))
            .build()
            .loadConfigOrThrow<AppConfig>()
    } catch (e: Exception) {
        logger.error(e) { "Failed to load configuration from $configPath" }
        return
    }

    logger.info { "Configuration loaded. Managing ${config.groups.size} group(s)" }

    // Initialize database
    DatabaseService.init(config.databasePath)

    // Initialize repository
    val repository = ChallengeRepository()

    // Initialize Telegram bot
    val bot = telegramBot(config.botToken)
    val telegramService = TelegramBotService(bot, repository)

    // Initialize scheduler
    val scheduler = ChallengeScheduler(telegramService, repository)

    // Create coroutine scope
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Start bot
    scope.launch {
        telegramService.start(scope)
    }

    // Start scheduling for each group
    config.groups.forEach { groupConfig ->
        logger.info { "Starting scheduler for group: ${groupConfig.name}" }
        scheduler.startScheduling(scope, groupConfig)
    }

    // Setup shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info { "Shutting down..." }
        scheduler.stopScheduling()
        scope.cancel()
        logger.info { "Shutdown complete" }
    })

    logger.info { "Telegram Challenge Bot is running. Press Ctrl+C to stop." }

    // Keep application running
    scope.coroutineContext[Job]?.join()
}

