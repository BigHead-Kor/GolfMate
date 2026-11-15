package com.selfbell.golfmate

internal enum class PreparationItem(val label: String) {
    GolfBag("골프백"),
    Shoes("골프화"),
    Glove("골프장갑"),
    Balls("골프공"),
    Rangefinder("거리측정기"),
    Hat("모자"),
    BostonBag("보스턴백"),
    Sunscreen("선크림")
}

internal data class PreparationChecklist(
    val checked: Set<PreparationItem> = emptySet()
) {
    val total: Int get() = PreparationItem.entries.size
    val count: Int get() = checked.size
    val progress: Float get() = count.toFloat() / total
    val isComplete: Boolean get() = count == total

    fun toggle(item: PreparationItem): PreparationChecklist =
        copy(checked = if (item in checked) checked - item else checked + item)
}
