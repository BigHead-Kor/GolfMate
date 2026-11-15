package com.selfbell.golfmate

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val teeTimeFormat = DateTimeFormatter.ofPattern("M월 d일 HH:mm")

@Composable
internal fun TeeTimeHome(
    destination: GolfDestination,
    teeTime: TeeTime?,
    now: Instant,
    onRefresh: () -> Unit,
    onSave: (TeeTime) -> Unit
) {
    var editing by remember(destination.key) { mutableStateOf(false) }
    val estimate = teeTime?.let { estimateTeeTime(it, destination, now) }
    val zone = teeTime?.start?.zone ?: ZoneId.systemDefault()
    val arrival = estimateArrival(destination, now)
    GolfPanel {
        Text(destination.name, style = MaterialTheme.typography.titleLarge)
        val metrics = listOf(
            "티타임" to (teeTime?.start?.format(teeTimeFormat) ?: "미설정"),
            "예상 도착" to (arrival?.atZone(zone)?.format(teeTimeFormat) ?: "정보 없음"),
            "목표 도착" to (estimate?.target?.atZone(zone)?.format(teeTimeFormat) ?: "—")
        )
        BoxWithConstraints {
            if (maxWidth >= 620.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    metrics.forEach { (label, value) -> TeeMetric(label, value, Modifier.weight(1f)) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    metrics.forEach { (label, value) -> TeeMetric(label, value, Modifier.fillMaxWidth()) }
                }
            }
        }
    }
    GolfPanel {
        if (teeTime != null && estimate != null) {
                val statusColor = when (estimate.status) {
                    ArrivalStatus.ShortPreparation -> Color(0xFFFFB36E)
                    ArrivalStatus.Late -> MaterialTheme.colorScheme.error
                    ArrivalStatus.OnTime -> MaterialTheme.colorScheme.primary
                    ArrivalStatus.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(estimate.status.label, color = statusColor, style = MaterialTheme.typography.titleLarge)
                estimate.preparationMargin?.let { margin ->
                    val minutes = margin.abs().toMinutes()
                    val amount = if (minutes == 0L && !margin.isZero) "1분 미만" else "${minutes}분"
                    Text(if (margin.isNegative) "목표 도착보다 $amount 늦어요" else "준비 여유 $amount")
                }
                Text("목표 준비 시간 · ${teeTime.preparationMinutes}분")
                val remaining = estimate.untilTeeTime
                Text(if (remaining.isNegative) "티타임이 지났어요"
                    else "티타임까지 ${timeLabel(remaining.seconds.coerceAtLeast(1))}")
                Text(
                    if (estimate.arrival != null) {
                        "마지막 내비 조회 · ${destination.receivedAt?.atZone(teeTime.start.zone)?.format(teeTimeFormat)}"
                    } else "최근 내비 정보가 없어요. 목적지를 다시 조회해 주세요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { editing = true }, modifier = Modifier.heightIn(min = 72.dp)) {
                    Text(if (teeTime == null) "티타임 설정" else "티타임 수정")
                }
                if (teeTime != null) {
                    OutlinedButton(onClick = onRefresh, modifier = Modifier.heightIn(min = 72.dp)) {
                        Text("목적지 조회")
                    }
                }
            }
    }
    if (editing) {
        TeeTimeEditor(teeTime, onDismiss = { editing = false }) {
            onSave(it)
            editing = false
        }
    }
}

@Composable
private fun TeeTimeEditor(teeTime: TeeTime?, onDismiss: () -> Unit, onSave: (TeeTime) -> Unit) {
    val context = LocalContext.current
    var start by remember { mutableStateOf(teeTime?.start ?: ZonedDateTime.now(ZoneId.systemDefault()).withSecond(0).withNano(0)) }
    var preparation by remember { mutableIntStateOf(teeTime?.preparationMinutes ?: 30) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("티타임 설정") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("정차 중에 라운드 날짜와 시간을 설정해 주세요.")
                OutlinedButton(onClick = {
                    DatePickerDialog(context, { _, year, month, day ->
                        start = java.time.LocalDate.of(year, month + 1, day)
                            .atTime(start.toLocalTime()).atZone(start.zone)
                    }, start.year, start.monthValue - 1, start.dayOfMonth).show()
                }, modifier = Modifier.heightIn(min = 72.dp)) {
                    Text("라운드 날짜 · ${start.toLocalDate()}")
                }
                OutlinedButton(onClick = {
                    TimePickerDialog(context, { _, hour, minute ->
                        start = start.withHour(hour).withMinute(minute)
                    }, start.hour, start.minute, true).show()
                }, modifier = Modifier.heightIn(min = 72.dp)) {
                    Text("티타임 · ${start.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                }
                Text("목표 준비 시간")
                listOf(listOf(20, 30), listOf(40, 60)).forEach { options ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { minutes ->
                            FilterChip(selected = minutes == preparation, onClick = { preparation = minutes },
                                label = { Text("${minutes}분") }, modifier = Modifier.heightIn(min = 48.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(TeeTime(start, preparation)) }) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
private fun TeeMetric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = GolfColors.secondaryContainer) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = GolfColors.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.displaySmall, color = GolfColors.primary)
        }
    }
}
