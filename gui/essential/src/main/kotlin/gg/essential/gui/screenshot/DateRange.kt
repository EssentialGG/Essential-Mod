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
package gg.essential.gui.screenshot

import java.text.SimpleDateFormat
import java.util.*

enum class DateRange(private val displayName: String) {

    MONTH_OTHER(""),
    LAST_MONTH("Last Month"),
    EARLIER_MONTH("Earlier This Month"),
    LAST_WEEK("Last Week"),
    EARLIER_WEEK("Earlier This Week"),
    YESTERDAY("Yesterday"),
    TODAY("Today");

    fun getName(startTime: Long): String {
        return if (this == MONTH_OTHER) {
            format.format(Date(startTime))
        } else {
            displayName
        }
    }

    fun getStartTime(): Long {
        val date = createDateOnlyCalendar()
        when (this) {
            TODAY -> {
            }

            YESTERDAY -> {
                date.roll(Calendar.DAY_OF_MONTH, -1)
            }

            EARLIER_WEEK -> {
                date.set(Calendar.DAY_OF_WEEK, 1)
            }

            LAST_WEEK -> {
                date.set(Calendar.DAY_OF_WEEK, 1)
                date.roll(Calendar.WEEK_OF_YEAR, -1)
            }

            EARLIER_MONTH -> {
                date.set(Calendar.DAY_OF_MONTH, 1)
            }

            LAST_MONTH -> {
                date.set(Calendar.DAY_OF_MONTH, 1)
                date.roll(Calendar.MONTH, -1)
            }

            MONTH_OTHER -> throw IllegalArgumentException("MONTH_OTHER is not a valid start time")
        }
        return date.time.time
    }

    companion object {
        private val format = SimpleDateFormat("MMMM YYYY", Locale.ENGLISH)
    }

}