package io.github.alelk.apps.challengetgbot.domain

/**
 * Configuration for a poll option
 */
data class PollOptionConfig(
    val points: Int,
    val countsAsCompleted: Boolean
)

