package io.github.alelk.apps.challengetgbot.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class UserStatisticsTest : FunSpec({

    context("displayName computation") {
        test("should return @username when username is present") {
            val stats = UserStatistics(
                userId = 100L,
                userName = "testuser",
                firstName = "Test",
                lastName = "User",
                totalPoints = 100,
                completedCount = 5,
                totalChallenges = 10
            )
            stats.displayName shouldBe "@testuser"
        }

        test("should return full name when username is null but first and last names present") {
            val stats = UserStatistics(
                userId = 100L,
                userName = null,
                firstName = "Иван",
                lastName = "Петров",
                totalPoints = 100,
                completedCount = 5,
                totalChallenges = 10
            )
            stats.displayName shouldBe "Иван Петров"
        }

        test("should return first name only when last name is null") {
            val stats = UserStatistics(
                userId = 100L,
                userName = null,
                firstName = "Иван",
                lastName = null,
                totalPoints = 100,
                completedCount = 5,
                totalChallenges = 10
            )
            stats.displayName shouldBe "Иван"
        }

        test("should return last name only when first name is null") {
            val stats = UserStatistics(
                userId = 100L,
                userName = null,
                firstName = null,
                lastName = "Петров",
                totalPoints = 100,
                completedCount = 5,
                totalChallenges = 10
            )
            stats.displayName shouldBe "Петров"
        }

        test("should return User #id when all names are null") {
            val stats = UserStatistics(
                userId = 12345L,
                userName = null,
                firstName = null,
                lastName = null,
                totalPoints = 100,
                completedCount = 5,
                totalChallenges = 10
            )
            stats.displayName shouldBe "User #12345"
        }

        test("should return @username even when first and last names are present") {
            val stats = UserStatistics(
                userId = 100L,
                userName = "preferred_username",
                firstName = "Иван",
                lastName = "Петров",
                totalPoints = 100,
                completedCount = 5,
                totalChallenges = 10
            )
            stats.displayName shouldBe "@preferred_username"
        }

        test("should handle blank username as null") {
            val stats = UserStatistics(
                userId = 100L,
                userName = "   ",
                firstName = "Иван",
                lastName = null,
                totalPoints = 100,
                completedCount = 5,
                totalChallenges = 10
            )
            stats.displayName shouldBe "Иван"
        }

        test("should handle blank first name as null") {
            val stats = UserStatistics(
                userId = 100L,
                userName = null,
                firstName = "",
                lastName = "Петров",
                totalPoints = 100,
                completedCount = 5,
                totalChallenges = 10
            )
            stats.displayName shouldBe "Петров"
        }

        test("should not add extra @ when username already starts with @") {
            val stats = UserStatistics(
                userId = 100L,
                userName = "@already_prefixed",
                firstName = "Test",
                lastName = "User",
                totalPoints = 100,
                completedCount = 5,
                totalChallenges = 10
            )
            stats.displayName shouldBe "@already_prefixed"
        }
    }

    context("statistics values") {
        test("should store correct total points") {
            val stats = UserStatistics(
                userId = 100L,
                userName = "test",
                firstName = null,
                lastName = null,
                totalPoints = 250,
                completedCount = 5,
                totalChallenges = 10
            )
            stats.totalPoints shouldBe 250
        }

        test("should store correct completed count") {
            val stats = UserStatistics(
                userId = 100L,
                userName = "test",
                firstName = null,
                lastName = null,
                totalPoints = 100,
                completedCount = 7,
                totalChallenges = 10
            )
            stats.completedCount shouldBe 7
        }

        test("should store correct total challenges") {
            val stats = UserStatistics(
                userId = 100L,
                userName = "test",
                firstName = null,
                lastName = null,
                totalPoints = 100,
                completedCount = 5,
                totalChallenges = 15
            )
            stats.totalChallenges shouldBe 15
        }
    }
})

