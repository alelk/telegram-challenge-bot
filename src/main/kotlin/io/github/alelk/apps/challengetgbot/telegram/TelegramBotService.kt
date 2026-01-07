package io.github.alelk.apps.challengetgbot.telegram

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.polls.sendRegularPoll
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onPollAnswer
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageThreadId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.polls.InputPollOption
import dev.inmo.tgbotapi.types.polls.PollAnswer
import io.github.alelk.apps.challengetgbot.config.GroupConfig
import io.github.alelk.apps.challengetgbot.config.SortBy
import io.github.alelk.apps.challengetgbot.domain.ChallengeEntity
import io.github.alelk.apps.challengetgbot.domain.PollAnswerEntity
import io.github.alelk.apps.challengetgbot.domain.UserStatistics
import io.github.alelk.apps.challengetgbot.repository.ChallengeRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

/**
 * Service for interacting with Telegram Bot API
 */
@OptIn(ExperimentalTime::class)
class TelegramBotService(
    private val bot: TelegramBot,
    private val repository: ChallengeRepository
) {

    /**
     * Start the bot and listen for poll answers
     */
    suspend fun start(scope: CoroutineScope) {
        bot.buildBehaviourWithLongPolling(scope = scope) {
            logger.info { "Bot started successfully" }

            onPollAnswer { pollAnswer ->
                handlePollAnswer(pollAnswer)
            }
        }
    }

    /**
     * Post a challenge poll to a group
     */
    suspend fun postChallenge(groupConfig: GroupConfig, questionText: String): ChallengeEntity? {
        return try {
            val chatId = ChatId(RawChatId(groupConfig.chatId))
            val options = groupConfig.challenge.options.map { InputPollOption(it.text) }

            val message = bot.sendRegularPoll(
                chatId = chatId,
                question = questionText,
                options = options,
                isAnonymous = groupConfig.challenge.isAnonymous,
                allowMultipleAnswers = groupConfig.challenge.allowsMultipleAnswers,
                threadId = groupConfig.threadId?.let { MessageThreadId(it) }
            )

            // Extract poll from the message content
            val pollContent = message.content
            val poll = pollContent.poll

            val challenge = ChallengeEntity(
                groupName = groupConfig.name,
                pollId = poll.id.string,
                messageId = message.messageId.long,
                chatId = groupConfig.chatId,
                questionText = questionText,
                postedAt = Clock.System.now()
            )

            val challengeId = repository.saveChallenge(challenge)

            // Save poll option configs with their indices
            groupConfig.challenge.options.forEachIndexed { index, option ->
                repository.savePollOptionConfig(
                    challengeId = challengeId,
                    optionIndex = index,
                    optionText = option.text,
                    points = option.points,
                    countsAsCompleted = option.countsAsCompleted
                )
            }

            logger.info { "Posted challenge for group '${groupConfig.name}' with poll ID: ${poll.id}" }
            challenge.copy(id = challengeId)
        } catch (e: Exception) {
            logger.error(e) { "Failed to post challenge for group '${groupConfig.name}'" }
            null
        }
    }

    /**
     * Send report to a group
     */
    suspend fun sendReport(groupConfig: GroupConfig, statistics: List<UserStatistics>) {
        try {
            val reportText = formatReport(groupConfig, statistics)

            bot.sendTextMessage(
                chatId = ChatId(RawChatId(groupConfig.chatId)),
                text = reportText,
                threadId = groupConfig.threadId?.let { MessageThreadId(it) }
            )

            logger.info { "Sent report to group '${groupConfig.name}'" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send report to group '${groupConfig.name}'" }
        }
    }

    /**
     * Handle poll answer from a user.
     * Supports:
     * - New vote
     * - Vote change (user selects different option)
     * - Vote retraction (user removes their vote - empty optionIds)
     */
    private suspend fun handlePollAnswer(pollAnswer: PollAnswer) {
        try {
            val challenge = repository.findChallengeByPollId(pollAnswer.pollId.string) ?: run {
                logger.warn { "Received poll answer for unknown poll: ${pollAnswer.pollId}" }
                return
            }

            val user = pollAnswer.user
            val optionIds = pollAnswer.chosen

            // If user retracted their vote (empty list), delete the answer
            if (optionIds.isEmpty()) {
                repository.deletePollAnswer(challenge.id, user.id.chatId.long)
                logger.debug { "User ${user.id.chatId} retracted vote for challenge ${challenge.id}" }
                return
            }

            // Calculate points and completion status based on selected options
            var totalPoints = 0
            var isCompleted = false

            optionIds.forEach { optionIndex ->
                val optionConfig = repository.getPollOptionConfig(challenge.id, optionIndex)

                if (optionConfig != null) {
                    totalPoints += optionConfig.points
                    if (optionConfig.countsAsCompleted) {
                        isCompleted = true
                    }
                }
            }

            val answer = PollAnswerEntity(
                challengeId = challenge.id,
                userId = user.id.chatId.long,
                userName = user.username?.username,
                userFirstName = user.firstName,
                userLastName = user.lastName,
                optionIds = optionIds,
                points = totalPoints,
                isCompleted = isCompleted,
                answeredAt = Clock.System.now()
            )

            // Save or update the answer (upsert logic in repository)
            repository.savePollAnswer(answer)
            logger.debug { "Saved poll answer from user ${user.id.chatId} for challenge ${challenge.id}: points=$totalPoints, completed=$isCompleted" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to handle poll answer" }
        }
    }


    /**
     * Format report message
     */
    private fun formatReport(groupConfig: GroupConfig, statistics: List<UserStatistics>): String {
        val sb = StringBuilder()
        sb.appendLine("📊 *Статистика выполнения заданий*")
        sb.appendLine()

        if (statistics.isEmpty()) {
            sb.appendLine("Пока нет данных для отображения.")
            return sb.toString()
        }

        val sortedStats = when (groupConfig.report.sortBy) {
            SortBy.POINTS_DESC ->
                statistics.sortedByDescending { it.totalPoints }
            SortBy.POINTS_ASC ->
                statistics.sortedBy { it.totalPoints }
            SortBy.COMPLETION_DESC ->
                statistics.sortedByDescending { it.completedCount }
            SortBy.COMPLETION_ASC ->
                statistics.sortedBy { it.completedCount }
            SortBy.NAME ->
                statistics.sortedBy { it.displayName }
        }

        if (groupConfig.report.includeCompletionStats) {
            sb.appendLine("*Выполнено заданий:*")
            sortedStats.forEach { stat ->
                sb.appendLine("${stat.displayName}: ${stat.completedCount}/${stat.totalChallenges}")
            }
            sb.appendLine()
        }

        if (groupConfig.report.includePointsStats) {
            sb.appendLine("*Очки:*")
            sortedStats.forEach { stat ->
                sb.appendLine("${stat.displayName}: ${stat.totalPoints}")
            }
        }

        return sb.toString()
    }
}

