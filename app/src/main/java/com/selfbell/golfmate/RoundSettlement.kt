package com.selfbell.golfmate

import java.math.BigDecimal
import java.math.RoundingMode

internal data class RouteEstimate(val distanceMeters: Long, val tollWon: Long) {
    val distanceKm: BigDecimal? get() = distanceMeters.takeIf { it > 0 }?.let { BigDecimal.valueOf(it, 3) }
    val toll: BigDecimal? get() = tollWon.takeIf { it > 0 }?.let { BigDecimal.valueOf(it) }
}

internal data class SettlementSettings(
    val distance: String = "",
    val toll: String = "",
    val efficiency: String = "",
    val fuelPrice: String = "",
    val people: Int = 1,
    val roundTrip: Boolean = false
)

internal data class SettlementEstimate(
    val distance: BigDecimal?,
    val fuel: BigDecimal?,
    val toll: BigDecimal?,
    val total: BigDecimal?,
    val perPerson: BigDecimal?
)

internal fun settlementNumber(text: String, allowZero: Boolean = false): BigDecimal? =
    text.trim().takeIf { it.matches(Regex("[0-9]{1,9}(\\.[0-9]{1,3})?")) }
        ?.toBigDecimalOrNull()?.takeIf { if (allowZero) it.signum() >= 0 else it.signum() > 0 }

internal fun estimateSettlement(settings: SettlementSettings, route: RouteEstimate?): SettlementEstimate {
    val multiplier = BigDecimal.valueOf(if (settings.roundTrip) 2 else 1)
    val distance = (if (settings.distance.isBlank()) route?.distanceKm
        else settlementNumber(settings.distance))?.multiply(multiplier)
    val toll = (if (settings.toll.isBlank()) route?.toll
        else settlementNumber(settings.toll, allowZero = true)?.takeIf { it.stripTrailingZeros().scale() <= 0 })
        ?.multiply(multiplier)
    val efficiency = settlementNumber(settings.efficiency)
    val fuelPrice = settlementNumber(settings.fuelPrice)
    val fuel = if (distance != null && efficiency != null && fuelPrice != null) {
        distance.multiply(fuelPrice).divide(efficiency, 8, RoundingMode.HALF_UP)
    } else null
    val total = if (fuel != null && toll != null) fuel + toll else null
    return SettlementEstimate(distance, fuel, toll, total,
        total?.let { amount -> settings.people.takeIf { it > 0 }?.let {
            amount.divide(BigDecimal.valueOf(it.toLong()), 8, RoundingMode.HALF_UP)
        } })
}

internal data class RouteCandidate(val name: String, val estimate: RouteEstimate) {
    fun matches(destination: GolfDestination): Boolean =
        name.isNotBlank() && name.equals(destination.name, ignoreCase = true) &&
            estimate.distanceMeters > 0 && destination.remainingDistance == estimate.distanceMeters
}
