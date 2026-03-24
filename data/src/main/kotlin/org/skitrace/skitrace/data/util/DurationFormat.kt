package org.skitrace.skitrace.data.util

import java.util.concurrent.TimeUnit

fun DurationFormat(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return if (hours > 0) {
            "%dh %02dm".format(hours, minutes)
        } else {
            "%02dm".format(minutes)
        }
}