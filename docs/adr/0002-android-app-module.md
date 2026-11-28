# ADR-0002: Android 진입점을 androidApp 모듈로 분리

> 상태: Accepted
> 결정일: 2026-11-17
> 대체 대상: 없음

## Context

[모듈 경계](../architecture/module-boundaries.md)는 `composeApp`을 애플리케이션 진입점으로 정했습니다. 버전 정책은 도입 시점에 호환되는 최신 Stable 조합을 쓰도록 합니다.

2026-11-17 최신 Stable은 AGP 9.5.1, Kotlin 2.4.21, Compose Multiplatform 1.12.2입니다. AGP 9부터는 Kotlin Multiplatform 플러그인과 `com.android.application`을 같은 모듈에 적용할 수 없습니다([Kotlin AGP 9 이전 안내](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html)).

## Options

1. AGP 9.5.1을 쓰고 Android 진입점을 `androidApp` 모듈로 분리합니다. 공식 권장 구조이며 이후 다시 옮길 필요가 없습니다. Gradle 모듈이 하나 늘어납니다.
2. AGP 8.x 최신을 쓰고 `composeApp` 단일 모듈을 유지합니다. 문서 구조는 그대로지만 버전 정책에서 벗어나고, 결국 같은 이전 작업이 필요합니다.

## Decision

1번을 선택합니다.

- `composeApp`은 `com.android.kotlin.multiplatform.library`를 쓰는 KMP 모듈입니다. 공통 UI, Compose 리소스와 iOS 프레임워크(`ComposeApp`)를 소유합니다.
- `androidApp`은 `com.android.application` 모듈입니다. `MainActivity`와 Android 매니페스트만 소유하고 `composeApp`의 `App()`을 호출합니다.
- iOS 진입점은 `iosApp` Xcode 프로젝트입니다. `composeApp`의 프레임워크를 링크합니다.

## Consequences

- Android 전용 진입 설정(applicationId, 매니페스트, 런처 리소스)은 `androidApp`에 둡니다. 공통 back stack과 의존성 조립은 계속 `composeApp`이 맡습니다.
- Compose 1.12 의존성 때문에 compileSdk는 37 이상이어야 합니다. targetSdk는 36을 유지합니다.
- iOS 타깃은 `iosArm64`, `iosSimulatorArm64`만 둡니다. Intel Mac 시뮬레이터(`iosX64`)는 필요가 확인되면 추가합니다.

## Verification

- `./gradlew spotlessCheck assembleDebug test`
- `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
- `iosApp` 시뮬레이터 빌드(`xcodebuild ... ARCHS=arm64 CODE_SIGNING_ALLOWED=NO build`)
