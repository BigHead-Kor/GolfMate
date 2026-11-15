package com.selfbell.golfmate

import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.math.RoundingMode
import java.util.Locale

private val DashboardTime = DateTimeFormatter.ofPattern("M/d HH:mm")

@Composable
internal fun RoundDashboard(
    state: GolfModeState,
    destination: GolfDestination,
    now: Instant,
    wide: Boolean,
    artHeight: androidx.compose.ui.unit.Dp,
    tileHeight: androidx.compose.ui.unit.Dp,
    onTeeTime: () -> Unit,
    onChecklist: () -> Unit,
    onBriefing: () -> Unit,
    onSettlement: () -> Unit
) {
    val tee = state.teeTime
    val estimate = tee?.let { estimateTeeTime(it, destination, now) }
    val zone = tee?.start?.zone ?: java.time.ZoneId.systemDefault()
    val arrival = estimateArrival(destination, now)
    val arrivalLabel = arrival?.atZone(zone)?.format(DashboardTime) ?: "정보 없음"
    val status = estimate?.status
    val statusColor = when (status) {
        ArrivalStatus.OnTime -> GolfColors.primary
        ArrivalStatus.ShortPreparation -> Color(0xFFFFB36E)
        ArrivalStatus.Late -> GolfColors.error
        else -> GolfColors.onSurfaceVariant
    }
    val expenses = estimateSettlement(state.settlement, destination.routeEstimate)
    val cost = expenses.perPerson?.let {
        NumberFormat.getIntegerInstance(Locale.KOREA).format(it.setScale(0, RoundingMode.HALF_UP)) + "원"
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            onClick = onTeeTime,
            modifier = Modifier.fillMaxWidth().heightIn(min = 214.dp),
            shape = GolfCardShape,
            color = GolfColors.surface
        ) {
            Column {
                CourseArtwork(Modifier.fillMaxWidth().height(artHeight))
                Column(
                    Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF2B4332), GolfColors.surface)))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                ) {
                    Text("라운드 목적지", color = GolfColors.secondary)
                    Text(
                        destination.name,
                        style = if (wide) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineMedium
                    )
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RoundMetric("남은 거리", distanceLabel(destination.remainingDistance), Modifier.weight(1f))
                            RoundMetric("예상 도착", arrivalLabel, Modifier.weight(1f))
                            RoundMetric("티타임", tee?.start?.format(DashboardTime) ?: "설정하기", Modifier.weight(1f), true)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RoundMetric("남은 거리", distanceLabel(destination.remainingDistance), Modifier.weight(1f))
                            RoundMetric("예상 도착", arrivalLabel, Modifier.weight(1f))
                        }
                        RoundMetric("티타임", tee?.start?.format(DashboardTime) ?: "설정하기", accent = true)
                    }
                    Surface(color = GolfColors.background.copy(alpha = 0.58f), shape = MaterialTheme.shapes.medium) {
                        Text(
                            status?.label ?: "티타임을 설정해 도착 상태를 확인하세요",
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = statusColor
                        )
                    }
                }
            }
        }
        val cards: @Composable (Modifier) -> Unit = { tile ->
            DashboardTile("준비물 체크", "${state.checklist.count}/${state.checklist.total}",
                "확인한 준비물", tile, onChecklist) {
                Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF496052), GolfCardShape)) {
                    if (state.checklist.count > 0) {
                        Box(Modifier.fillMaxWidth(state.checklist.progress).fillMaxHeight()
                            .background(GolfColors.primary, GolfCardShape))
                    }
                }
            }
            DashboardTile("골프 브리핑", "음성 브리핑", "경로와 라운드 준비 안내", tile, onBriefing)
            DashboardTile("라운드 정산", cost ?: "예상 비용 계산",
                if (cost != null) "${state.settlement.people}인 기준 · 1인 예상 비용" else "유류비와 통행료", tile, onSettlement)
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { cards(Modifier.weight(1f).heightIn(min = tileHeight)) }
        } else {
            cards(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun RoundMetric(label: String, value: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = GolfColors.onSurfaceVariant)
        Text(value, style = if (value.firstOrNull()?.isDigit() == true) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = if (accent) GolfColors.primary else GolfColors.onSurface)
    }
}

@Composable
private fun DashboardTile(
    label: String,
    value: String,
    detail: String,
    modifier: Modifier,
    onClick: () -> Unit,
    extra: @Composable () -> Unit = {}
) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 172.dp),
        color = GolfColors.surface, shape = GolfCardShape) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
            DashboardIcon(label)
            Text(label, style = MaterialTheme.typography.titleMedium, color = GolfColors.secondary)
            Text(value, style = if (value.firstOrNull()?.isDigit() == true) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = GolfColors.primary)
            extra()
            Text(detail, color = GolfColors.onSurfaceVariant)
        }
    }
}

@Composable
internal fun CourseArtwork(modifier: Modifier) {
    Canvas(modifier.clip(GolfCardShape).then(Modifier.background(Brush.verticalGradient(
        listOf(Color(0xFF122831), Color(0xFF496353), Color(0xFFA48252))
    ), GolfCardShape))) {
        val w = size.width
        val h = size.height
        drawCircle(Color(0xFFFFD78C), h * .12f, Offset(w * .79f, h * .28f))
        drawOval(Color(0xFF253E33), Offset(-w * .2f, h * .43f), Size(w * 1.4f, h))
        drawOval(Color(0xFF34533B), Offset(w * .12f, h * .56f), Size(w, h))
        drawOval(Color(0xFF477047), Offset(-w * .14f, h * .70f), Size(w, h))
        drawOval(Color(0xFF759255), Offset(w * .34f, h * .74f), Size(w * .44f, h * .21f))
        drawOval(Color(0xFFD4B78A), Offset(w * .68f, h * .66f), Size(w * .15f, h * .09f))
        val pole = Offset(w * .56f, h * .53f)
        drawLine(GolfColors.secondary, pole, Offset(pole.x, h * .86f), 2.dp.toPx())
        val flag = androidx.compose.ui.graphics.Path().apply {
            moveTo(pole.x, pole.y)
            lineTo(pole.x + h * .17f, pole.y + h * .05f)
            lineTo(pole.x, pole.y + h * .11f)
            close()
        }
        drawPath(flag, GolfColors.primary)
    }
}

@Composable
private fun DashboardIcon(label: String) {
    val tint = GolfColors.primary
    Canvas(Modifier.size(32.dp)) {
        val u = size.width / 32
        fun point(x: Int, y: Int) = Offset(x * u, y * u)
        fun line(x: Int, y: Int, endX: Int, endY: Int) {
            drawLine(tint, point(x, y), point(endX, endY), 2.5f * u, StrokeCap.Round)
        }
        when (label) {
            "준비물 체크" -> {
                drawRoundRect(tint, point(5, 5), Size(22 * u, 24 * u),
                    androidx.compose.ui.geometry.CornerRadius(3 * u),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f * u))
                line(11, 5, 21, 5)
                line(10, 16, 14, 20)
                line(14, 20, 22, 12)
            }
            "골프 브리핑" -> {
                listOf(5, 11, 16, 11, 5).forEachIndexed { index, height ->
                    val x = 4 + index * 6
                    line(x, 16 - height / 2, x, 16 + height / 2)
                }
            }
            else -> {
                drawRoundRect(tint, point(3, 7), Size(26 * u, 19 * u),
                    androidx.compose.ui.geometry.CornerRadius(3 * u),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f * u))
                line(4, 12, 27, 12)
                line(21, 19, 26, 19)
            }
        }
    }
}
