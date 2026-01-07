package io.github.alelk.apps.challengetgbot.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.utils.TelegramAPIUrlsKeeper
import io.github.alelk.apps.challengetgbot.config.AppConfig
import io.github.alelk.apps.challengetgbot.db.DatabaseService
import io.github.alelk.apps.challengetgbot.repository.ChallengeRepository
import io.github.alelk.apps.challengetgbot.scheduler.ChallengeScheduler
import io.github.alelk.apps.challengetgbot.telegram.TelegramBotService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.*
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * Command to run the bot in daemon mode
 */
class RunCommand : CliktCommand(name = "run") {

    override fun help(context: Context): String = "Запустить бот в режиме демона"

    val configFile: File by option("-c", "--config")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .help("Путь к файлу конфигурации")
        .default(File(System.getenv("CONFIG_PATH") ?: "config.yaml"))

    override fun run() {
        runBlocking {
            runBot()
        }
    }

    private suspend fun runBot() {
        log.info { "Starting Telegram Challenge Bot..." }
        log.info { "Loading configuration from: ${configFile.absolutePath}" }

        val config = try {
            ConfigLoaderBuilder.default()
                .addFileSource(configFile)
                .build()
                .loadConfigOrThrow<AppConfig>()
        } catch (e: Exception) {
            log.error(e) { "Failed to load configuration from ${configFile.absolutePath}" }
            return
        }

        log.info { "Configuration loaded. Managing ${config.groups.size} group(s)" }

        // Initialize database
        DatabaseService.init(config.databasePath)

        // Initialize repository
        val repository = ChallengeRepository()

        // Initialize Telegram bot with custom timeout for long polling
        // Long polling uses 25-30 second waits, so we need a longer request timeout
        val bot = telegramBot(TelegramAPIUrlsKeeper(config.botToken)) {
            client = HttpClient(CIO) {
                engine {
                    requestTimeout = 65_000 // 65 seconds to handle long polling
                }
            }
        }
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
            log.info { "Starting scheduler for group: ${groupConfig.name}" }
            scheduler.startScheduling(scope, groupConfig)
        }

        // Setup shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            log.info { "Shutting down..." }
            scheduler.stopScheduling()
            scope.cancel()
            log.info { "Shutdown complete" }
        })

        log.info { "Telegram Challenge Bot is running. Press Ctrl+C to stop." }

        // Keep application running
        scope.coroutineContext[Job]?.join()
    }
}

