package com.selfbell.golfmate

import java.time.Duration
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class TeeTimeTest {
    private val start = ZonedDateTime.parse("2026-11-16T00:30:00+09:00")
    private val now = start.minusMinutes(60).toInstant()
    private val course = GolfDestination("a", "한양CC", 10000, 1800, now)
    private val tee = TeeTime(start)

    @Test fun comparesArrivalAcrossMidnightAndThresholds() {
        val onTime = estimateTeeTime(tee, course, now)
        assertEquals(ArrivalStatus.OnTime, onTime.status)
        assertEquals(start.minusMinutes(30).toInstant(), onTime.arrival)
        assertEquals(Duration.ofMinutes(60), onTime.untilTeeTime)
        assertEquals(Duration.ZERO, onTime.preparationMargin)
        assertEquals(ArrivalStatus.ShortPreparation, estimateTeeTime(
            tee, course.copy(remainingTime = 3600), now
        ).status)
        assertEquals(ArrivalStatus.Late, estimateTeeTime(
            tee, course.copy(remainingTime = 3601), now
        ).status)
    }

    @Test fun keepsSnapshotArrivalAndRejectsMissingOrStaleData() {
        assertEquals(estimateTeeTime(tee, course, now).arrival,
            estimateTeeTime(tee, course, now.plusSeconds(60)).arrival)
        for (destination in listOf(null, course.copy(remainingTime = 0),
            course.copy(receivedAt = now.minusSeconds(301)), course.copy(receivedAt = null))) {
            val estimate = estimateTeeTime(tee, destination, now)
            assertEquals(ArrivalStatus.Unknown, estimate.status)
            assertNull(estimate.arrival)
        }
    }

    @Test fun retainsTeeTimeOnlyForCurrentGolfSessionAndIncludesItInBriefing() {
        val home = GolfModeState().update(course).accept().copy(teeTime = tee)
        assertEquals(tee, home.update(course.copy(remainingTime = 2000)).teeTime)
        assertNull(home.update(course.copy(key = "b")).teeTime)
        assertNull(home.update(null).teeTime)
        val briefing = briefingText(course, home.checklist, tee, now)
        assertTrue(briefing.contains("티타임"))
        assertTrue(briefing.contains("정상 도착 예상"))
    }
}
