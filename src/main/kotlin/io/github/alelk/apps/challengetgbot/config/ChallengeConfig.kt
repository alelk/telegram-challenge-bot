package io.github.alelk.apps.challengetgbot.config

/**
 * Configuration for the challenge itself
 */
data class ChallengeConfig(
    val questionTemplate: String,
    val options: List<PollOption>,
    val isAnonymous: Boolean = false,
    val allowsMultipleAnswers: Boolean = false
)