package io.github.alelk.apps.challengetgbot.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import io.github.alelk.apps.challengetgbot.config.AppConfig
import io.github.alelk.apps.challengetgbot.di.appModule
import io.github.alelk.apps.challengetgbot.telegram.TelegramBotService
import io.github.alelk.apps.challengetgbot.util.DateFormatter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * Command to post a single challenge manually
 */
class PostChallengeCommand : CliktCommand(name = "post"), KoinComponent {

    override fun help(context: Context): String = "Опубликовать челлендж вручную"

    val configFile: File by option("-c", "--config")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .help("Путь к файлу конфигурации")
        .default(File(System.getenv("CONFIG_PATH") ?: "config.yaml"))

    val groupName: String by argument("group")
        .help("Имя группы из конфигурации")

    val customQuestion: String? by option("-q", "--question")
        .help("Кастомный текст вопроса (если не указан, используется шаблон из конфига)")

    override fun run() {
        runBlocking {
            postChallenge()
        }
    }

    private suspend fun postChallenge() {
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

        try {
            // Get dependencies from Koin
            val config: AppConfig by inject()
            val telegramService: TelegramBotService by inject()

            val groupConfig = config.groups.find { it.name == groupName }
            if (groupConfig == null) {
                log.error { "Group '$groupName' not found in configuration" }
                log.info { "Available groups: ${config.groups.map { it.name }}" }
                return
            }

            log.info { "Posting challenge to group: ${groupConfig.name}" }

            // Determine question text
            val questionText = customQuestion ?: DateFormatter.formatQuestionFromConfig(groupConfig)
            log.info { "Question: $questionText" }

            // Post challenge
            val challenge = telegramService.postChallenge(groupConfig, questionText)

            if (challenge != null) {
                log.info { "Challenge posted successfully!" }
                log.info { "  Poll ID: ${challenge.pollId}" }
                log.info { "  Message ID: ${challenge.messageId}" }
                log.info { "  Chat ID: ${challenge.chatId}" }
            } else {
                log.error { "Failed to post challenge" }
            }
        } finally {
            stopKoin()
        }
    }
}

