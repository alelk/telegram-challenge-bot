package io.github.alelk.apps.challengetgbot.di

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.utils.TelegramAPIUrlsKeeper
import io.github.alelk.apps.challengetgbot.config.AppConfig
import io.github.alelk.apps.challengetgbot.db.DatabaseService
import io.github.alelk.apps.challengetgbot.repository.ChallengeRepository
import io.github.alelk.apps.challengetgbot.scheduler.ChallengeScheduler
import io.github.alelk.apps.challengetgbot.telegram.TelegramBotService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.io.File

/**
 * Koin module for dependency injection
 */
fun appModule(configFile: File) = module {
    // Configuration
    single<AppConfig> {
        ConfigLoaderBuilder.default()
            .addFileSource(configFile)
            .build()
            .loadConfigOrThrow<AppConfig>()
    }

    // Database
    single<DatabaseService> {
        val config = get<AppConfig>()
        DatabaseService(config.databasePath)
    }

    // Repository
    singleOf(::ChallengeRepository)

    // Telegram Bot
    single<TelegramBot> {
        val config = get<AppConfig>()
        telegramBot(TelegramAPIUrlsKeeper(config.botToken)) {
            client = HttpClient(CIO) {
                engine {
                    requestTimeout = 65_000 // 65 seconds for long polling
                }
            }
        }
    }

    // Telegram Service
    singleOf(::TelegramBotService)

    // Scheduler
    singleOf(::ChallengeScheduler)
}

/**
 * Koin module for testing with in-memory database
 */
fun testModule() = module {
    single<DatabaseService> { DatabaseService.createForTest() }
    singleOf(::ChallengeRepository)
}

