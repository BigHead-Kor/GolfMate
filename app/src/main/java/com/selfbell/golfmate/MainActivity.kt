package com.selfbell.golfmate

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.constants.NaviErrorCode

class MainActivity : ComponentActivity() {

    private val speech by lazy { BriefingSpeech(applicationContext) }
    private lateinit var naviHelper: NaviHelper

    private var isConnected by mutableStateOf(false)
    private var connectionStatus by mutableStateOf("내비게이션 연결 중")
    private var golfMode by mutableStateOf(GolfModeState())

    private var routeCandidate: RouteCandidate? = null
    private var routeEndedAt: Long? = null

    private fun trace(message: String) {
        Log.d(
            "GolfMate",
            "t=${SystemClock.elapsedRealtime()}ms activity=${System.identityHashCode(this)} " +
                "thread=${Thread.currentThread().name} $message"
        )
    }

    private var destroyed = false

    private fun whileActive(action: () -> Unit) {
        runOnUiThread {
            if (!destroyed) action()
        }
    }

    private fun requestDestination(source: String) = whileActive {
        trace("request getDestinationInfo source=$source")
        naviHelper.getDestinationInfo()
    }

    // 내비게이션 이벤트 리스너
    private val naviEventListener = object : NaviHelperEventListener {

        override fun onDestinationChanged() {
            trace("event onDestinationChanged")
            requestDestination("onDestinationChanged")
        }

        override fun onRoutingCompleted() {
            trace("event onRoutingCompleted")
            requestDestination("onRoutingCompleted")
        }

        override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
            trace("event onRouteStarted")
            whileActive {
                routeEndedAt = null
                routeCandidate = RouteCandidate(
                    routeStartInfo.name.orEmpty().trim(),
                    RouteEstimate(routeStartInfo.distance.toLong(), routeStartInfo.tollFare.toLong())
                )
                val destination = golfMode.destination
                if (destination != null) golfMode = golfMode.update(destination.copy(routeEstimate = null))
            }
        }

        override fun onRouteEnded() {
            trace("event onRouteEnded")
            whileActive {
                routeEndedAt = SystemClock.elapsedRealtime()
                requestDestination("onRouteEnded")
            }
        }

        override fun onRouteCancelled() {
            trace("event onRouteCancelled")
            whileActive {
                routeCandidate = null
                routeEndedAt = null
                golfMode = golfMode.update(null)
            }
        }

        override fun onReRouteEnded() {
            trace("event onReRouteEnded")
        }

        override fun onNavigationStatus(navigationStatus: NavigationStatus) {
            trace("event onNavigationStatus status=$navigationStatus")
        }

        override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
            trace("event onRouteStateInfo")
        }

        override fun onError(errorCode: NaviErrorCode) {
            trace("event onError code=$errorCode")
        }

        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            trace("response onDestinationInfo")

            whileActive {
                val name = destinationInfo.name.orEmpty().trim()
                val destination = if (name.isBlank()) {
                    null
                } else {
                    val poiId = destinationInfo.poiId.orEmpty()
                    val key = if (poiId.isNotBlank()) {
                        "poi:${destinationInfo.poiProvider}:$poiId:${destinationInfo.poiSubId.orEmpty()}"
                    } else {
                        "location:$name:${destinationInfo.latitude}:${destinationInfo.longitude}"
                    }
                    GolfDestination(
                        key,
                        name,
                        destinationInfo.remainDistance.toLong(),
                        destinationInfo.remainTime.toLong(),
                        java.time.Instant.now()
                    )
                }
                trace("apply destination present=${destination != null}")
                val previous = golfMode.destination
                val endedAt = routeEndedAt
                val candidate = routeCandidate
                val matched = destination != null && endedAt != null && candidate != null &&
                    SystemClock.elapsedRealtime() - endedAt <= 10_000 && candidate.matches(destination)
                if (matched || endedAt != null && SystemClock.elapsedRealtime() - endedAt > 10_000) {
                    routeCandidate = null
                    routeEndedAt = null
                }
                val estimate = if (matched) candidate?.estimate
                    else previous?.routeEstimate?.takeIf { previous.key == destination?.key }
                trace("settlement route matched=$matched")
                golfMode = golfMode.update(destination?.copy(routeEstimate = estimate))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        trace("lifecycle onCreate")
        naviHelper = NaviHelper(applicationContext)
        trace("initialize NaviHelper")

        naviHelper.initialize(
            onConnected = {
                whileActive {
                    trace("service onConnected")
                    naviHelper.addListener(naviEventListener)
                    isConnected = true
                    connectionStatus = "내비게이션 연결됨"
                    requestDestination("onConnected")
                }
            },
            onFailed = {
                whileActive {
                    Log.e("GolfMate", "NaviHelper connection failed")
                    isConnected = false
                    connectionStatus = "내비게이션 연결 실패"
                }
            }
        )

        setContent {
            GolfModeContent(
                state = golfMode,
                connectionStatus = connectionStatus,
                isConnected = isConnected,
                onRefresh = { requestDestination("manual button") },
                onAccept = { golfMode = golfMode.accept() },
                onDecline = { golfMode = golfMode.decline() },
                onOpenChecklist = { golfMode = golfMode.openChecklist() },
                onCloseChecklist = { golfMode = golfMode.closeChecklist() },
                onToggleItem = { golfMode = golfMode.toggleItem(it) },
                speech = speech,
                onSettlementChange = { destinationKey, settings ->
                    if (golfMode.screen == GolfModeScreen.Home && golfMode.destination?.key == destinationKey) {
                        golfMode = golfMode.copy(settlement = settings)
                    }
                },
                onSaveTeeTime = { destinationKey, teeTime ->
                    if (golfMode.screen == GolfModeScreen.Home && golfMode.destination?.key == destinationKey) {
                        golfMode = golfMode.copy(teeTime = teeTime)
                    }
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        trace("lifecycle onStart")
    }

    override fun onResume() {
        super.onResume()
        trace("lifecycle onResume")
    }

    override fun onPause() {
        speech.stop()
        super.onPause()
        trace("lifecycle onPause")
    }

    override fun onStop() {
        super.onStop()
        trace("lifecycle onStop")
    }

    override fun onDestroy() {
        destroyed = true
        trace("lifecycle onDestroy")
        speech.close()
        if (::naviHelper.isInitialized) {
            trace("removeListener listener=${System.identityHashCode(naviEventListener)}")
            naviHelper.removeListener(naviEventListener)
            naviHelper.release()
        }
        super.onDestroy()
    }
}
