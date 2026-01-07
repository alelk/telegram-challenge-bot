package io.github.alelk.apps.challengetgbot.domain

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Challenge entity representing a posted poll
 */
@OptIn(ExperimentalTime::class)
data class ChallengeEntity(
    val id: Long = 0,
    val groupName: String,
    val pollId: String,
    val messageId: Long,
    val chatId: Long,
    val questionText: String,
    val postedAt: Instant
)