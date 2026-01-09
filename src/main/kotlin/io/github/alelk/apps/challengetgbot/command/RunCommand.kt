package io.github.alelk.apps.challengetgbot.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import io.github.alelk.apps.challengetgbot.config.AppConfig
import io.github.alelk.apps.challengetgbot.di.appModule
import io.github.alelk.apps.challengetgbot.scheduler.ChallengeScheduler
import io.github.alelk.apps.challengetgbot.telegram.TelegramBotService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * Command to run the bot in daemon mode
 */
class RunCommand : CliktCommand(name = "run"), KoinComponent {

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

        // Initialize Koin DI
        try {
            startKoin {
                modules(appModule(configFile))
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to initialize application" }
            return
        }

        // Get dependencies from Koin
        val config: AppConfig by inject()
        val telegramService: TelegramBotService by inject()
        val scheduler: ChallengeScheduler by inject()

        log.info { "Configuration loaded. Managing ${config.groups.size} group(s)" }

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
            stopKoin()
            log.info { "Shutdown complete" }
        })

        log.info { "Telegram Challenge Bot is running. Press Ctrl+C to stop." }

        // Keep application running
        scope.coroutineContext[Job]?.join()
    }
}

