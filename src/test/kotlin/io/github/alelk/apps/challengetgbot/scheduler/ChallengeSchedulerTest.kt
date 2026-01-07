package io.github.alelk.apps.challengetgbot.scheduler

import io.github.alelk.apps.challengetgbot.config.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ChallengeSchedulerTest : FunSpec({

    /**
     * Helper to create a minimal GroupConfig for testing
     */
    fun createGroupConfig(
        scheduleFrequency: Frequency = Frequency.DAILY,
        scheduleTime: LocalTime = LocalTime.of(9, 0, 0),
        scheduleDaysOfWeek: List<DayOfWeek>? = null,
        timezone: String = "UTC"
    ): GroupConfig {
        return GroupConfig(
            chatId = -1001234567890L,
            threadId = null,
            name = "TestGroup",
            challenge = ChallengeConfig(
                questionTemplate = "{date} - Test?",
                options = listOf(
                    PollOption("Yes", 100, true),
                    PollOption("No", 0, false)
                )
            ),
            schedule = ScheduleConfig(
                frequency = scheduleFrequency,
                time = scheduleTime,
                daysOfWeek = scheduleDaysOfWeek,
                timezone = timezone
            ),
            report = ReportConfig(
                frequency = ReportFrequency.WEEKLY,
                dayOfWeek = DayOfWeek.MONDAY,
                time = LocalTime.of(20, 0, 0),
                timezone = timezone
            )
        )
    }

    context("GroupConfig creation") {
        test("schedule config should accept valid time") {
            val time = LocalTime.of(9, 30, 0)
            val config = createGroupConfig(scheduleTime = time)
            config.schedule.time.hour shouldBe 9
            config.schedule.time.minute shouldBe 30
        }

        test("schedule config should support daily frequency") {
            val config = createGroupConfig(scheduleFrequency = Frequency.DAILY)
            config.schedule.frequency shouldBe Frequency.DAILY
        }

        test("schedule config should support weekly frequency with days") {
            val config = createGroupConfig(
                scheduleFrequency = Frequency.WEEKLY,
                scheduleDaysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            )
            config.schedule.frequency shouldBe Frequency.WEEKLY
            config.schedule.daysOfWeek?.size shouldBe 3
            config.schedule.daysOfWeek shouldContainExactly listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        }

        test("schedule config should support custom timezone") {
            val config = createGroupConfig(timezone = "Europe/Moscow")
            config.schedule.timezone shouldBe "Europe/Moscow"
        }

        test("report config should have correct default values") {
            val config = createGroupConfig()
            config.report.frequency shouldBe ReportFrequency.WEEKLY
            config.report.dayOfWeek shouldBe DayOfWeek.MONDAY
            config.report.includeCompletionStats shouldBe true
            config.report.includePointsStats shouldBe true
            config.report.sortBy shouldBe SortBy.POINTS_DESC
        }

        test("question template should contain date placeholder") {
            val config = createGroupConfig()
            config.challenge.questionTemplate shouldNotBe null
            config.challenge.questionTemplate.contains("{date}") shouldBe true
        }
    }

    context("Russian date formatting") {
        val monthNames = listOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
        )

        fun formatDate(date: LocalDate): String {
            return "${date.dayOfMonth} ${monthNames[date.monthValue - 1]}"
        }

        test("month names list should have 12 items") {
            monthNames.size shouldBe 12
        }

        test("January should format correctly") {
            formatDate(LocalDate.of(2026, 1, 7)) shouldBe "7 января"
        }

        test("December should format correctly") {
            formatDate(LocalDate.of(2026, 12, 25)) shouldBe "25 декабря"
        }

        test("June should format correctly") {
            formatDate(LocalDate.of(2026, 6, 15)) shouldBe "15 июня"
        }

        test("all months should have correct names") {
            monthNames[0] shouldBe "января"
            monthNames[1] shouldBe "февраля"
            monthNames[2] shouldBe "марта"
            monthNames[3] shouldBe "апреля"
            monthNames[4] shouldBe "мая"
            monthNames[5] shouldBe "июня"
            monthNames[6] shouldBe "июля"
            monthNames[7] shouldBe "августа"
            monthNames[8] shouldBe "сентября"
            monthNames[9] shouldBe "октября"
            monthNames[10] shouldBe "ноября"
            monthNames[11] shouldBe "декабря"
        }
    }

    context("Russian day of week formatting") {
        fun formatDayOfWeek(dayOfWeek: DayOfWeek): String {
            return when (dayOfWeek) {
                DayOfWeek.MONDAY -> "понедельник"
                DayOfWeek.TUESDAY -> "вторник"
                DayOfWeek.WEDNESDAY -> "среда"
                DayOfWeek.THURSDAY -> "четверг"
                DayOfWeek.FRIDAY -> "пятница"
                DayOfWeek.SATURDAY -> "суббота"
                DayOfWeek.SUNDAY -> "воскресенье"
            }
        }

        test("Monday should be понедельник") {
            formatDayOfWeek(DayOfWeek.MONDAY) shouldBe "понедельник"
        }

        test("Tuesday should be вторник") {
            formatDayOfWeek(DayOfWeek.TUESDAY) shouldBe "вторник"
        }

        test("Wednesday should be среда") {
            formatDayOfWeek(DayOfWeek.WEDNESDAY) shouldBe "среда"
        }

        test("Thursday should be четверг") {
            formatDayOfWeek(DayOfWeek.THURSDAY) shouldBe "четверг"
        }

        test("Friday should be пятница") {
            formatDayOfWeek(DayOfWeek.FRIDAY) shouldBe "пятница"
        }

        test("Saturday should be суббота") {
            formatDayOfWeek(DayOfWeek.SATURDAY) shouldBe "суббота"
        }

        test("Sunday should be воскресенье") {
            formatDayOfWeek(DayOfWeek.SUNDAY) shouldBe "воскресенье"
        }
    }

    context("Question template substitution") {
        val monthNames = listOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
        )

        fun formatDate(date: LocalDate): String {
            return "${date.dayOfMonth} ${monthNames[date.monthValue - 1]}"
        }

        test("template with date variable should substitute correctly") {
            val template = "{date} - Отжимания?"
            val date = LocalDate.of(2026, 1, 7)
            val result = template.replace("{date}", formatDate(date))
            result shouldBe "7 января - Отжимания?"
        }

        test("template with multiple variables should substitute all") {
            val template = "{date} ({dayOfWeek}) - Задание дня"
            val date = LocalDate.of(2026, 1, 7)
            val result = template
                .replace("{date}", formatDate(date))
                .replace("{dayOfWeek}", "среда")
            result shouldBe "7 января (среда) - Задание дня"
        }

        test("template without variables should remain unchanged") {
            val template = "Простой вопрос без переменных?"
            val result = template.replace("{date}", "unused")
            result shouldBe "Простой вопрос без переменных?"
        }

        test("template with day and month separately") {
            val template = "{day} {month} {year}"
            val date = LocalDate.of(2026, 1, 7)
            val result = template
                .replace("{day}", date.dayOfMonth.toString())
                .replace("{month}", monthNames[date.monthValue - 1])
                .replace("{year}", date.year.toString())
            result shouldBe "7 января 2026"
        }
    }

    context("Time zone handling") {
        test("Moscow timezone should be valid") {
            val tz = ZoneId.of("Europe/Moscow")
            tz shouldNotBe null
        }

        test("UTC timezone should be valid") {
            val tz = ZoneId.of("UTC")
            tz shouldNotBe null
        }

        test("converting instant to zoned datetime with timezone") {
            val instant = java.time.Instant.now()
            val moscow = ZoneId.of("Europe/Moscow")
            val zonedDateTime = instant.atZone(moscow)
            zonedDateTime shouldNotBe null
        }
    }
})


