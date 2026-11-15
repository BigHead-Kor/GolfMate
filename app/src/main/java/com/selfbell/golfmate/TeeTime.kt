package com.selfbell.golfmate

import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

internal data class TeeTime(val start: ZonedDateTime, val preparationMinutes: Int = 30)

internal enum class ArrivalStatus(val label: String) {
    OnTime("정상 도착 예상"), ShortPreparation("준비 시간 부족"),
    Late("지각 예상"), Unknown("도착 예측 불가")
}

internal data class TeeTimeEstimate(
    val arrival: Instant?,
    val target: Instant,
    val untilTeeTime: Duration,
    val preparationMargin: Duration?,
    val status: ArrivalStatus
)

internal fun estimateTeeTime(
    teeTime: TeeTime,
    destination: GolfDestination?,
    now: Instant
): TeeTimeEstimate {
    val start = teeTime.start.toInstant()
    val target = start.minusSeconds(teeTime.preparationMinutes * 60L)
    val arrival = estimateArrival(destination, now)

    val status = when {
        arrival == null -> ArrivalStatus.Unknown
        arrival.isAfter(start) -> ArrivalStatus.Late
        arrival.isAfter(target) -> ArrivalStatus.ShortPreparation
        else -> ArrivalStatus.OnTime
    }
    return TeeTimeEstimate(
        arrival, target, Duration.between(now, start),
        arrival?.let { Duration.between(it, target) }, status
    )
}

internal fun estimateArrival(destination: GolfDestination?, now: Instant): Instant? {
    val received = destination?.receivedAt
    return if (received != null && destination.remainingTime > 0 &&
        !received.isAfter(now) && Duration.between(received, now) <= Duration.ofMinutes(5)
    ) received.plusSeconds(destination.remainingTime).takeUnless { it.isBefore(now) } else null

}
