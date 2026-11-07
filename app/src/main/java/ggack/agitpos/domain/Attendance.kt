package ggack.agitpos.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object Attendance {
    fun nextCount(previous: Long, today: LocalDate): Int? {
        val date = runCatching {
            LocalDate.parse((previous / 100).toString(), DateTimeFormatter.BASIC_ISO_DATE)
        }.getOrNull()
        if (date != null && !date.isBefore(today)) return null
        val count = if (date != null && YearMonth.from(date) == YearMonth.from(today)) (previous % 100).toInt() else 0
        return if (count in 0..7) count + 1 else null
    }
}
