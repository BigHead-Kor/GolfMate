package com.selfbell.golfmate

internal data class GolfDestination(
    val key: String,
    val name: String,
    val remainingDistance: Long,
    val remainingTime: Long,
    val receivedAt: java.time.Instant? = null,
    val routeEstimate: RouteEstimate? = null
)

internal enum class GolfModeScreen { Waiting, Offer, Home }

internal data class GolfModeState(
    val destination: GolfDestination? = null,
    val decision: GolfModeScreen? = null,
    val checklist: PreparationChecklist = PreparationChecklist(),
    val checklistOpen: Boolean = false,
    val teeTime: TeeTime? = null,
    val settlement: SettlementSettings = SettlementSettings()
) {
    val screen: GolfModeScreen
        get() = if (destination?.let { isGolfCourse(it.name) } == true) {
            decision ?: GolfModeScreen.Offer
        } else {
            GolfModeScreen.Waiting
        }

    fun update(next: GolfDestination?): GolfModeState =
        if (next != null && next.key == destination?.key) {
            val updated = copy(destination = next)
            if (updated.screen == GolfModeScreen.Home) updated
            else GolfModeState(next, decision)
        } else {
            GolfModeState(next)
        }

    fun openChecklist(): GolfModeState =
        if (screen == GolfModeScreen.Home) copy(checklistOpen = true) else this

    fun closeChecklist(): GolfModeState = copy(checklistOpen = false)

    fun toggleItem(item: PreparationItem): GolfModeState =
        if (screen == GolfModeScreen.Home && checklistOpen) copy(checklist = checklist.toggle(item))
        else this

    fun accept(): GolfModeState =
        if (screen == GolfModeScreen.Offer) copy(decision = GolfModeScreen.Home) else this

    fun decline(): GolfModeState =
        if (screen == GolfModeScreen.Offer) copy(decision = GolfModeScreen.Waiting) else this
}
