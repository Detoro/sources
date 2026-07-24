package toro.sources.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun convertTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val date = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return date.format(formatter)
}