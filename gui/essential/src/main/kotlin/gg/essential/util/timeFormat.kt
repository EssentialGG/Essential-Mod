/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package gg.essential.util

import gg.essential.config.EssentialConfig
import gg.essential.gui.elementa.state.v2.ObservedDuration
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.*
import java.util.concurrent.TimeUnit

const val DATE_FORMAT = "MMM dd, yyyy"
const val DATE_FORMAT_NO_YEAR = "MMM dd"

fun getTimeFormat(includeSeconds: Boolean): String {
    val seconds = if (includeSeconds) ":ss" else ""
    return when (EssentialConfig.timeFormat) {
        EssentialConfig.TimeFormat.AM_PM -> "h:mm$seconds a"
        EssentialConfig.TimeFormat.REGULAR -> "HH:mm$seconds"
    }
}

fun formatDate(date: LocalDate, displayYear: Boolean = true) =
    date.formatter(if (displayYear) DATE_FORMAT else DATE_FORMAT_NO_YEAR)

fun formatTime(time: TemporalAccessor, includeSeconds: Boolean) =
    time.formatter(getTimeFormat(includeSeconds))

fun formatDateAndTime(date: TemporalAccessor) =
    date.formatter("$DATE_FORMAT @ ${getTimeFormat(includeSeconds = false)}")

fun TemporalAccessor.formatter(pattern: String): String {
    val format = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH).withZone(ZoneId.systemDefault())
    return format.format(this)
}

private val weeksTimeMap =
    listOf(
        TimeUnit.DAYS.toMillis(7) to "w",
        TimeUnit.DAYS.toMillis(1) to "d",
        TimeUnit.HOURS.toMillis(1) to "h",
        TimeUnit.MINUTES.toMillis(1) to "m",
        TimeUnit.SECONDS.toMillis(1) to "s"
    )

private val daysTimeMap =
    listOf(
        TimeUnit.DAYS.toMillis(1) to "d",
        TimeUnit.HOURS.toMillis(1) to "h",
        TimeUnit.MINUTES.toMillis(1) to "m",
        TimeUnit.SECONDS.toMillis(1) to "s"
    )

fun Duration.toShortString(weeks: Boolean = true, expiredText: String = "Expired"): String =
    ObservedDuration(this) {}.toShortString(weeks, expiredText)

fun ObservedDuration.toShortString(weeks: Boolean = true, expiredText: String = "Expired"): String {
    val delta = toMillis()
    val timeMap = if (weeks) weeksTimeMap else daysTimeMap

    val ceilEntry = timeMap.find { delta.greaterOrEqual(it.first) } ?: return expiredText
    val floorEntry = timeMap.find { ceilEntry.first - 1 >= it.first }
    if (ceilEntry === floorEntry || floorEntry == null) {
        return (delta / ceilEntry.first).toString() + ceilEntry.second
    }
    val ceilUnits = delta / ceilEntry.first
    val floorUnits = (delta - ceilUnits * ceilEntry.first) / floorEntry.first

    return ceilUnits.toString() + ceilEntry.second + " " + floorUnits.toString() + floorEntry.second
}
