package com.selfbell.golfmate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun SettlementContent(
    destination: GolfDestination,
    settings: SettlementSettings,
    wide: Boolean,
    panelHeight: androidx.compose.ui.unit.Dp,
    onChange: (SettlementSettings) -> Unit
) {
    val route = destination.routeEstimate
    val estimate = estimateSettlement(settings, route)
    Text("${destination.name} · 예상 교통비", style = MaterialTheme.typography.titleMedium)
    Text("입력은 정차 중에 해 주세요. 왕복은 귀가 시 같은 경로를 이용한다고 가정합니다.")
    val summary: @Composable (Modifier) -> Unit = { modifier ->
        GolfPanel(modifier) {
            Column(Modifier.fillMaxWidth().padding(vertical = if (wide) (panelHeight * 0.07f).coerceIn(16.dp, 52.dp) else 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1인당 예상 교통비", style = MaterialTheme.typography.titleLarge)
                Text(won(estimate.perPerson), style = MaterialTheme.typography.displaySmall,
                    color = GolfColors.primary, textAlign = TextAlign.Center)
                Text("${if (settings.roundTrip) "왕복" else "편도"} · ${settings.people}인 기준")
            }
            HorizontalDivider()
            SettlementResult("예상 이동거리", estimate.distance?.let { "${it.stripTrailingZeros().toPlainString()} km" } ?: "미확인")
            SettlementResult("예상 유류비", won(estimate.fuel))
            SettlementResult("예상 통행료", won(estimate.toll))
            val total = estimate.total
            val fuel = estimate.fuel
            if (total != null && fuel != null && total.signum() > 0) {
                LinearProgressIndicator(
                    progress = { (fuel.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = GolfColors.primary, trackColor = GolfColors.secondary
                )
            }
            HorizontalDivider()
            SettlementResult("예상 총 교통비", won(total), emphasized = true)
            Text("실제 결제 금액은 달라질 수 있어요.", color = GolfColors.onSurfaceVariant)
        }
    }
    val conditions: @Composable (Modifier) -> Unit = { modifier ->
        GolfPanel(modifier) {
            Text("이동 조건", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(selected = !settings.roundTrip, onClick = { onChange(settings.copy(roundTrip = false)) },
                    label = { Text("편도", style = MaterialTheme.typography.titleMedium) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GolfColors.primary, selectedLabelColor = GolfColors.onPrimary),
                    modifier = Modifier.weight(1f).heightIn(min = 72.dp))
                FilterChip(selected = settings.roundTrip, onClick = { onChange(settings.copy(roundTrip = true)) },
                    label = { Text("왕복", style = MaterialTheme.typography.titleMedium) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GolfColors.primary, selectedLabelColor = GolfColors.onPrimary),
                    modifier = Modifier.weight(1f).heightIn(min = 72.dp))
            }
            Text("정산 참여 인원")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..4).forEach { people ->
                    FilterChip(selected = settings.people == people, onClick = { onChange(settings.copy(people = people)) },
                        label = { Text("${people}명", style = MaterialTheme.typography.titleMedium) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GolfColors.primary, selectedLabelColor = GolfColors.onPrimary),
                    modifier = Modifier.weight(1f).heightIn(min = 72.dp))
                }
            }
            val fields: List<@Composable (Modifier) -> Unit> = listOf(
                { field -> SettlementField("편도 거리 (km)", settings.distance,
                    route?.distanceKm?.let { "내비 자동 · ${it.stripTrailingZeros().toPlainString()} km" }
                        ?: "내비 거리 미확인",
                    settlementNumber(settings.distance) != null, field, settings.distance.isNotBlank()
                ) { onChange(settings.copy(distance = it)) } },
                { field -> SettlementField("통행료 (원)", settings.toll,
                    route?.toll?.let { "내비 자동 · ${won(it)}" } ?: "미확인 · 무료면 0 입력",
                    settlementNumber(settings.toll, allowZero = true)?.stripTrailingZeros()?.scale()?.let { it <= 0 } == true,
                    field, settings.toll.isNotBlank()
                ) { onChange(settings.copy(toll = it)) } },
                { field -> SettlementField("연비 (km/L)", settings.efficiency, "직접 입력",
                    settlementNumber(settings.efficiency) != null, field
                ) { onChange(settings.copy(efficiency = it)) } },
                { field -> SettlementField("유류 단가 (원/L)", settings.fuelPrice, "직접 입력",
                    settlementNumber(settings.fuelPrice) != null, field
                ) { onChange(settings.copy(fuelPrice = it)) } }
            )
            BoxWithConstraints {
                val twoColumns = maxWidth >= 330.dp
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    fields.chunked(if (twoColumns) 2 else 1).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { field -> field(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
    if (wide) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            summary(Modifier.weight(1f).heightIn(min = panelHeight))
            conditions(Modifier.weight(1f).heightIn(min = panelHeight))
        }
    } else {
        summary(Modifier.fillMaxWidth())
        conditions(Modifier.fillMaxWidth())
    }
}

@Composable
private fun SettlementField(
    label: String, value: String, hint: String, valid: Boolean, modifier: Modifier,
    manualOverride: Boolean = false, onChange: (String) -> Unit
) {
    val invalid = value.isNotBlank() && !valid
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        supportingText = { Text(if (invalid) "유효한 금액 또는 수치를 입력해 주세요." else if (manualOverride) "수동 보정 적용" else hint, style = MaterialTheme.typography.bodySmall) },
        isError = invalid, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.heightIn(min = 84.dp),
        textStyle = MaterialTheme.typography.bodyLarge
    )
}

private fun won(value: BigDecimal?): String = value?.let {
    NumberFormat.getIntegerInstance(Locale.KOREA).format(it.setScale(0, RoundingMode.HALF_UP)) + "원"
} ?: "미확인"

@Composable
private fun SettlementResult(label: String, value: String, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Text(value, Modifier.weight(1f), textAlign = TextAlign.End,
            style = if (emphasized) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge)
    }
}
