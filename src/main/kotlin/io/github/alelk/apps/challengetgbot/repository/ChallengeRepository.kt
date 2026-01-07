package io.github.alelk.apps.challengetgbot.repository

import io.github.alelk.apps.challengetgbot.db.Challenges
import io.github.alelk.apps.challengetgbot.db.DatabaseService
import io.github.alelk.apps.challengetgbot.db.PollAnswers
import io.github.alelk.apps.challengetgbot.db.PollOptionConfigs
import io.github.alelk.apps.challengetgbot.domain.ChallengeEntity
import io.github.alelk.apps.challengetgbot.domain.PollAnswerEntity
import io.github.alelk.apps.challengetgbot.domain.UserStatistics
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

/**
 * Repository for managing challenges and poll answers
 */
@OptIn(ExperimentalTime::class)
class ChallengeRepository {

    /**
     * Save a new challenge
     */
    fun saveChallenge(challenge: ChallengeEntity): Long {
        return DatabaseService.query {
            Challenges.insertAndGetId {
                it[groupName] = challenge.groupName
                it[pollId] = challenge.pollId
                it[messageId] = challenge.messageId
                it[chatId] = challenge.chatId
                it[questionText] = challenge.questionText
                it[postedAt] = challenge.postedAt
            }.value
        }
    }

    /**
     * Find challenge by poll ID
     */
    fun findChallengeByPollId(pollId: String): ChallengeEntity? {
        return DatabaseService.query {
            Challenges.selectAll()
                .where { Challenges.pollId eq pollId }
                .map { row ->
                    ChallengeEntity(
                        id = row[Challenges.id].value,
                        groupName = row[Challenges.groupName],
                        pollId = row[Challenges.pollId],
                        messageId = row[Challenges.messageId],
                        chatId = row[Challenges.chatId],
                        questionText = row[Challenges.questionText],
                        postedAt = row[Challenges.postedAt]
                    )
                }
                .firstOrNull()
        }
    }

    /**
     * Save or update poll answer.
     * When user changes their answer, the old answer is replaced with new points and completion status.
     */
    fun savePollAnswer(answer: PollAnswerEntity) {
        DatabaseService.query {
            val existing = PollAnswers.selectAll()
                .where { (PollAnswers.challengeId eq answer.challengeId) and (PollAnswers.userId eq answer.userId) }
                .firstOrNull()

            if (existing != null) {
                // User is changing their answer - update with new values
                PollAnswers.update({
                    (PollAnswers.challengeId eq answer.challengeId) and (PollAnswers.userId eq answer.userId)
                }) {
                    it[userName] = answer.userName
                    it[userFirstName] = answer.userFirstName
                    it[userLastName] = answer.userLastName
                    it[optionIds] = answer.optionIds.joinToString(",")
                    it[points] = answer.points
                    it[isCompleted] = answer.isCompleted
                    it[answeredAt] = answer.answeredAt
                }
            } else {
                PollAnswers.insert {
                    it[challengeId] = answer.challengeId
                    it[userId] = answer.userId
                    it[userName] = answer.userName
                    it[userFirstName] = answer.userFirstName
                    it[userLastName] = answer.userLastName
                    it[optionIds] = answer.optionIds.joinToString(",")
                    it[points] = answer.points
                    it[isCompleted] = answer.isCompleted
                    it[answeredAt] = answer.answeredAt
                }
            }
        }
    }

    /**
     * Delete poll answer when user retracts their vote
     */
    fun deletePollAnswer(challengeId: Long, userId: Long) {
        DatabaseService.query {
            PollAnswers.deleteWhere {
                (PollAnswers.challengeId eq challengeId) and (PollAnswers.userId eq userId)
            }
        }
    }

    /**
     * Get user statistics for a group.
     * Statistics are calculated from current poll answer values (already updated if user changed their vote).
     */
    fun getUserStatistics(groupName: String, from: Instant? = null, to: Instant? = null): List<UserStatistics> {
        return DatabaseService.query {
            val totalChallenges = getTotalChallengesCount(groupName, from, to)

            val pointsSum = PollAnswers.points.sum()
            val completedSum = PollAnswers.isCompleted.castTo<Int>(IntegerColumnType()).sum()

            var condition: Op<Boolean> = Challenges.groupName eq groupName
            from?.let { condition = condition and (Challenges.postedAt greaterEq it) }
            to?.let { condition = condition and (Challenges.postedAt lessEq it) }

            (Challenges innerJoin PollAnswers)
                .select(
                    PollAnswers.userId,
                    PollAnswers.userName,
                    PollAnswers.userFirstName,
                    PollAnswers.userLastName,
                    pointsSum,
                    completedSum
                )
                .where { condition }
                .groupBy(
                    PollAnswers.userId,
                    PollAnswers.userName,
                    PollAnswers.userFirstName,
                    PollAnswers.userLastName
                )
                .map { row ->
                    UserStatistics(
                        userId = row[PollAnswers.userId],
                        userName = row[PollAnswers.userName],
                        firstName = row[PollAnswers.userFirstName],
                        lastName = row[PollAnswers.userLastName],
                        totalPoints = row[pointsSum] ?: 0,
                        completedCount = row[completedSum] ?: 0,
                        totalChallenges = totalChallenges
                    )
                }
        }
    }

    /**
     * Get total number of challenges for a group in time range
     */
    fun getTotalChallengesCount(groupName: String, from: Instant? = null, to: Instant? = null): Int {
        return DatabaseService.query {
            var condition: Op<Boolean> = Challenges.groupName eq groupName
            from?.let { condition = condition and (Challenges.postedAt greaterEq it) }
            to?.let { condition = condition and (Challenges.postedAt lessEq it) }

            Challenges.selectAll()
                .where { condition }
                .count()
                .toInt()
        }
    }

    /**
     * Save poll option configuration
     */
    fun savePollOptionConfig(challengeId: Long, optionIndex: Int, optionText: String, points: Int, countsAsCompleted: Boolean) {
        DatabaseService.query {
            PollOptionConfigs.insert {
                it[PollOptionConfigs.challengeId] = challengeId
                it[PollOptionConfigs.optionIndex] = optionIndex
                it[PollOptionConfigs.optionText] = optionText
                it[PollOptionConfigs.points] = points
                it[PollOptionConfigs.countsAsCompleted] = countsAsCompleted
            }
        }
    }

    /**
     * Get poll option configuration
     */
    fun getPollOptionConfig(challengeId: Long, optionIndex: Int): Pair<Int, Boolean>? {
        return DatabaseService.query {
            PollOptionConfigs.selectAll()
                .where { (PollOptionConfigs.challengeId eq challengeId) and (PollOptionConfigs.optionIndex eq optionIndex) }
                .map { row ->
                    Pair(row[PollOptionConfigs.points], row[PollOptionConfigs.countsAsCompleted])
                }
                .firstOrNull()
        }
    }

    /**
     * Get poll answer for a specific user and challenge
     */
    fun getPollAnswer(challengeId: Long, userId: Long): PollAnswerEntity? {
        return DatabaseService.query {
            PollAnswers.selectAll()
                .where { (PollAnswers.challengeId eq challengeId) and (PollAnswers.userId eq userId) }
                .map { row ->
                    PollAnswerEntity(
                        challengeId = row[PollAnswers.challengeId].value,
                        userId = row[PollAnswers.userId],
                        userName = row[PollAnswers.userName],
                        userFirstName = row[PollAnswers.userFirstName],
                        userLastName = row[PollAnswers.userLastName],
                        optionIds = row[PollAnswers.optionIds].split(",").filter { it.isNotEmpty() }.map { it.toInt() },
                        points = row[PollAnswers.points],
                        isCompleted = row[PollAnswers.isCompleted],
                        answeredAt = row[PollAnswers.answeredAt]
                    )
                }
                .firstOrNull()
        }
    }
}

