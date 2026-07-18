package com.artiface.core.common.generation

import com.artiface.core.model.TimeOfDay
import java.time.LocalTime

class LocalTimeContextProvider : TimeContextProvider {
    override fun currentTimeOfDay(): TimeOfDay {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..11 -> TimeOfDay.Morning
            in 12..16 -> TimeOfDay.Afternoon
            in 17..20 -> TimeOfDay.Evening
            else -> TimeOfDay.Night
        }
    }
}

fun categorizeHour(hour: Int): TimeOfDay = when (hour) {
    in 5..11 -> TimeOfDay.Morning
    in 12..16 -> TimeOfDay.Afternoon
    in 17..20 -> TimeOfDay.Evening
    else -> TimeOfDay.Night
}
