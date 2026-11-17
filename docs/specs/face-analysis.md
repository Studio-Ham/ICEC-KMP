# FaceAnalysis

> 상태: Draft
> SSOT: Git Repository
> 관련 요구사항: [Wiki FaceAnalysis · Editor UX](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/editor.md) · [Wiki PRD](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/prd.md) · Figma `[승인] 화면 3` (`316:2600`, `316:2750`, `316:2774`)
> 관련 결정: [UX-002](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-002), [UX-037~038](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-037), [UX-040](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-040), [ADR-0001](../adr/0001-face-analysis-route.md), [ADR-0003](../adr/0003-ios-mlkit-swiftpm-import.md)
> 최종 검토일: 2026-11-17

## 목적

Gallery에서 고른 사진의 얼굴을 기기 안에서 찾습니다. 결과에 따라 Editor 또는 복구 화면으로 이동합니다.

## 범위

구현은 세 단계로 나눕니다.

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
- 검출은 메인 스레드 밖에서 실행합니다.
- 시간 초과는 10초입니다(2026-11-17 사용자 결정). ③에서 구현합니다.

## 실패와 복구

| 상황 | 결과 |
| --- | --- |
| 얼굴 0개 | 빈 목록. ③에서 NoFaceDetected |
| 사진 삭제·읽기 실패·검출기 오류 | 예외. ③에서 AnalysisFailed |
| 10초 초과 | ③에서 AnalysisTimeout |

## 플랫폼 차이

| 항목 | Android | iOS |
| --- | --- | --- |
| 검출기 | ML Kit `face-detection` 16.1.7(모델 포함) | ML Kit 9.0.0, 커뮤니티 SwiftPM 패키지 9.0.3([ADR-0003](../adr/0003-ios-mlkit-swiftpm-import.md)) |
| 이미지 읽기 | API 28+: `ImageDecoder`가 회전을 반영해 축소. API 24~27: `BitmapFactory` `inSampleSize`, EXIF 회전 미반영 | `PHImageManager`(iCloud 다운로드 허용), `imageOrientation`을 ML Kit에 전달 |
| 모델 파일 | 라이브러리에 포함 | `GoogleMVFaceDetectorResources.bundle`을 앱 번들에 복사. 없으면 오류 없이 0개 |
| 사용 통계 | ML Kit이 Google 서버로 전송 | 같음 |

두 플랫폼의 ML Kit 모델 버전이 다릅니다. 작은 얼굴이 많은 사진에서는 검출 수가 다를 수 있습니다. 2026-11-17 같은 단체 사진에서 Android 6개, iOS 5개를 찾았습니다.

## Acceptance Criteria

### ① 얼굴 검출

- [x] Android·iOS에서 같은 테스트 사진(`platform/face-detection/fixtures/three-faces.jpg`, NASA 공개 사진)의 얼굴 3개를 찾는다
- [x] 얼굴이 없는 사진은 빈 목록을 반환한다
- [x] `IosFaceDetectorTest`가 iOS 시뮬레이터에서 실제 ML Kit으로 검출한다
- [x] 임시 FaceAnalysis 화면이 앱에서 검출 결과 수를 보여 준다(②에서 교체)

### ②·③

해당 단계의 Issue에서 이 문서를 갱신합니다.
