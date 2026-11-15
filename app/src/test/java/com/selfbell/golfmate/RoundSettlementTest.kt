package com.selfbell.golfmate

import java.math.RoundingMode
import org.junit.Assert.*
import org.junit.Test

class RoundSettlementTest {
    private val route = RouteEstimate(139000, 9800)
    private val settings = SettlementSettings(efficiency = "12", fuelPrice = "1700", people = 4)

    @Test fun estimatesOneWayAndRoundTrip() {
        assertEquals("29492", estimateSettlement(settings, route).total?.setScale(0, RoundingMode.HALF_UP).toString())
        val round = estimateSettlement(settings.copy(roundTrip = true), route)
        assertEquals("58983", round.total?.setScale(0, RoundingMode.HALF_UP).toString())
        assertEquals("14746", round.perPerson?.setScale(0, RoundingMode.HALF_UP).toString())
    }

    @Test fun unknownTollAndManualOverrides() {
        assertNull(estimateSettlement(settings, route.copy(tollWon = 0)).total)
        val manual = estimateSettlement(settings.copy(distance = "12", toll = "0"), null)
        assertEquals("1700", manual.total?.setScale(0).toString())
        assertNull(estimateSettlement(settings.copy(distance = "-1"), route).total)
        assertNull(estimateSettlement(settings.copy(efficiency = "0"), route).fuel)
        assertNull(estimateSettlement(settings.copy(people = 0), route).perPerson)
    }

    @Test fun checksRouteMatchAndSessionReset() {
        val destination = GolfDestination("a", "한양CC", 139000, 6600)
        val candidate = RouteCandidate("한양CC", route)
        assertTrue(candidate.matches(destination))
        assertFalse(candidate.matches(destination.copy(name = "다른 골프장")))
        assertFalse(candidate.matches(destination.copy(remainingDistance = 138000)))
        val home = GolfModeState().update(destination).accept().copy(settlement = settings)
        assertEquals(settings, home.update(destination.copy(remainingDistance = 100000)).settlement)
        assertEquals(SettlementSettings(), home.update(destination.copy(key = "b")).settlement)
        assertEquals(SettlementSettings(), home.update(null).settlement)
    }
}
