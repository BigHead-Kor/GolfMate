package com.selfbell.golfmate

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechStatusTest {
    @Test fun completionAfterStopAllowsReplay() {
        assertEquals(SpeechStatus.Stopped, SpeechStatus.Stopping.afterCallback(SpeechStatus.Done))
        assertEquals(SpeechStatus.Stopped, SpeechStatus.Stopped.afterCallback(SpeechStatus.Stopped))
    }

    @Test fun ignoresOldTerminalCallbacksWhileWaitingForStart() {
        assertEquals(SpeechStatus.Starting, SpeechStatus.Starting.afterCallback(SpeechStatus.Stopped))
        assertEquals(SpeechStatus.Starting, SpeechStatus.Starting.afterCallback(SpeechStatus.Done))
        assertEquals(SpeechStatus.Playing, SpeechStatus.Starting.afterCallback(SpeechStatus.Playing))
        assertEquals(SpeechStatus.Done, SpeechStatus.Playing.afterCallback(SpeechStatus.Done))
    }
}
