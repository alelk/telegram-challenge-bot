package io.github.alelk.apps.challengetgbot.db

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

/**
 * Stores poll answers from users
 */
object PollAnswers : LongIdTable("poll_answers") {
    val challengeId = reference("challenge_id", Challenges)
    val userId = long("user_id")
    val userName = varchar("user_name", 255).nullable()
    val userFirstName = varchar("user_first_name", 255).nullable()
    val userLastName = varchar("user_last_name", 255).nullable()
    val optionIds = varchar("option_ids", 1000) // Comma-separated list of option IDs
    val points = integer("points").default(0)
    val isCompleted = bool("is_completed").default(false)
    @OptIn(ExperimentalTime::class)
    val answeredAt = timestamp("answered_at")

    init {
        uniqueIndex(challengeId, userId)
    }
}