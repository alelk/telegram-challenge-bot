package io.github.alelk.apps.challengetgbot.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ChallengeEntityTest : FunSpec({

    test("should create challenge entity with all fields") {
        val now = Clock.System.now()
        val entity = ChallengeEntity(
            id = 1L,
            groupName = "TestGroup",
            pollId = "poll123",
            messageId = 456L,
            chatId = -1001234567890L,
            questionText = "Test question?",
            postedAt = now
        )

        entity.id shouldBe 1L
        entity.groupName shouldBe "TestGroup"
        entity.pollId shouldBe "poll123"
        entity.messageId shouldBe 456L
        entity.chatId shouldBe -1001234567890L
        entity.questionText shouldBe "Test question?"
        entity.postedAt shouldBe now
    }

    test("should have default id of 0") {
        val entity = ChallengeEntity(
            groupName = "TestGroup",
            pollId = "poll123",
            messageId = 456L,
            chatId = -1001234567890L,
            questionText = "Test?",
            postedAt = Clock.System.now()
        )

        entity.id shouldBe 0L
    }

    test("should support copy with new id") {
        val original = ChallengeEntity(
            groupName = "TestGroup",
            pollId = "poll123",
            messageId = 456L,
            chatId = -1001234567890L,
            questionText = "Test?",
            postedAt = Clock.System.now()
        )

        val copied = original.copy(id = 999L)

        copied.id shouldBe 999L
        copied.groupName shouldBe original.groupName
        copied.pollId shouldBe original.pollId
    }
})