# 플랫폼 기능 경계

> 상태: Accepted
> SSOT: Git Repository
> 최종 검토일: 2026-11-28

## 얼굴 검출

- `commonMain`은 얼굴 검출 계약과 플랫폼 독립 결과 모델을 정의합니다.
- Android는 ML Kit Android SDK를 사용합니다.
- iOS는 ML Kit iOS SDK를 Kotlin SwiftPM import로 연결합니다([ADR-0003](../adr/0003-ios-mlkit-swiftpm-import.md)).
- 얼굴 검출 이후의 선택 상태, 편집 정책과 모자이크 대상 관리는 공통 로직이 담당합니다.
- 2026-11-17 Android·iOS 시뮬레이터에서 실제 얼굴 좌표 반환을 검증했습니다. 계약은 [FaceAnalysis Spec](../specs/face-analysis.md)에 있습니다.

## 이미지와 모자이크 렌더링

- `commonMain`은 선택된 얼굴과 제품에서 정한 고정 모자이크 강도를 적용하는 플랫폼 독립 정책을 담당합니다.
- 공통 요청 모델은 얼굴 영역, 선택된 얼굴 집합과 블록 크기처럼 렌더링에 필요한 최소 정보만 가집니다.
- 픽셀 처리는 공통 코드가 담당합니다. 미리보기와 저장은 같은 함수로 블록을 그립니다([ADR-0004](../adr/0004-common-mosaic-rendering.md)).
- 플랫폼 코드는 원본을 방향을 적용한 원본 해상도 이미지로 읽고, 결과를 JPEG로 저장합니다. 네이티브 이미지 타입은 외부에 노출하지 않습니다.
- Android API 24~27은 `BitmapFactory`가 EXIF 방향을 무시하므로 `ExifInterface` 방향 행렬을 원본 읽기와 얼굴 검출 디코딩에 함께 적용합니다([ADR-0005](../adr/0005-basic-photo-edit.md)).
- `thumbnail(id, sizePx)`는 Gallery 타일 전용입니다. 짧은 변이 대략 `sizePx` 이상이고, 타일이 정사각형으로 잘라 표시합니다. iOS는 로컬 데이터만 사용합니다. 그래서 Gallery 스크롤이 iCloud 다운로드를 시작하지 않습니다.
- `preview(id, maxSidePx)`는 사용자가 고른 사진 전체를 표시하는 화면(팔레트, 모자이크 Editor, Result)에서 사용합니다. 방향을 바로 세우고 원본 비율을 유지하며 긴 변은 `maxSidePx` 이하입니다. iOS는 iCloud 다운로드를 허용합니다.
- Android `preview()`는 API 28+에서 `ImageDecoder`로 방향을 적용하고 긴 변을 상한에 맞춰 정확히 줄입니다. API 24~27은 `BitmapFactory`로 크기를 먼저 읽고 긴 변이 상한 이하가 되는 2의 거듭제곱 `inSampleSize`로 디코딩한 뒤 같은 방향 행렬을 적용합니다. `loadThumbnail`은 요청 크기의 약 2배까지 반환할 수 있으므로 `preview()`에 쓰지 않습니다.
- Android `thumbnail()`은 API 29+에서 `loadThumbnail`을 씁니다. API 24~28은 같은 디코딩 함수로 짧은 변을 타일 크기 이상으로 두고 긴 변을 타일 크기의 4배 이하로 제한합니다. EXIF를 읽지 못하면 정상 디코딩한 사진을 회전 없이 사용합니다.
- iOS `preview()`는 `PHImageManager`의 `AspectFit`과 `ResizeModeExact`를 사용합니다. PhotoKit 결과 콜백이 여러 번 호출될 수 있으므로 저화질(`PHImageResultIsDegradedKey`) 결과는 건너뛰고 첫 최종 결과로 한 번만 재개합니다. 코루틴을 취소하면 요청도 취소합니다. 이미지 변환은 메인 스레드 밖에서 실행합니다.
- 기본 편집의 회전·반전·자르기도 공통 코드(`Canvas.orient`, `edited`)가 처리합니다. 미리보기와 저장이 같은 변환을 씁니다.
- 고해상도 이미지에서는 다운샘플링, 버퍼 재사용과 렌더링 범위 최소화를 검증합니다.

MVP는 사용자에게 강도 조절 기능을 제공하지 않습니다. 블록 크기는 얼굴 폭의 1/8입니다([UX-042](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-042)). 최종 저장 해상도는 [Wiki PRD](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/prd.md)의 원본 해상도 요구사항을 따릅니다.

```kotlin
interface PhotoLibrary {
    // ...
    suspend fun thumbnail(id: String, sizePx: Int): ImageBitmap?   // Gallery 타일
    suspend fun preview(id: String, maxSidePx: Int): ImageBitmap?  // 고른 사진 전체
    suspend fun original(id: String): ImageBitmap
    suspend fun save(image: ImageBitmap): String
}

internal fun ImageBitmap.withMosaic(faces: List<IntRect>): ImageBitmap
```

## 미디어 접근과 저장

- Gallery 접근, 권한 상태와 저장 API는 `platform/media`가 담당합니다.
- feature는 플랫폼 사진 식별자와 공통 모델만 사용합니다.
- 원본 사진을 변경하지 않고 플랫폼 사진 보관함에 새 결과를 저장합니다.
- 권한, 제한 접근, 원본 로드 실패와 저장 실패의 UX는 Wiki UX 결정을 따르고 구체적인 오류 매핑은 관련 Spec에서 확정합니다.

## 성능 계약

- 얼굴 검출, 모자이크 렌더링, 저장 시간과 peak memory를 측정할 수 있는 지점을 둡니다.
- Android와 iOS에서 같은 입력과 모자이크 조건을 사용해 결과와 성능을 비교할 수 있어야 합니다.
- PRD의 얼굴 검출 3초 이내와 저장 1초 이내 기준은 대표 기기와 승인된 이미지 세트에서 반복 측정해 검증합니다.
