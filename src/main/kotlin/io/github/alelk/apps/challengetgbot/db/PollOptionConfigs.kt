package io.github.alelk.apps.challengetgbot.db

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/**
 * Stores configuration for poll options per group
 */
object PollOptionConfigs : LongIdTable("poll_option_configs") {
    val challengeId = reference("challenge_id", Challenges)
    val optionIndex = integer("option_index")
    val optionText = text("option_text")
    val points = integer("points")
    val countsAsCompleted = bool("counts_as_completed")

    init {
        uniqueIndex(challengeId, optionIndex)
    }
}

