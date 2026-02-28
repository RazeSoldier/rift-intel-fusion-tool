package dev.nohus.rift.compose

import dev.nohus.rift.i18n.ApplicationLocale
import dev.nohus.rift.utils.getName
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun getRelativeTime(timestamp: Instant, displayTimezone: ZoneId, now: Instant = Instant.now()): String {
    if (ApplicationLocale == Locale.CHINESE) {
        return getRelativeTimeZh(timestamp, displayTimezone, now)
    }
    val duration = Duration.between(timestamp, now)
    return if (duration.toSeconds() < 5) {
        "just now"
    } else if (duration.toSeconds() < 60) {
        "${duration.toSeconds()} seconds ago"
    } else if (duration.toMinutes() < 2) {
        "1 minute ago"
    } else if (duration.toMinutes() < 60) {
        "${duration.toMinutes()} minutes ago"
    } else {
        val time = ZonedDateTime.ofInstant(timestamp, displayTimezone)
        val timezoneName = displayTimezone.getName()
        val formatted = if (duration.toHours() < 12) {
            DateTimeFormatter.ofPattern("HH:mm").format(time)
        } else {
            DateTimeFormatter.ofPattern("d MMM, HH:mm").format(time)
        }
        "$formatted $timezoneName"
    }
}

private fun getRelativeTimeZh(timestamp: Instant, displayTimezone: ZoneId, now: Instant = Instant.now()): String {
    val duration = Duration.between(timestamp, now)
    return when {
        duration.toSeconds() < 5 -> {
            "就是现在"
        }
        duration.toSeconds() < 60 -> {
            "${duration.toSeconds()}秒前"
        }
        duration.toMinutes() < 2 -> {
            "1分钟前"
        }
        duration.toMinutes() < 60 -> {
            "${duration.toMinutes()}分钟前"
        }
        else -> {
            val time = ZonedDateTime.ofInstant(timestamp, displayTimezone)
            val timezoneName = displayTimezone.getName()
            val formatted = if (duration.toHours() < 12) {
                DateTimeFormatter.ofPattern("HH:mm").format(time)
            } else {
                DateTimeFormatter.ofPattern("MMMd HH:mm").format(time)
            }
            "$formatted $timezoneName"
        }
    }
}