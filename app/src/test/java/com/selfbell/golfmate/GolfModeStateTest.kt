package com.selfbell.golfmate

import org.junit.Assert.assertEquals
import org.junit.Test

class GolfModeStateTest {
    private val course = GolfDestination("course", "솔라고컨트리클럽", 139000, 6600)
    private val restaurant = GolfDestination("restaurant", "맥도날드", 2000, 300)

    @Test
    fun offersOnlyForGolfCoursesAndRetainsAcceptanceAcrossRefreshes() {
        val waiting = GolfModeState()
        assertEquals(GolfModeScreen.Waiting, waiting.screen)
        assertEquals(GolfModeScreen.Waiting, waiting.update(restaurant).screen)
        val offer = waiting.update(course)
        assertEquals(GolfModeScreen.Offer, offer.screen)
        val refreshed = course.copy(remainingTime = 6500)
        val home = offer.accept().update(refreshed)
        assertEquals(GolfModeScreen.Home, home.screen)
        assertEquals(refreshed, home.destination)
        assertEquals(GolfModeScreen.Waiting, home.update(restaurant).screen)
        assertEquals(GolfModeScreen.Waiting, home.update(null).screen)
    }

    @Test
    fun suppressesRejectedProposalUntilTheDestinationChangesOrIsCancelled() {
        val declined = GolfModeState().update(course).decline()
        assertEquals(GolfModeScreen.Waiting, declined.update(course.copy(remainingDistance = 120000)).screen)
        assertEquals(GolfModeScreen.Offer, declined.update(course.copy(key = "another-course")).screen)
        assertEquals(GolfModeScreen.Offer, declined.update(null).update(course).screen)
    }
}
