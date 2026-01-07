package io.github.alelk.apps.challengetgbot.repository

import io.github.alelk.apps.challengetgbot.db.DatabaseService
import io.github.alelk.apps.challengetgbot.domain.ChallengeEntity
import io.github.alelk.apps.challengetgbot.domain.PollAnswerEntity
import io.github.alelk.apps.challengetgbot.domain.PollOptionConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ChallengeRepositoryTest : FunSpec({

    lateinit var repository: ChallengeRepository

    beforeEach {
        DatabaseService.initForTest()
        repository = ChallengeRepository()
    }

    afterEach {
        DatabaseService.dropAllTables()
    }

    test("saveChallenge should save and return id") {
        val challenge = ChallengeEntity(
            groupName = "TestGroup",
            pollId = "poll123",
            messageId = 12345L,
            chatId = -1001234567890L,
            questionText = "Test question?",
            postedAt = Clock.System.now()
        )

        val id = repository.saveChallenge(challenge)

        id shouldNotBe 0L
    }

    test("findChallengeByPollId should return challenge when exists") {
        val challenge = ChallengeEntity(
            groupName = "TestGroup",
            pollId = "poll456",
            messageId = 12345L,
            chatId = -1001234567890L,
            questionText = "Test question?",
            postedAt = Clock.System.now()
        )

        repository.saveChallenge(challenge)

        val found = repository.findChallengeByPollId("poll456")

        found shouldNotBe null
        found!!.groupName shouldBe "TestGroup"
        found.pollId shouldBe "poll456"
    }

    test("findChallengeByPollId should return null when not exists") {
        val found = repository.findChallengeByPollId("nonexistent")
        found shouldBe null
    }

    test("savePollAnswer should save new answer") {
        val challengeId = repository.saveChallenge(ChallengeEntity(
            groupName = "TestGroup",
            pollId = "poll789",
            messageId = 12345L,
            chatId = -1001234567890L,
            questionText = "Test?",
            postedAt = Clock.System.now()
        ))

        val answer = PollAnswerEntity(
            challengeId = challengeId,
            userId = 100L,
            userName = "testuser",
            userFirstName = "Test",
            userLastName = "User",
            optionIds = listOf(0),
            points = 25,
            isCompleted = true,
            answeredAt = Clock.System.now()
        )

        repository.savePollAnswer(answer)

        val found = repository.getPollAnswer(challengeId, 100L)
        found shouldNotBe null
        found!!.points shouldBe 25
        found.isCompleted shouldBe true
    }

    test("savePollAnswer should update existing answer when user changes vote") {
        val challengeId = repository.saveChallenge(ChallengeEntity(
            groupName = "TestGroup",
            pollId = "poll_update",
            messageId = 12345L,
            chatId = -1001234567890L,
            questionText = "Test?",
            postedAt = Clock.System.now()
        ))

        // First answer: 25 points
        val firstAnswer = PollAnswerEntity(
            challengeId = challengeId,
            userId = 200L,
            userName = "user2",
            userFirstName = "User",
            userLastName = "Two",
            optionIds = listOf(0),
            points = 25,
            isCompleted = true,
            answeredAt = Clock.System.now()
        )
        repository.savePollAnswer(firstAnswer)

        // User changes vote: 100 points
        val updatedAnswer = firstAnswer.copy(
            optionIds = listOf(3),
            points = 100,
            answeredAt = Clock.System.now()
        )
        repository.savePollAnswer(updatedAnswer)

        // Check that points are updated
        val found = repository.getPollAnswer(challengeId, 200L)
        found shouldNotBe null
        found!!.points shouldBe 100
        found.optionIds shouldContainExactly listOf(3)
    }

    test("deletePollAnswer should remove answer") {
        val challengeId = repository.saveChallenge(ChallengeEntity(
            groupName = "TestGroup",
            pollId = "poll_delete",
            messageId = 12345L,
            chatId = -1001234567890L,
            questionText = "Test?",
            postedAt = Clock.System.now()
        ))

        val answer = PollAnswerEntity(
            challengeId = challengeId,
            userId = 300L,
            userName = "user3",
            userFirstName = "User",
            userLastName = "Three",
            optionIds = listOf(1),
            points = 50,
            isCompleted = true,
            answeredAt = Clock.System.now()
        )
        repository.savePollAnswer(answer)

        repository.deletePollAnswer(challengeId, 300L)

        val found = repository.getPollAnswer(challengeId, 300L)
        found shouldBe null
    }

    test("getUserStatistics should calculate correct totals after vote changes") {
        val challengeId = repository.saveChallenge(ChallengeEntity(
            groupName = "StatsGroup",
            pollId = "poll_stats",
            messageId = 12345L,
            chatId = -1001234567890L,
            questionText = "Test?",
            postedAt = Clock.System.now()
        ))

        // User 1 answers with 25 points
        repository.savePollAnswer(PollAnswerEntity(
            challengeId = challengeId,
            userId = 400L,
            userName = "user4",
            userFirstName = "User",
            userLastName = "Four",
            optionIds = listOf(0),
            points = 25,
            isCompleted = true,
            answeredAt = Clock.System.now()
        ))

        // User 1 changes to 75 points
        repository.savePollAnswer(PollAnswerEntity(
            challengeId = challengeId,
            userId = 400L,
            userName = "user4",
            userFirstName = "User",
            userLastName = "Four",
            optionIds = listOf(2),
            points = 75,
            isCompleted = true,
            answeredAt = Clock.System.now()
        ))

        // Get statistics - should show 75 points, not 100 (25+75)
        val stats = repository.getUserStatistics("StatsGroup")

        stats shouldHaveSize 1
        stats.first().totalPoints shouldBe 75
        stats.first().completedCount shouldBe 1
    }

    test("getUserStatistics should handle multiple users") {
        val challenge1Id = repository.saveChallenge(ChallengeEntity(
            groupName = "MultiGroup",
            pollId = "poll_multi1",
            messageId = 1L,
            chatId = -1001234567890L,
            questionText = "Challenge 1",
            postedAt = Clock.System.now()
        ))

        val challenge2Id = repository.saveChallenge(ChallengeEntity(
            groupName = "MultiGroup",
            pollId = "poll_multi2",
            messageId = 2L,
            chatId = -1001234567890L,
            questionText = "Challenge 2",
            postedAt = Clock.System.now()
        ))

        // User A: 50 + 100 = 150 points, 2 completed
        repository.savePollAnswer(PollAnswerEntity(
            challengeId = challenge1Id,
            userId = 500L,
            userName = "userA",
            userFirstName = "User",
            userLastName = "A",
            optionIds = listOf(1),
            points = 50,
            isCompleted = true,
            answeredAt = Clock.System.now()
        ))
        repository.savePollAnswer(PollAnswerEntity(
            challengeId = challenge2Id,
            userId = 500L,
            userName = "userA",
            userFirstName = "User",
            userLastName = "A",
            optionIds = listOf(3),
            points = 100,
            isCompleted = true,
            answeredAt = Clock.System.now()
        ))

        // User B: 25 points, 1 completed
        repository.savePollAnswer(PollAnswerEntity(
            challengeId = challenge1Id,
            userId = 600L,
            userName = "userB",
            userFirstName = "User",
            userLastName = "B",
            optionIds = listOf(0),
            points = 25,
            isCompleted = true,
            answeredAt = Clock.System.now()
        ))

        val stats = repository.getUserStatistics("MultiGroup")

        stats shouldHaveSize 2

        val userAStats = stats.find { it.userId == 500L }
        val userBStats = stats.find { it.userId == 600L }

        userAStats shouldNotBe null
        userAStats!!.totalPoints shouldBe 150
        userAStats.completedCount shouldBe 2

        userBStats shouldNotBe null
        userBStats!!.totalPoints shouldBe 25
        userBStats.completedCount shouldBe 1
    }

    test("savePollOptionConfig and getPollOptionConfig should work correctly") {
        val challengeId = repository.saveChallenge(ChallengeEntity(
            groupName = "TestGroup",
            pollId = "poll_options",
            messageId = 12345L,
            chatId = -1001234567890L,
            questionText = "Test?",
            postedAt = Clock.System.now()
        ))

        repository.savePollOptionConfig(challengeId, 0, "Option 1", 25, true)
        repository.savePollOptionConfig(challengeId, 1, "Option 2", 50, true)
        repository.savePollOptionConfig(challengeId, 2, "Skip", 0, false)

        val option0 = repository.getPollOptionConfig(challengeId, 0)
        val option1 = repository.getPollOptionConfig(challengeId, 1)
        val option2 = repository.getPollOptionConfig(challengeId, 2)
        val optionMissing = repository.getPollOptionConfig(challengeId, 99)

        option0 shouldBe PollOptionConfig(points = 25, countsAsCompleted = true)
        option1 shouldBe PollOptionConfig(points = 50, countsAsCompleted = true)
        option2 shouldBe PollOptionConfig(points = 0, countsAsCompleted = false)
        optionMissing shouldBe null
    }

    test("getTotalChallengesCount should return correct count") {
        repository.saveChallenge(ChallengeEntity(
            groupName = "CountGroup",
            pollId = "count1",
            messageId = 1L,
            chatId = -1001234567890L,
            questionText = "Q1",
            postedAt = Clock.System.now()
        ))
        repository.saveChallenge(ChallengeEntity(
            groupName = "CountGroup",
            pollId = "count2",
            messageId = 2L,
            chatId = -1001234567890L,
            questionText = "Q2",
            postedAt = Clock.System.now()
        ))
        repository.saveChallenge(ChallengeEntity(
            groupName = "OtherGroup",
            pollId = "other1",
            messageId = 3L,
            chatId = -1009999999999L,
            questionText = "Q3",
            postedAt = Clock.System.now()
        ))

        val countGroup = repository.getTotalChallengesCount("CountGroup")
        val otherGroup = repository.getTotalChallengesCount("OtherGroup")
        val nonexistent = repository.getTotalChallengesCount("NonExistent")

        countGroup shouldBe 2
        otherGroup shouldBe 1
        nonexistent shouldBe 0
    }

    test("statistics should not include deleted answers") {
        val challengeId = repository.saveChallenge(ChallengeEntity(
            groupName = "DeleteStatsGroup",
            pollId = "poll_delete_stats",
            messageId = 12345L,
            chatId = -1001234567890L,
            questionText = "Test?",
            postedAt = Clock.System.now()
        ))

        // Add answer
        repository.savePollAnswer(PollAnswerEntity(
            challengeId = challengeId,
            userId = 700L,
            userName = "user7",
            userFirstName = "User",
            userLastName = "Seven",
            optionIds = listOf(0),
            points = 100,
            isCompleted = true,
            answeredAt = Clock.System.now()
        ))

        // Verify stats include the answer
        val statsBefore = repository.getUserStatistics("DeleteStatsGroup")
        statsBefore shouldHaveSize 1
        statsBefore.first().totalPoints shouldBe 100

        // Delete answer (user retracted vote)
        repository.deletePollAnswer(challengeId, 700L)

        // Verify stats don't include deleted answer
        val statsAfter = repository.getUserStatistics("DeleteStatsGroup")
        statsAfter shouldHaveSize 0
    }
})

