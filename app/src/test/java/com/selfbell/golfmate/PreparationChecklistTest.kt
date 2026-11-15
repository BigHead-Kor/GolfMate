package com.selfbell.golfmate

import org.junit.Assert.*
import org.junit.Test

class PreparationChecklistTest {
    private val course = GolfDestination("a", "한양CC", 10000, 600)

    @Test
    fun tracksCompletionAndUnchecking() {
        val initial = PreparationChecklist()
        assertEquals(8, initial.total)
        assertEquals(0f, initial.progress, 0f)
        val complete = PreparationItem.entries.fold(initial) { list, item -> list.toggle(item) }
        assertEquals(8, complete.count)
        assertEquals(1f, complete.progress, 0f)
        assertTrue(complete.isComplete)
        val unchecked = complete.toggle(PreparationItem.Shoes)
        assertEquals(7, unchecked.count)
        assertEquals(0.875f, unchecked.progress, 0f)
        assertFalse(unchecked.isComplete)
    }

    @Test
    fun retainsChecksDuringSessionAndResetsOnNewRoute() {
        val offer = GolfModeState().update(course)
        assertFalse(offer.openChecklist().checklistOpen)
        val home = offer.accept().openChecklist().toggleItem(PreparationItem.Hat).closeChecklist()
        assertFalse(home.checklistOpen)
        val reopened = home.update(course.copy(remainingTime = 500)).openChecklist()
        assertTrue(reopened.checklistOpen)
        assertEquals(setOf(PreparationItem.Hat), reopened.checklist.checked)
        val cancelled = reopened.update(null)
        assertFalse(cancelled.checklistOpen)
        assertEquals(0, cancelled.update(course).accept().openChecklist().checklist.count)
        val changed = reopened.update(course.copy(key = "b", name = "스카이72GC"))
        assertFalse(changed.checklistOpen)
        assertEquals(0, changed.accept().openChecklist().checklist.count)
    }
}
