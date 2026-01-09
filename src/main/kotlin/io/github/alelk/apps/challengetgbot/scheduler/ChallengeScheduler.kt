package io.github.alelk.apps.challengetgbot.scheduler

import io.github.alelk.apps.challengetgbot.config.Frequency
import io.github.alelk.apps.challengetgbot.config.GroupConfig
import io.github.alelk.apps.challengetgbot.config.ReportFrequency
import io.github.alelk.apps.challengetgbot.repository.ChallengeRepository
import io.github.alelk.apps.challengetgbot.telegram.TelegramBotService
import io.github.alelk.apps.challengetgbot.util.DateFormatter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.time.DayOfWeek
import java.time.Duration.between
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinDuration

private val logger = KotlinLogging.logger {}

/**
 * Scheduler for posting challenges and reports
 */
@OptIn(ExperimentalTime::class)
class ChallengeScheduler(
    private val telegramService: TelegramBotService,
    private val repository: ChallengeRepository
) {
    private val jobs = mutableListOf<Job>()

    /**
     * Start scheduling for a group
     */
    fun startScheduling(scope: CoroutineScope, groupConfig: GroupConfig) {
        // Schedule challenge posting
        val challengeJob = scope.launch {
            scheduleChallenge(groupConfig)
        }
        jobs.add(challengeJob)

        // Schedule report posting
        val reportJob = scope.launch {
            scheduleReport(groupConfig)
        }
        jobs.add(reportJob)

        logger.info { "Started scheduling for group '${groupConfig.name}'" }
    }

    /**
     * Stop all scheduling
     */
    fun stopScheduling() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        logger.info { "Stopped all scheduling" }
    }

    /**
     * Schedule challenge posting
     */
    private suspend fun scheduleChallenge(groupConfig: GroupConfig) {
        while (true) {
            try {
                val now = Clock.System.now()
                val nextTime = calculateNextChallengeTime(groupConfig, now)
                val delay = between(now.toJavaInstant(), nextTime.toJavaInstant()).toKotlinDuration()

                val zoneId = ZoneId.of(groupConfig.schedule.timezone)
                logger.info {
                    "Next challenge for '${groupConfig.name}' scheduled at: " +
                    "${ZonedDateTime.ofInstant(nextTime.toJavaInstant(), zoneId)}"
                }

                delay(delay)

                // Post the challenge
                val questionText = formatQuestionText(groupConfig, Clock.System.now())
                telegramService.postChallenge(groupConfig, questionText)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Error in challenge scheduling for group '${groupConfig.name}'" }
                delay(1.minutes) // Wait before retry
            }
        }
    }

    /**
     * Schedule report posting
     */
    private suspend fun scheduleReport(groupConfig: GroupConfig) {
        while (true) {
            try {
                val now = Clock.System.now()
                val nextTime = calculateNextReportTime(groupConfig, now)
                val delay = between(now.toJavaInstant(), nextTime.toJavaInstant()).toKotlinDuration()

                val zoneId = ZoneId.of(groupConfig.report.timezone)
                logger.info {
                    "Next report for '${groupConfig.name}' scheduled at: " +
                    "${ZonedDateTime.ofInstant(nextTime.toJavaInstant(), zoneId)}"
                }

                delay(delay)

                // Send the report
                val statistics = repository.getUserStatistics(groupConfig.name)
                telegramService.sendReport(groupConfig, statistics)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Error in report scheduling for group '${groupConfig.name}'" }
                delay(1.minutes) // Wait before retry
            }
        }
    }

    /**
     * Calculate next challenge time
     */
    private fun calculateNextChallengeTime(groupConfig: GroupConfig, from: Instant): Instant {
        val zoneId = ZoneId.of(groupConfig.schedule.timezone)
        val localNow = ZonedDateTime.ofInstant(from.toJavaInstant(), zoneId).toLocalDateTime()
        val targetTime = groupConfig.schedule.time

        return when (groupConfig.schedule.frequency) {
            Frequency.DAILY -> {
                val targetDate = if (localNow.toLocalTime() >= targetTime) {
                    localNow.toLocalDate().plusDays(1)
                } else {
                    localNow.toLocalDate()
                }
                LocalDateTime.of(targetDate, targetTime).atZone(zoneId).toInstant().toKotlinInstant()
            }
            Frequency.WEEKLY -> {
                val daysOfWeek = groupConfig.schedule.daysOfWeek ?: listOf(DayOfWeek.MONDAY)
                findNextWeeklyTime(localNow, targetTime, daysOfWeek, zoneId)
            }
            Frequency.CUSTOM -> {
                val daysOfWeek = groupConfig.schedule.daysOfWeek ?: listOf(DayOfWeek.MONDAY)
                findNextWeeklyTime(localNow, targetTime, daysOfWeek, zoneId)
            }
        }
    }

    /**
     * Calculate next report time
     */
    private fun calculateNextReportTime(groupConfig: GroupConfig, from: Instant): Instant {
        val zoneId = ZoneId.of(groupConfig.report.timezone)
        val localNow = ZonedDateTime.ofInstant(from.toJavaInstant(), zoneId).toLocalDateTime()
        val targetTime = groupConfig.report.time

        return when (groupConfig.report.frequency) {
            ReportFrequency.DAILY -> {
                val targetDate = if (localNow.toLocalTime() >= targetTime) {
                    localNow.toLocalDate().plusDays(1)
                } else {
                    localNow.toLocalDate()
                }
                LocalDateTime.of(targetDate, targetTime).atZone(zoneId).toInstant().toKotlinInstant()
            }
            ReportFrequency.WEEKLY -> {
                findNextWeeklyTime(
                    localNow,
                    targetTime,
                    listOf(groupConfig.report.dayOfWeek),
                    zoneId
                )
            }
            ReportFrequency.MONTHLY -> {
                // Default to first day of next month
                val targetDate = if (localNow.dayOfMonth == 1 && localNow.toLocalTime() < targetTime) {
                    localNow.toLocalDate()
                } else {
                    localNow.toLocalDate().plusMonths(1).withDayOfMonth(1)
                }
                LocalDateTime.of(targetDate, targetTime).atZone(zoneId).toInstant().toKotlinInstant()
            }
        }
    }

    /**
     * Find next occurrence of a weekly scheduled time
     */
    private fun findNextWeeklyTime(
        from: LocalDateTime,
        targetTime: LocalTime,
        daysOfWeek: List<DayOfWeek>,
        zoneId: ZoneId
    ): Instant {
        var date = from.toLocalDate()
        val currentTime = from.toLocalTime()

        // Try current day first
        if (daysOfWeek.contains(date.dayOfWeek) && currentTime < targetTime) {
            return LocalDateTime.of(date, targetTime).atZone(zoneId).toInstant().toKotlinInstant()
        }

        // Search for next day in the list
        for (i in 1..7) {
            date = date.plusDays(1)
            if (daysOfWeek.contains(date.dayOfWeek)) {
                return LocalDateTime.of(date, targetTime).atZone(zoneId).toInstant().toKotlinInstant()
            }
        }

        // Fallback (should never reach here)
        return LocalDateTime.of(date, targetTime).atZone(zoneId).toInstant().toKotlinInstant()
    }

    /**
     * Format question text with template variables
     */
    private fun formatQuestionText(groupConfig: GroupConfig, time: Instant): String {
        val zoneId = ZoneId.of(groupConfig.schedule.timezone)
        val localDate = ZonedDateTime.ofInstant(time.toJavaInstant(), zoneId).toLocalDate()
        return DateFormatter.formatQuestionTemplate(groupConfig.challenge.questionTemplate, localDate)
    }
}

@OptIn(ExperimentalTime::class)
private fun java.time.Instant.toKotlinInstant(): Instant = Instant.fromEpochMilliseconds(this.toEpochMilli())

