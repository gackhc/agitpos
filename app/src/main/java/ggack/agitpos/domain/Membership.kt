package ggack.agitpos.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.TemporalAdjusters

object Membership {
    private val timestamp = DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(ResolverStyle.STRICT)
    fun encode(expiry: LocalDateTime): String = expiry.format(timestamp)

    fun expiry(value: String?): LocalDateTime? = runCatching {
        when (value?.length) {
            6 -> YearMonth.parse(value, DateTimeFormatter.ofPattern("uuuuMM"))
                .plusMonths(1).atDay(1).atStartOfDay()
            8 -> LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE).plusDays(1).atStartOfDay()
            14 -> LocalDateTime.parse(value, timestamp)
            else -> null
        }
    }.getOrNull()

    fun monthly(now: LocalDateTime): LocalDateTime =
        now.toLocalDate().withDayOfMonth(1).plusMonths(1).atTime(8, 0)

    fun weekly(now: LocalDateTime): LocalDateTime =
        now.toLocalDate().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(8, 0)
}
