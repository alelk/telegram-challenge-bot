package io.github.alelk.apps.challengetgbot.db

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

/**
 * Stores information about posted challenges
 */
object Challenges : LongIdTable("challenges") {
    val groupName = varchar("group_name", 255)
    val pollId = varchar("poll_id", 255).uniqueIndex()
    val messageId = long("message_id")
    val chatId = long("chat_id")
    val questionText = text("question_text")
    @OptIn(ExperimentalTime::class)
    val postedAt = timestamp("posted_at")
}