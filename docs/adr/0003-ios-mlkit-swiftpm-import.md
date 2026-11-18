# ADR-0003: iOS ML Kit을 Kotlin SwiftPM import로 연결

> 상태: Accepted
> 결정일: 2026-11-17
> 대체 대상: 없음

## Context

[플랫폼 기능 경계](../architecture/platform-capabilities.md)는 iOS 얼굴 검출에 ML Kit iOS SDK를 쓰고 Kotlin/Native interop으로 연결하도록 정했습니다. 연결 방식은 정하지 않았습니다.

2026-11-17에 확인한 사실은 다음과 같습니다.

- Google은 ML Kit iOS를 CocoaPods로만 배포합니다(`GoogleMLKit/FaceDetection` 9.0.0, `MLKitFaceDetection` 8.0.0).
- 공식 pod는 `EXCLUDED_ARCHS[sdk=iphonesimulator*] = arm64`를 지정합니다. Apple Silicon 시뮬레이터용 바이너리가 없습니다.
- [ADR-0002](0002-android-app-module.md)에 따라 iOS 타깃은 `iosArm64`와 `iosSimulatorArm64`뿐입니다. 공식 pod를 쓰면 시뮬레이터 빌드와 iOS 테스트를 링크할 수 없습니다.
- Kotlin 2.4는 `swiftPMDependencies {}`로 Swift 패키지의 Objective-C API를 가져오는 SwiftPM import를 실험 기능으로 제공합니다. 공식 문서는 2.5.0-RC를 권장하지만 현재 버전 2.4.21에서도 동작합니다.
- 커뮤니티 패키지 [d-date/google-mlkit-swiftpm](https://github.com/d-date/google-mlkit-swiftpm) 9.0.3은 ML Kit 9.0.0 바이너리를 xcframework로 재패키징합니다. 실기기 바이너리에서 arm64 시뮬레이터 바이너리를 만들어 포함합니다.

## Options

1. **KMP CocoaPods 플러그인과 공식 pod:** Google 공식 배포본입니다. Apple Silicon 시뮬레이터에서는 ML Kit을 링크할 수 없습니다. 시뮬레이터에는 대체 구현이 필요하고 iOS 검출은 실기기에서만 확인합니다.
2. **SwiftPM import와 커뮤니티 패키지:** 시뮬레이터와 iOS 테스트에서 실제 검출을 확인합니다. CocoaPods가 필요 없습니다. 실험 기능과 비공식 배포본에 의존합니다.
3. **iosApp Swift 브리지:** Swift가 Kotlin 인터페이스를 구현합니다. cinterop이 필요 없습니다. 검출 코드가 `platform/face-detection` 밖에 놓이고 시뮬레이터 문제는 그대로입니다.
4. **iOS Apple Vision:** 외부 의존성이 없고 시뮬레이터에서 동작합니다. Architecture v1의 ML Kit 결정을 바꾸고 Android와 검출 결과가 달라집니다.

## Decision

2번을 선택합니다. 2026-11-17 사용자가 검증 결과를 보고 결정했습니다.

- `platform/face-detection`이 `swiftPMDependencies`로 `MLKitFaceDetection` 9.0.3을 선언합니다. `iosMain`이 ML Kit Objective-C API를 직접 호출합니다.
- iosApp은 Gradle이 생성한 `KotlinMultiplatformLinkedPackage`를 링크합니다. 생성 패키지, Xcode 프로젝트 변경과 `.swiftpm-locks`의 `Package.resolved`를 커밋합니다.
- ML Kit은 Objective-C category를 등록합니다. 정적 링크에서 category가 빠지지 않도록 iOS 테스트 바이너리와 iosApp에 `-ObjC` 링커 플래그를 둡니다.
- 얼굴 모델 `GoogleMVFaceDetectorResources.bundle`은 저장소에 넣지 않습니다. Gradle `faceModel` 작업이 패키지 릴리스에서 내려받고 SHA-256을 확인합니다. iOS 테스트 바이너리 옆과 iosApp 번들에 복사합니다.

## Consequences

- iOS 얼굴 검출을 Apple Silicon 시뮬레이터, iOS 테스트와 CI에서 확인합니다. CocoaPods는 설치하지 않습니다.
- SwiftPM import는 실험 기능입니다. Kotlin을 올릴 때 DSL과 생성물이 바뀔 수 있습니다. 버전 업그레이드 Issue에서 iOS 빌드와 `IosFaceDetectorTest`를 먼저 확인합니다.
- ML Kit 업데이트는 커뮤니티 패키지의 릴리스에 의존합니다. 패키지 버전과 모델 SHA-256을 함께 바꿉니다.
- 모델 번들이 없으면 ML Kit은 오류 없이 얼굴 0개를 반환합니다. `IosFaceDetectorTest`가 이 경우를 잡습니다.
- ML Kit은 사용 통계를 Google 서버(`play.googleapis.com`)로 보냅니다. 사진과 얼굴 데이터는 보내지 않습니다. 공식 pod와 Android ML Kit도 같은 동작을 합니다. 출시 전 개인정보 처리방침에서 다시 확인합니다.
- Android는 `INTERNET`, `ACCESS_NETWORK_STATE` 권한을 매니페스트에서 제거해 사용 통계를 보내지 않습니다(#36).
- **되돌릴 경로:** 커뮤니티 패키지나 SwiftPM import가 더 동작하지 않으면 1번(공식 pod)으로 바꿉니다. 이 경우 시뮬레이터용 대체 구현을 추가하고 이 ADR을 대체하는 새 ADR을 씁니다. Google이 ML Kit SPM 배포를 시작하면 공식 패키지로 바꿉니다.

## Verification

- `./gradlew :platform:face-detection:iosSimulatorArm64Test`: 테스트 사진(NASA 공개 사진, 얼굴 3개)에서 얼굴 3개를 찾습니다.
- iosApp 시뮬레이터 빌드(`xcodebuild ... ARCHS=arm64 CODE_SIGNING_ALLOWED=NO build`)가 성공하고 앱 번들에 모델 번들이 들어갑니다.
- 2026-11-17 iosApp 시뮬레이터 프로세스에서 같은 테스트 사진의 얼굴 3개를 찾았습니다.
