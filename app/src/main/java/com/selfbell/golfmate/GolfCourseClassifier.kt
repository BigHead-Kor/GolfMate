package com.selfbell.golfmate

private val nonCourseFacility = Regex(
    """연습장|연습센터|스크린|용품|아카데미|레슨|식당|카페|주차장|카센터|정비소|대리점|driving\s+range|golf\s+(shop|store|academy)|screen\s+golf""",
    RegexOption.IGNORE_CASE
)
private val courseName = Regex(
    """컨트리\s*클럽|골프\s*(클럽|장)|\b(?:country\s+club|golf\s+(?:club|course))\b""",
    RegexOption.IGNORE_CASE
)
private val courseSuffix = Regex(
    """(?<![A-Za-z])(?:C\.?C|G\.?C)\.?\s*\)?$""",
    RegexOption.IGNORE_CASE
)

internal fun isGolfCourse(name: String): Boolean {
    val destinationName = name.trim()
    return destinationName.isNotEmpty() &&
        !nonCourseFacility.containsMatchIn(destinationName) &&
        (courseName.containsMatchIn(destinationName) || courseSuffix.containsMatchIn(destinationName))
}
