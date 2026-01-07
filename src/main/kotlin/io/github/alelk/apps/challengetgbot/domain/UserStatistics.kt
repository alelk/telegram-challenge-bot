package io.github.alelk.apps.challengetgbot.domain

/**
 * User statistics for a group
 */
data class UserStatistics(
    val userId: Long,
    val userName: String?,
    val firstName: String?,
    val lastName: String?,
    val totalPoints: Int,
    val completedCount: Int,
    val totalChallenges: Int
) {
    val displayName: String
        get() = when {
            !userName.isNullOrBlank() -> "@$userName"
            !firstName.isNullOrBlank() && !lastName.isNullOrBlank() -> "$firstName $lastName"
            !firstName.isNullOrBlank() -> firstName
            !lastName.isNullOrBlank() -> lastName
            else -> "User #$userId"
        }
}