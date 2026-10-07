package com.asta669.wakeup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

class AlarmSchedulerTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun time(value: String) = Instant.parse(value).toEpochMilli()

    @Test fun futureTimeUsesToday() {
        assertEquals(time("2026-10-05T07:30:00Z"),
            AlarmScheduler.nextTriggerMillis(7, 30, time("2026-10-05T07:29:45Z"), utc))
    }

    @Test fun elapsedMinuteUsesTomorrowInsteadOfRingingImmediately() {
        assertEquals(time("2026-10-06T07:29:00Z"),
            AlarmScheduler.nextTriggerMillis(7, 29, time("2026-10-05T07:29:45Z"), utc))
    }

    @Test fun exactCurrentInstantUsesNextDay() {
        assertEquals(time("2026-10-06T07:30:00Z"),
            AlarmScheduler.nextTriggerMillis(7, 30, time("2026-10-05T07:30:00Z"), utc))
    }

    @Test fun midnightAdvancesAcrossYearBoundary() {
        assertEquals(time("2027-01-01T00:00:00Z"),
            AlarmScheduler.nextTriggerMillis(0, 0, time("2026-12-31T23:59:50Z"), utc))
    }

    @Test fun nextDayKeepsLocalHourAcrossSummerTime() {
        assertEquals(time("2026-03-29T05:30:00Z"),
            AlarmScheduler.nextTriggerMillis(7, 30, time("2026-03-28T07:00:00Z"),
                TimeZone.getTimeZone("Europe/Paris")))
    }

    @Test fun invalidTimesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { AlarmScheduler.nextTriggerMillis(24, 0, 0, utc) }
        assertThrows(IllegalArgumentException::class.java) { AlarmScheduler.nextTriggerMillis(7, -1, 0, utc) }
    }
}
