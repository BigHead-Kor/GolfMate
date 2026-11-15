package com.selfbell.golfmate

import java.text.NumberFormat
import java.util.Locale

// Pleos 시연 경로 응답을 기준으로 잔여 거리는 m, 잔여 시간은 s로 해석한다.
internal fun distanceLabel(meters: Long): String = when {
    meters <= 0 -> "정보 없음"
    meters < 1000 -> "$meters m"
    else -> {
        val format = NumberFormat.getNumberInstance(Locale.KOREA).apply { maximumFractionDigits = 1 }
        "${format.format(meters / 1000.0)} km"
    }
}

internal fun timeLabel(seconds: Long): String {
    if (seconds <= 0) return "정보 없음"
    if (seconds < 60) return "1분 미만"
    val minutes = seconds / 60
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours == 0L -> "${minutes}분"
        remainder == 0L -> "${hours}시간"
        else -> "${hours}시간 ${remainder}분"
    }
}

internal fun briefingText(destination: GolfDestination, checklist: PreparationChecklist,
    teeTime: TeeTime? = null,
    now: java.time.Instant = java.time.Instant.now()
): String =
    buildList {
        if (destination.name.isNotBlank()) add("오늘의 목적지는 ${destination.name}입니다.")
        if (destination.remainingDistance > 0) {
            val distance = distanceLabel(destination.remainingDistance)
                .replace(" km", " 킬로미터").replace(" m", " 미터")
            add("남은 거리는 ${distance}입니다.")
        }
        if (destination.remainingTime > 0) {
            add("예상 소요시간은 ${timeLabel(destination.remainingTime)}입니다.")
        }
        if (teeTime != null) {
            val estimate = estimateTeeTime(teeTime, destination, now)
            add("티타임은 ${teeTime.start.format(java.time.format.DateTimeFormatter.ofPattern("M월 d일 H시 mm분"))}입니다.")
            val arrival = estimate.arrival?.atZone(teeTime.start.zone)
            if (arrival != null) {
                add("마지막 내비게이션 조회 기준 예상 도착은 ${arrival.format(java.time.format.DateTimeFormatter.ofPattern("M월 d일 H시 mm분"))}입니다. ${estimate.status.label}입니다.")
            } else {
                add("최근 내비게이션 정보가 없어 도착 시간을 예측할 수 없습니다.")
            }
        }
        val remaining = PreparationItem.entries.filterNot { it in checklist.checked }
        add(if (remaining.isEmpty()) "모든 준비물을 확인했습니다."
            else "아직 확인하지 않은 준비물은 ${remaining.joinToString { it.label }}입니다.")
    }.joinToString(" ")
