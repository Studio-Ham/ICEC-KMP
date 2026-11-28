# FaceAnalysis

> 상태: Accepted
> SSOT: Git Repository
> 관련 요구사항: [Wiki FaceAnalysis · Editor UX](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/editor.md) · [Wiki PRD](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/prd.md) · Figma `[승인] 화면 3` (`316:2600`, `316:2750`, `316:2774`, `340:397`)
> 관련 결정: [UX-002](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-002), [UX-037~038](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-037), [UX-040](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-040), [UX-045](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-045), [ADR-0001](../adr/0001-face-analysis-route.md), [ADR-0003](../adr/0003-ios-mlkit-swiftpm-import.md)
> 최종 검토일: 2026-11-21

## 목적

Gallery에서 고른 사진의 얼굴을 기기 안에서 찾습니다. 결과에 따라 Editor 또는 복구 화면으로 이동합니다.

## 범위

구현은 세 단계로 나뉩니다.

| 단계 | Issue | 내용 |
| --- | --- | --- |
| ① | #18 | `platform/face-detection` 모듈, Android·iOS ML Kit 연동 |
| ② | #19 | `feature/editor` 모듈, Analyzing 화면, Editor 이동 |
| ③ | #20 | NoFaceDetected·AnalysisFailed·AnalysisTimeout, Background 유지 |

## 계약

### 얼굴 검출

- `platform/face-detection`은 `FaceDetector`와 `FaceBounds`를 제공합니다. Android·iOS 구현은 이 모듈 안에 둡니다.
- `FaceDetector.detect(photoId)`는 `Photo.id`(MediaStore id, PHAsset `localIdentifier`)를 받습니다.
- 빈 목록은 분석에 성공했고 얼굴이 없다는 뜻입니다. 사진을 읽을 수 없거나 검출기가 실패하면 예외를 던집니다.
- `FaceBounds`는 똑바로 세운 사진 기준의 비율(0~1)입니다. 화면이나 저장 해상도에 상관없이 같은 값을 씁니다.
- 분석용 이미지는 긴 변 2048px 이하로 줄입니다. PRD가 검출 단계의 다운샘플링을 허용합니다.
- `AndroidFaceDetector`와 `IosFaceDetector`는 ML Kit `performanceMode`를 `ACCURATE`로 설정합니다(이전 기본값 `FAST`).
- 두 검출기는 `minFaceSize`를 `commonMain` `FaceDetector.kt`의 공통 상수 `MIN_FACE_SIZE`(`0.03f`)로 설정합니다(이전 기본값 `0.1`).
- 검출기는 기본값 `0.1`일 때 얼굴 폭이 이미지 폭의 10% 이상인 얼굴만 찾습니다. `0.03`은 얼굴 폭이 3% 이상인 얼굴까지 찾습니다.
- TestFlight에서 검출기가 단체 사진의 작은 얼굴과 옆얼굴을 찾지 못했습니다(#46). 이 문제를 해결하려고 `ACCURATE` 모드와 `MIN_FACE_SIZE`를 적용했습니다.
- 감도 조정 후에도 입력 이미지 상한(긴 변 2048px)과 분석 제한 시간(10초)은 그대로 유지합니다.
- 2026-11-19 Argon 환경의 iOS 시뮬레이터에서 `three-faces.jpg`를 검증했습니다. 검출기는 얼굴 3개를 찾았습니다. 1장 검출 시간은 147ms입니다.
- `IosFaceDetector.detect(image)`는 ML Kit에 넘기기 전에 `UIImage.rgbx()`로 8bit sRGB RGBX, `bytesPerRow = width × 4` 비트맵을 만듭니다.
- 실기기 ML Kit은 행 패딩이 있는 이미지를 바르게 읽지 못합니다. 폭이 4의 배수가 아닌 작은 사진에서 생깁니다.
- 단위 테스트 `convertsBgrxToRgbxAtTheSamePixelSize`는 13×8 BGRX 입력에서 출력 `bytesPerRow`가 52인지 확인합니다.
- 실기기(iPhone 15 Pro Max, 2026-11-20)에서 증명사진(JPEG 425×567)을 진단했습니다. 시뮬레이터에서는 재현되지 않았습니다.

| 입력 조건 | `bytesPerRow` | 픽셀 형식 / `bitmapInfo` | 검출 수 |
| --- | --- | --- | --- |
| `PHImageManager` 원본 CGImage | 1728 (행 패딩 28바이트) | BGRX (`kCGImageAlphaNoneSkipFirst \| kCGBitmapByteOrder32Little`, 8198) | 0개 (ACCURATE·FAST 모두) |
| 1차 수정 (RGBX, `bytesPerRow` 자동) | 패딩 남음 | RGBX | 0개 |
| 2차 수정 (RGBX, `bytesPerRow = width × 4`) | 1700 | RGBX (`kCGImageAlphaNoneSkipLast`, 5) | 1개 |
- 검출은 메인 스레드 밖에서 실행합니다.
- 시간 초과는 10초입니다(2026-11-17 사용자 결정). `FaceAnalysisViewModel`이 검출 호출에 적용합니다.

### FaceAnalysis 화면

- `feature/editor`가 `FaceAnalysisViewModel`과 `FaceAnalysisScreen`을 제공합니다([ADR-0001](../adr/0001-face-analysis-route.md)).
- ViewModel은 Navigation 3 entry마다 하나입니다. 생성될 때 한 번만 분석하고, entry가 back stack에서 빠지면 분석을 취소합니다.
- 상태는 `Analyzing`, `Detected(faces)`, `NoFace`, `Failed`, `Timeout`입니다.
- Analyzing은 선택 사진의 썸네일(512px)을 흑백으로 바꾸고 블러 처리한 뒤 검정 60%로 덮습니다. 그 위에 회전하는 로딩 표시와 `얼굴을 찾는 중...` 안내를 둡니다(UX-002, Figma `316:2600`). 편집 입력은 없습니다.
- `Detected`이면 `composeApp`이 back stack의 FaceAnalysis를 `Route.Editor(photoId, faces)`로 바꿉니다. Editor에서 뒤로 가면 Gallery로 돌아갑니다. Editor 구현 전까지는 임시 화면을 보여 줍니다.
- 분석 중 시스템 뒤로가기는 확인 없이 Gallery의 이전 위치로 돌아갑니다. Figma 디자인에는 상단 뒤로가기가 없습니다.
- Background에서 분석이 끝나면 결과를 상태로 보관합니다. 화면 이동은 화면이 다시 보일 때 실행합니다. 분석을 다시 실행하지 않습니다(UX-040).
- NoFaceDetected(Figma `316:2750`)는 상단 뒤로가기, 미검출 아이콘, 안내와 `다른 사진 선택` 버튼을 보여 줍니다.
- AnalysisFailed(`340:397`)와 AnalysisTimeout(`316:2774`)은 아이콘과 안내만 다릅니다. 두 화면은 한 줄 본문과 `다시 분석하기`, `다른 사진 선택` 버튼을 보여 줍니다(UX-038, UX-045).
- `다시 분석하기`는 같은 화면에서 같은 사진을 다시 분석합니다. `다른 사진 선택`과 뒤로가기는 Gallery의 이전 위치로 돌아갑니다.

## 실패와 복구

| 상황 | 결과 |
| --- | --- |
| 얼굴 0개 | NoFaceDetected |
| 사진 삭제·읽기 실패·검출기 오류 | AnalysisFailed |
| 10초 초과 | AnalysisTimeout. 검출 호출을 취소합니다 |

## 플랫폼 차이

| 항목 | Android | iOS |
| --- | --- | --- |
| 검출기 | ML Kit `face-detection` 16.1.7(모델 포함) | ML Kit 9.0.0, 커뮤니티 SwiftPM 패키지 9.0.3([ADR-0003](../adr/0003-ios-mlkit-swiftpm-import.md)) |
| 이미지 읽기 | API 28+: `ImageDecoder`가 회전을 반영해 축소. API 24~27: `BitmapFactory` `inSampleSize`, EXIF 회전 미반영 | `PHImageManager`(iCloud 다운로드 허용), `imageOrientation`을 ML Kit에 전달 |
| 배경 블러 | Android 12+에서만 블러 적용. 이전 버전은 어둡게만 처리 | 블러 적용 |
| 모델 파일 | 라이브러리에 포함 | `GoogleMVFaceDetectorResources.bundle`을 앱 번들에 복사. 없으면 오류 없이 0개 |
| Background 실행 | 프로세스가 살아 있으면 ViewModel 코루틴이 계속 실행 | 분석 동안 `beginBackgroundTask`로 실행 시간을 요청. OS가 허용한 시간이 끝나면 일시 정지 |
| 사용 통계 | 인터넷 권한을 제거해 전송하지 않음 (#36) | ML Kit이 Google 서버로 전송할 수 있음 |

두 플랫폼의 ML Kit 모델 버전이 다릅니다. 작은 얼굴이 많은 사진에서는 검출 수가 다를 수 있습니다. 2026-11-17 같은 단체 사진에서 Android 6개, iOS 5개를 찾았습니다.

## Acceptance Criteria

### ① 얼굴 검출

- [x] Android·iOS에서 같은 테스트 사진(`platform/face-detection/fixtures/three-faces.jpg`, NASA 공개 사진)의 얼굴 3개를 찾는다
- [x] 얼굴이 없는 사진은 빈 목록을 반환한다
- [x] `IosFaceDetectorTest`가 iOS 시뮬레이터에서 실제 ML Kit으로 검출한다
- [x] 임시 FaceAnalysis 화면이 앱에서 검출 결과 수를 보여 준다(②에서 교체)

### ② Analyzing과 Editor 이동

- [x] 사진 선택 → Analyzing(블러 배경, 로딩 표시, 안내) → 얼굴이 있으면 Editor 임시 화면
- [x] Editor 임시 화면에서 뒤로 가면 Gallery로 돌아간다
- [x] 분석 중 뒤로가기는 Gallery의 이전 위치로 돌아간다
- [x] `FaceAnalysisViewModelTest`: 검출 결과별 상태(`Detected`, `NoFace`, `Failed`)

### ③ 복구 화면과 Background 유지

- [x] 얼굴 0개는 NoFaceDetected, 처리 오류는 AnalysisFailed, 10초 초과는 AnalysisTimeout
- [x] `다시 분석하기`는 같은 사진을 다시 분석하고, `다른 사진 선택`·뒤로가기는 Gallery로 돌아간다
- [x] Background 중 끝난 결과는 보관했다가 화면이 다시 보일 때 이동한다
- [x] `FaceAnalysisViewModelTest`: 시간 초과 후 재분석
