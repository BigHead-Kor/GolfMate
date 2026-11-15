package com.selfbell.golfmate

import org.junit.Assert.*
import org.junit.Test

class GolfBriefingTest {
    @Test
    fun usesCurrentDestinationAndOnlyUncheckedItems() {
        val destination = GolfDestination("a", "한양CC", 139000, 6540)
        val checklist = PreparationChecklist(PreparationItem.entries.toSet() - PreparationItem.Glove)
        val text = briefingText(destination, checklist)
        assertTrue(text.contains("한양CC"))
        assertTrue(text.contains("139 킬로미터"))
        assertTrue(text.contains("1시간 49분"))
        assertTrue(text.contains("골프장갑"))
        assertFalse(text.contains("골프백"))
    }

    @Test
    fun omitsUnavailableFieldsAndReportsCompletion() {
        val destination = GolfDestination("a", "", 0, -1)
        val checklist = PreparationChecklist(PreparationItem.entries.toSet())
        assertEquals("모든 준비물을 확인했습니다.", briefingText(destination, checklist))
    }
}
