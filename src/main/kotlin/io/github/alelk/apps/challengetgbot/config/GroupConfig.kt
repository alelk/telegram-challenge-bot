package io.github.alelk.apps.challengetgbot.config

/**
 * Configuration for a single Telegram group
 *
 * @param oldName If the group was renamed, specify the old name here to migrate database records
 */
data class GroupConfig(
    val chatId: Long,
    val threadId: Long? = null,
    val name: String,
    val oldName: String? = null,
    val challenge: ChallengeConfig,
    val schedule: ScheduleConfig,
    val report: ReportConfig
)