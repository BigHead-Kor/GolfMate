package com.selfbell.golfmate

internal enum class SpeechStatus(val label: String) {
    Loading("한국어 음성 확인 중"), Ready("재생 준비 완료"), Starting("재생 요청 중"),
    Playing("재생 중"), Stopping("재생 정지 중"), Done("재생 완료"), Stopped("재생 정지"),
    Error("재생할 수 없어요. 다시 시도해 주세요."),
    Unavailable("한국어 음성을 사용할 수 없어요. 아래 전문을 확인해 주세요.")
}

internal fun SpeechStatus.afterCallback(event: SpeechStatus): SpeechStatus = when (this) {
    SpeechStatus.Stopping -> when (event) {
        SpeechStatus.Done, SpeechStatus.Stopped -> SpeechStatus.Stopped
        SpeechStatus.Error -> SpeechStatus.Error
        else -> this
    }
    SpeechStatus.Starting -> when (event) {
        SpeechStatus.Playing, SpeechStatus.Error -> event
        else -> this
    }
    SpeechStatus.Playing -> when (event) {
        SpeechStatus.Done, SpeechStatus.Stopped, SpeechStatus.Error -> event
        else -> this
    }
    else -> this
}
