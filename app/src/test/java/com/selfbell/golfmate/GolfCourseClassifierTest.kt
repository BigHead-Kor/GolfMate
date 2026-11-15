package com.selfbell.golfmate

import org.junit.Assert.assertEquals
import org.junit.Test

class GolfCourseClassifierTest {
    @Test
    fun recognizesCourseNamesAndRejectsUnrelatedFacilities() {
        val examples = mapOf(
            "솔라고컨트리클럽" to true,
            "한양CC" to true,
            "스카이72GC" to true,
            "  서울 골프장  " to true,
            "테스트 C.C." to true,
            "Pine Golf Club" to true,
            "서울역" to false,
            "숭실대학교" to false,
            "서울식당" to false,
            "" to false,
            "  " to false,
            "골프용품점" to false,
            "한양CC 골프연습장" to false,
            "서울골프장 주차장" to false,
            "ABC GOLF" to false,
            "GUCCI" to false,
            "ABCACC" to false,
            "GC 식당" to false
        )

        examples.forEach { (name, expected) ->
            assertEquals("목적지 이름 [$name]", expected, isGolfCourse(name))
        }
    }
}
