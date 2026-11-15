# Golf Mate MVP v1.0

Golf Mate는 Pleos Connect 차량용 화면에서 골프장으로 이동하는 동안 라운드 준비, 도착 예정 시간, 음성 브리핑, 예상 교통비를 한곳에서 확인하는 Android 앱입니다.

## 주요 기능

- **골프 모드** — 내비 목적지 변경을 감지하고 이름 기반으로 골프장을 판별합니다. 감지하면 골프 모드를 제안하고 사용자가 시작 또는 보류를 선택합니다.
- **준비물** — 기본 준비물 8개의 체크 상태와 진행률을 표시합니다.
- **스마트 티타임** — 라운드 날짜, 티타임, 준비 시간을 설정하고 내비 응답 기준 예상 도착과 목표 도착을 비교합니다.
- **음성 브리핑** — 목적지, 이동 정보, 티타임, 준비물에 맞춰 구성한 한국어 문장을 화면에 표시하고 재생·정지·재재생합니다.
- **라운드 정산** — 경로 시작 거리와 예상 통행료, 입력한 연비와 유류 단가를 사용해 편도·왕복 및 1~4인 예상 교통비를 계산합니다. 거리와 통행료는 수동 보정할 수 있습니다.

## 기술 스택

| 구성 | 버전 및 용도 |
| --- | --- |
| Android | Kotlin 2.0.21, Jetpack Compose(Material 3, BOM 2024.09.00) |
| 빌드 | Gradle Wrapper 8.11.1, Android Gradle Plugin 8.10.0, Android SDK 36으로 컴파일, target 35, min 28 |
| Pleos NaviHelper | `ai.pleos.playground:NaviHelper:3.0.5` · 목적지, 경로 이벤트, 거리 및 예상 통행료 |
| Pleos TextToSpeech | `ai.pleos.playground:TextToSpeech:3.3.1` · `ON_DEVICE`, 한국어 `ko-KR` |

앱 ID는 `com.selfbell.golfmate`입니다. Manifest에 내비 경로·경로 검색 및 Pleos TTS 서비스 권한을 선언합니다.

## 빌드 및 실행

1. Android Studio에서 프로젝트 루트를 엽니다. Android SDK Platform 36을 설치하고 Gradle JDK는 17 이상을 사용합니다.
2. Gradle Sync를 실행합니다. 최초 의존성 다운로드에는 Pleos Maven 저장소와 Android/Gradle 저장소 접속이 필요합니다. Pleos 저장소는 `settings.gradle.kts`에 설정돼 있습니다.
3. Pleos Connect Emulator 2.1.0 AVD를 부팅하고 Android Studio에서 `app` 실행 대상으로 선택합니다.
4. **Run**을 눌러 앱을 설치·실행합니다. Pleos 기본 내비에서 골프장을 목적지로 설정한 뒤 Golf Mate로 돌아와 골프 모드를 시작합니다.

터미널에서 디버그 APK 빌드 및 JVM 테스트를 실행할 수도 있습니다.

```sh
sh gradlew :app:assembleDebug :app:testDebugUnitTest
```

빌드가 성공하면 APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다. 내비 및 한국어 음성 경로를 검증하려면 해당 Pleos 서비스가 동작하는 환경이 필요합니다.

## 알려진 제한사항

- 골프장 자동 판별은 목적지 이름 키워드 기준입니다. 약칭이나 예외 명칭에서 오탐·누락이 가능합니다.
- 도착 시간은 마지막 내비 응답을 기준으로 계산한 추정치입니다. 응답이 5분을 넘겼거나 유효한 잔여 시간이 없으면 예측을 제공하지 않습니다.
- 정산 자동값은 새 경로 이벤트와 목적지 응답이 매칭될 때 제공됩니다. 내비 통행료 0은 미확인으로 처리합니다. 왕복은 동일 경로를 가정하며 금액은 실제 결제와 다를 수 있습니다.
- 준비물, 티타임, 정산 입력은 현재 골프 모드 세션에서 관리합니다. 목적지 변경, 경로 취소 또는 앱 재시작 시 초기화됩니다.
- Pleos TTS 콜백에는 재생 요청 ID가 없어 새 재생 시작 이후 도착한 이전 콜백은 식별할 수 없습니다. 내비 안내와 동시에 발화할 때의 오디오 우선순위는 별도 검증이 필요합니다.
- **향후 개발** — Gleo AI 연동. v1.0의 음성 브리핑은 앱 데이터로 구성한 정형 문장과 Pleos TTS를 사용합니다.

## 제출 소스

프로젝트 루트의 Gradle 설정과 Wrapper, `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/`, README 및 PROGRESS를 포함합니다. `local.properties`, IDE 개인 설정·캐시, 빌드 출력과 서명 키는 소스 제출에서 제외합니다. APK 제출이 필요하면 최종 빌드 산출물을 별도 첨부합니다.

테스트 및 승인 이력은 [PROGRESS.md](PROGRESS.md)를 참고하세요.
