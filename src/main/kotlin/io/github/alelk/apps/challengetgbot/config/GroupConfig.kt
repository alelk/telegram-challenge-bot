package io.github.alelk.apps.challengetgbot.config

/**
 * Configuration for a single Telegram group
 */
data class GroupConfig(
    val chatId: Long,
    val threadId: Long? = null,
    val name: String,
    val challenge: ChallengeConfig,
    val schedule: ScheduleConfig,
    val report: ReportConfig
)