package com.selfbell.golfmate

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.produceState
import java.time.Instant
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.key
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer

@Composable
internal fun GolfModeContent(
    state: GolfModeState,
    connectionStatus: String,
    isConnected: Boolean,
    onRefresh: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenChecklist: () -> Unit,
    onCloseChecklist: () -> Unit,
    onToggleItem: (PreparationItem) -> Unit,
    speech: BriefingSpeech,
    onSaveTeeTime: (String, TeeTime) -> Unit,
    onSettlementChange: (String, SettlementSettings) -> Unit
) {
    var briefingDestination by remember {
        mutableStateOf<String?>(null)
    }
    val briefingOpen = state.screen == GolfModeScreen.Home &&
        briefingDestination != null && briefingDestination == state.destination?.key
    if (briefingOpen) {
        DisposableEffect(briefingDestination) {
            onDispose { speech.stop() }
        }
    } else if (briefingDestination != null) {
        LaunchedEffect(briefingDestination) { briefingDestination = null }
    }
    var settlementKey by remember(state.screen, state.destination?.key) { mutableStateOf<String?>(null) }
    val settlementOpen = settlementKey != null && state.screen == GolfModeScreen.Home
    var teeTimeOpen by remember(state.screen, state.destination?.key) { mutableStateOf(false) }
    BackHandler(enabled = teeTimeOpen) { teeTimeOpen = false }
    BackHandler(enabled = settlementOpen) { settlementKey = null }
    BackHandler(enabled = briefingOpen) { briefingDestination = null }
    BackHandler(enabled = state.checklistOpen, onBack = onCloseChecklist)
    MaterialTheme(colorScheme = GolfColors, typography = GolfTypography) {
        Surface(modifier = Modifier.fillMaxSize(), color = GolfColors.background) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                val wide = maxWidth >= 620.dp
                val wideSettlement = maxWidth >= 760.dp
                val destination = state.destination
                val now by produceState(Instant.now(), state.screen, state.teeTime, destination?.receivedAt) {
                    value = Instant.now()
                    while (state.screen == GolfModeScreen.Home) {
                        delay(1_000)
                        value = Instant.now()
                    }
                }
                val viewportHeight = maxHeight
                val dashboard = state.screen == GolfModeScreen.Home && !state.checklistOpen &&
                    !briefingOpen && !settlementOpen && !teeTimeOpen
                val scrollState = key(state.screen, state.checklistOpen, briefingOpen, settlementOpen, teeTimeOpen) { rememberScrollState() }
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(if (wide) 24.dp else 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val detailBack: (() -> Unit)? = when {
                        settlementOpen -> { { settlementKey = null } }
                        briefingOpen -> { { briefingDestination = null } }
                        teeTimeOpen -> { { teeTimeOpen = false } }
                        state.checklistOpen -> onCloseChecklist
                        else -> null
                    }
                    val title = when {
                        settlementOpen -> "라운드 정산"
                        briefingOpen -> "오늘의 라운드 브리핑"
                        teeTimeOpen -> "스마트 티타임"
                        state.checklistOpen -> "라운드 준비물"
                        else -> "Golf Mate"
                    }
                    GolfHeader(title, if (dashboard) "골프 모드" else connectionStatus, detailBack)
                    if (dashboard && destination != null) {
                        RoundDashboard(
                            state, destination, now, wide,
                            (viewportHeight * 0.34f).coerceIn(100.dp, 340.dp),
                            tileHeight = if (wide) (viewportHeight * 0.31f).coerceAtLeast(172.dp) else 172.dp,
                            onTeeTime = { teeTimeOpen = true },
                            onChecklist = onOpenChecklist,
                            onBriefing = { briefingDestination = destination.key },
                            onSettlement = { settlementKey = destination.key }
                        )
                    } else if (teeTimeOpen && destination != null) {
                        TeeTimeHome(destination, state.teeTime, now, onRefresh) {
                            onSaveTeeTime(destination.key, it)
                        }
                    } else if (settlementOpen && destination != null) {
                        SettlementContent(destination, state.settlement, wideSettlement, (viewportHeight - 240.dp).coerceAtLeast(0.dp)) {
                            onSettlementChange(destination.key, it)
                        }
                    } else if (briefingOpen && destination != null) {
                        BriefingContent(destination.name, briefingText(destination, state.checklist, state.teeTime, now), speech)
                    } else if (state.checklistOpen) {
                        ChecklistContent(state.checklist, wide, viewportHeight, onToggleItem)
                    } else when (state.screen) {
                        GolfModeScreen.Waiting -> {
                            val artHeight = (viewportHeight * 0.29f).coerceIn(110.dp, 290.dp)
                            Column(
                                Modifier.fillMaxWidth().heightIn(min = (viewportHeight - artHeight - 142.dp).coerceAtLeast(0.dp)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
                            ) {
                                Text(destination?.name ?: "오늘도 라운드 가시나요?",
                                    style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                                Text(
                                    if (state.decision == GolfModeScreen.Waiting) "골프 모드 시작을 보류했어요."
                                    else "내비게이션에서 골프장을 목적지로 설정해 주세요.",
                                    color = GolfColors.onSurfaceVariant, textAlign = TextAlign.Center
                                )
                                Button(onClick = onRefresh, enabled = isConnected,
                                    modifier = Modifier.widthIn(min = 220.dp).heightIn(min = 80.dp)) {
                                    Text("목적지 조회", style = MaterialTheme.typography.titleLarge)
                                }
                            }
                            CourseArtwork(Modifier.fillMaxWidth().height(artHeight))
                        }
                        GolfModeScreen.Home -> Unit
                        GolfModeScreen.Offer -> if (destination != null) {
                            Column(
                                Modifier.fillMaxWidth().heightIn(min = (viewportHeight - 132.dp).coerceAtLeast(0.dp)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
                            ) {
                                Text("골프장 목적지가 감지되었어요", color = GolfColors.primary, textAlign = TextAlign.Center)
                                Text(destination.name, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    OfferMetric("남은 거리", distanceLabel(destination.remainingDistance), wide, Modifier.weight(1f))
                                    OfferMetric("예상 소요시간", timeLabel(destination.remainingTime), wide, Modifier.weight(1f))
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    FilledTonalButton(onClick = onDecline, modifier = Modifier.weight(1f).heightIn(min = 80.dp)) {
                                        Text("나중에", style = MaterialTheme.typography.titleMedium)
                                    }
                                    Button(onClick = onAccept, modifier = Modifier.weight(2f).heightIn(min = 80.dp)) {
                                        Text("골프 모드 시작", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferMetric(label: String, value: String, wide: Boolean, modifier: Modifier) {
    Surface(modifier, shape = GolfCardShape, color = GolfColors.surface) {
        Column(Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Text(value, style = if (wide) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ChecklistContent(
    checklist: PreparationChecklist,
    wide: Boolean,
    viewportHeight: androidx.compose.ui.unit.Dp,
    onToggle: (PreparationItem) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${checklist.count} / ${checklist.total}", style = MaterialTheme.typography.displaySmall, color = GolfColors.primary)
        Text(if (checklist.isComplete) "모든 준비물을 확인했어요!"
            else "준비물 ${checklist.total - checklist.count}개가 남았어요.", textAlign = TextAlign.Center)
        LinearProgressIndicator(progress = { checklist.progress }, modifier = Modifier.fillMaxWidth(0.8f).height(8.dp),
            color = GolfColors.primary, trackColor = GolfColors.secondaryContainer)
    }
    PreparationItem.entries.chunked(if (wide) 4 else 2).forEach { items ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { item ->
                val checked = item in checklist.checked
                Surface(
                    modifier = Modifier.weight(1f),
                    color = if (checked) GolfColors.primary else GolfColors.surface,
                    shape = GolfCardShape
                ) {
                    Column(
                        Modifier.fillMaxWidth().heightIn(min = if (wide) (viewportHeight * 0.24f).coerceIn(144.dp, 280.dp) else 144.dp)
                            .toggleable(checked, role = Role.Checkbox, onValueChange = { onToggle(item) }).padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                    ) {
                        Text(item.label, style = MaterialTheme.typography.titleLarge,
                            fontSize = if (wide) 36.sp else 28.sp,
                            lineHeight = if (wide) 44.sp else 36.sp, textAlign = TextAlign.Center,
                            color = if (checked) GolfColors.onPrimary else GolfColors.onSurface)
                        Checkbox(checked = checked, onCheckedChange = null,
                            modifier = Modifier.padding(8.dp).graphicsLayer {
                                scaleX = 1.7f
                                scaleY = 1.7f
                            }, colors = CheckboxDefaults.colors(
                            checkedColor = GolfColors.onPrimary, checkmarkColor = GolfColors.primary,
                            uncheckedColor = GolfColors.onSurfaceVariant))
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefingContent(destination: String, text: String, speech: BriefingSpeech) {
    val status = speech.status
    val playing = status == SpeechStatus.Playing
    val active = playing || status == SpeechStatus.Starting
    val stopping = status == SpeechStatus.Stopping
    var frame by remember { mutableStateOf(0) }
    LaunchedEffect(playing) {
        frame = 0
        while (playing) {
            delay(110)
            frame = (frame + 1) % 24
        }
    }
    GolfPanel {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(destination, style = MaterialTheme.typography.titleLarge)
            androidx.compose.foundation.Canvas(Modifier.fillMaxWidth(0.86f).height(126.dp)) {
                val heights = listOf(.14f, .24f, .43f, .32f, .62f, .77f, .48f, .93f, .60f, .76f, .45f, .58f, .30f, .20f, .12f)
                val step = size.width / heights.size
                heights.forEachIndexed { index, base ->
                    val pulse = if (playing) 0.72f + 0.28f *
                        kotlin.math.sin((frame * Math.PI / 12 + index * 0.55)).toFloat() else 0.72f
                    val half = size.height * base * pulse / 2
                    val x = step * (index + 0.5f)
                    drawLine(GolfColors.primary.copy(alpha = if (playing) 1f else 0.55f),
                        androidx.compose.ui.geometry.Offset(x, center.y - half),
                        androidx.compose.ui.geometry.Offset(x, center.y + half),
                        strokeWidth = (step * 0.44f).coerceAtMost(12.dp.toPx()),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
            }
            Text(status.label, color = GolfColors.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { speech.play(text) },
                    enabled = !active && !stopping && status != SpeechStatus.Loading && status != SpeechStatus.Unavailable,
                    modifier = Modifier.heightIn(min = 80.dp)) {
                    Text(if (status == SpeechStatus.Done || status == SpeechStatus.Stopped) "↻  다시 듣기" else "▶  재생",
                        style = MaterialTheme.typography.titleLarge)
                }
                OutlinedButton(onClick = speech::stop, enabled = active, modifier = Modifier.heightIn(min = 72.dp)) {
                    Text("정지")
                }
            }
        }
    }
    GolfPanel {
        Text("브리핑 내용", style = MaterialTheme.typography.titleLarge)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = GolfColors.secondary)
    }
}
