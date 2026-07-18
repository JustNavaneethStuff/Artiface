package com.artiface.core.common.generation

import com.artiface.core.model.TimeOfDay
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimeOfDayCategorizationTest {

    @Test
    fun categorizes_dayparts() {
        assertThat(categorizeHour(7)).isEqualTo(TimeOfDay.Morning)
        assertThat(categorizeHour(14)).isEqualTo(TimeOfDay.Afternoon)
        assertThat(categorizeHour(19)).isEqualTo(TimeOfDay.Evening)
        assertThat(categorizeHour(23)).isEqualTo(TimeOfDay.Night)
        assertThat(categorizeHour(2)).isEqualTo(TimeOfDay.Night)
    }
}
