package io.github.alelk.apps.challengetgbot.domain

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Poll answer entity representing a user's answer to a poll
 */
@OptIn(ExperimentalTime::class)
data class PollAnswerEntity(
    val challengeId: Long,
    val userId: Long,
    val userName: String?,
    val userFirstName: String?,
    val userLastName: String?,
    val optionIds: List<Int>,
    val points: Int,
    val isCompleted: Boolean,
    val answeredAt: Instant
)