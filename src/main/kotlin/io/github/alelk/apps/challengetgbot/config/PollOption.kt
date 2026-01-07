package io.github.alelk.apps.challengetgbot.config

/**
 * Single poll option configuration
 */
data class PollOption(
    val text: String,
    val points: Int,
    val countsAsCompleted: Boolean
)