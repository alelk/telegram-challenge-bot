package io.github.alelk.apps.challengetgbot.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import io.github.alelk.apps.challengetgbot.config.AppConfig
import io.github.alelk.apps.challengetgbot.db.DatabaseService
import io.github.alelk.apps.challengetgbot.repository.ChallengeRepository
import io.github.alelk.apps.challengetgbot.telegram.TelegramBotService
import io.github.alelk.apps.challengetgbot.util.DateFormatter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * Command to post a single challenge manually
 */
class PostChallengeCommand : CliktCommand(name = "post") {

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

        val config = try {
            ConfigLoaderBuilder.default()
                .addFileSource(configFile)
                .build()
                .loadConfigOrThrow<AppConfig>()
        } catch (e: Exception) {
            log.error(e) { "Failed to load configuration from ${configFile.absolutePath}" }
            return
        }

        val groupConfig = config.groups.find { it.name == groupName }
        if (groupConfig == null) {
            log.error { "Group '$groupName' not found in configuration" }
            log.info { "Available groups: ${config.groups.map { it.name }}" }
            return
        }

        log.info { "Posting challenge to group: ${groupConfig.name}" }

        // Initialize database
        DatabaseService.init(config.databasePath)

        // Initialize repository
        val repository = ChallengeRepository()

        // Initialize Telegram bot
        val bot = telegramBot(config.botToken)
        val telegramService = TelegramBotService(bot, repository)

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
    }
}

