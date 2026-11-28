# ADR-0004: 모자이크 픽셀 처리를 공통 코드로 구현

> 상태: Accepted
> 결정일: 2026-11-18
> 대체 대상: [플랫폼 기능 경계](../architecture/platform-capabilities.md)의 플랫폼별 `MosaicRenderer` 계약

## Context

[플랫폼 기능 경계](../architecture/platform-capabilities.md)는 Android와 iOS가 같은 `MosaicRenderer` 계약을 각각 구현하도록 정했습니다. 픽셀 처리와 이미지 인코딩을 플랫폼 렌더러가 맡는 구조입니다.

Wiki 정책은 Editor 미리보기와 저장 결과가 같은 모자이크를 보여 주도록 요구합니다([UX-042](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-042)). Editor ①(#24)은 미리보기를 Compose `ImageBitmap`과 `Canvas`로 공통 구현했습니다. Compose `Canvas`는 Android(Android Bitmap)와 iOS(Skia)에서 모두 화면 밖 이미지에 그릴 수 있습니다.

## Options

1. **플랫폼별 `MosaicRenderer`:** 원래 계획입니다. 플랫폼 이미지 API를 직접 써서 메모리를 세밀하게 다룰 수 있습니다. 미리보기와 같은 규칙을 두 번 더 구현하고, 결과 일치를 따로 검증해야 합니다.
2. **공통 픽셀 처리, 플랫폼은 읽기와 저장만:** 미리보기와 저장이 같은 함수(`mosaicBlocks`)를 씁니다. 플랫폼 코드는 원본을 똑바로 세운 `ImageBitmap`으로 읽고, 결과를 JPEG로 저장합니다. 전체 해상도 이미지를 메모리에 두 장 올립니다.

## Decision

2번을 선택합니다.

- `feature/editor`의 `withMosaic`이 원본 해상도 `ImageBitmap`에 미리보기와 같은 `mosaicBlocks`를 그립니다.
- `platform/media`의 `PhotoLibrary`는 `original(id)`와 `save(image)`를 제공합니다. `original`은 방향을 적용한 원본 해상도 이미지를 돌려줍니다. `save`는 메타데이터 없는 JPEG(품질 95)를 새 사진으로 저장하고 원본은 바꾸지 않습니다.
- 저장 실패는 `SaveException.NoSpace`, `SaveException.NoPermission`과 그 밖의 예외로 나눕니다.

## Consequences

- 미리보기와 저장 결과가 같은 코드로 만들어집니다. `EditorSaveTest`가 iOS 시뮬레이터에서 블록 색 일치와 얼굴 밖 픽셀 보존을 확인합니다.
- 전체 해상도 ARGB 이미지 두 장(원본과 결과)을 메모리에 올립니다. 12MP 사진에서 약 96MB입니다. 대용량 사진에서 메모리가 부족하면 얼굴 영역만 처리하는 방식으로 바꿉니다.
- 저장 파일은 원본 형식과 관계없이 JPEG입니다. GPS를 포함한 메타데이터는 모두 빠집니다. 방향은 픽셀에 적용되어 있습니다.
- iOS 원본 읽기는 `UIImage`를 똑바로 다시 그린 뒤 최고 품질 JPEG로 Skia에 넘깁니다. 저장 결과는 한 번 더 JPEG로 인코딩합니다.
- Android 9 이하는 저장에 `WRITE_EXTERNAL_STORAGE`가 필요하므로 사진 권한 요청에 함께 넣습니다.

## Verification

- `./gradlew allTests`: `EditorSaveTest`(모자이크 블록, 원본 해상도 저장, 오류 매핑과 재시도)
- 2026-11-18 Android 에뮬레이터에서 NASA 테스트 사진(640×502)을 저장했습니다. 결과는 640×502이고 선택한 얼굴만 모자이크되었으며 원본 파일은 그대로였습니다.
