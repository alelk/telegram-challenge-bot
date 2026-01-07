package io.github.alelk.apps.challengetgbot.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class PollAnswerEntityTest : FunSpec({

    test("should create poll answer entity with all fields") {
        val now = Clock.System.now()
        val entity = PollAnswerEntity(
            challengeId = 1L,
            userId = 100L,
            userName = "testuser",
            userFirstName = "Test",
            userLastName = "User",
            optionIds = listOf(0, 2),
            points = 75,
            isCompleted = true,
            answeredAt = now
        )

        entity.challengeId shouldBe 1L
        entity.userId shouldBe 100L
        entity.userName shouldBe "testuser"
        entity.userFirstName shouldBe "Test"
        entity.userLastName shouldBe "User"
        entity.optionIds shouldBe listOf(0, 2)
        entity.points shouldBe 75
        entity.isCompleted shouldBe true
        entity.answeredAt shouldBe now
    }

    test("should handle null optional fields") {
        val entity = PollAnswerEntity(
            challengeId = 1L,
            userId = 100L,
            userName = null,
            userFirstName = null,
            userLastName = null,
            optionIds = listOf(0),
            points = 25,
            isCompleted = true,
            answeredAt = Clock.System.now()
        )

        entity.userName shouldBe null
        entity.userFirstName shouldBe null
        entity.userLastName shouldBe null
    }

    test("should handle empty option ids") {
        val entity = PollAnswerEntity(
            challengeId = 1L,
            userId = 100L,
            userName = "test",
            userFirstName = null,
            userLastName = null,
            optionIds = emptyList(),
            points = 0,
            isCompleted = false,
            answeredAt = Clock.System.now()
        )

        entity.optionIds shouldBe emptyList()
    }

    test("should support updating via copy") {
        val original = PollAnswerEntity(
            challengeId = 1L,
            userId = 100L,
            userName = "test",
            userFirstName = null,
            userLastName = null,
            optionIds = listOf(0),
            points = 25,
            isCompleted = true,
            answeredAt = Clock.System.now()
        )

        val updated = original.copy(
            optionIds = listOf(3),
            points = 100
        )

        updated.challengeId shouldBe original.challengeId
        updated.userId shouldBe original.userId
        updated.optionIds shouldBe listOf(3)
        updated.points shouldBe 100
    }
})