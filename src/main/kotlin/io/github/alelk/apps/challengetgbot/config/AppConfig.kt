package io.github.alelk.apps.challengetgbot.config

/**
 * Root configuration for the Telegram Challenge Bot
 */
data class AppConfig(
    val botToken: String,
    val databasePath: String = "./challenge-bot.db",
    val groups: List<GroupConfig>
)

