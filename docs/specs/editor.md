# Editor

> 상태: Accepted
> SSOT: Git Repository
> 관련 요구사항: [Wiki FaceAnalysis · Editor UX](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/editor.md) · [Wiki PRD](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/prd.md) · Figma `[승인] 화면 4` (`316:3606`, `316:4197`, `316:4294`, `335:2`, Snackbar `316:4050`·`316:3747`·`316:3897`)
> 관련 결정: [UX-039](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-039), [UX-041](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-041), [UX-014](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-014), [UX-016](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-016), [UX-042](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-042), [UX-045](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-045), [UX-046](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-046), [ADR-0001](../adr/0001-face-analysis-route.md), [ADR-0004](../adr/0004-common-mosaic-rendering.md)
> 최종 검토일: 2026-11-21

## 목적

사용자가 검출된 얼굴 중 가릴 얼굴을 고르고, 저장 전에 모자이크 결과를 바로 확인하게 합니다.

## 범위

구현은 세 단계로 나눕니다.

| 단계 | Issue | 내용 |
| --- | --- | --- |
| ① | #24 | 얼굴 선택, 모자이크 미리보기, 얼굴 목록, 전체 선택/해제, 저장 버튼 상태, 이탈 확인 |
| ② | #25 | Pinch Zoom, Pan (Result 포함) |
| ③ | #26 | 원본 해상도 모자이크 저장, 저장 중 Dialog, 저장 실패, Result 이동 |

## 계약

### 진입과 상태

- `feature/editor`가 `EditorViewModel`과 `EditorScreen`을 제공합니다. FaceAnalysis와 같은 모듈입니다([ADR-0001](../adr/0001-face-analysis-route.md)).
- `Route.Editor(photoId, faces)`로 진입합니다. FaceAnalysis를 대체하므로 Editor를 나가면 Gallery로 돌아갑니다.
- 화면 이미지는 `PhotoLibrary.thumbnail(photoId, 2048)`입니다. 저장 이미지는 ③에서 원본 해상도로 따로 렌더링합니다.
- 사진 상자는 이미지 비율에 맞추고 12dp 둥근 모서리로 자릅니다. 1dp 테두리를 두르고 바깥 여백 배경은 채우지 않습니다.
- 얼굴은 왼쪽부터 정렬합니다. 사진 위 가이드와 하단 목록이 같은 순서를 씁니다.
- 모든 얼굴은 미선택으로 시작합니다(`Detected ≠ Selected`).
- `EditorState`는 얼굴 목록, 선택한 얼굴 집합과 이탈 확인 Dialog 표시 여부를 가집니다. `canSave`는 선택이 1개 이상일 때 참입니다(UX-039).

### 선택과 미리보기

- 사진 위 얼굴 가이드나 하단 목록 항목을 누르면 선택이 바뀝니다. 가이드가 겹치면 더 작은 얼굴을 고릅니다.
- 미선택 얼굴은 파란 1dp 가이드, 선택한 얼굴은 라임 2dp 가이드, 체크 표시와 모자이크 미리보기를 보여 줍니다.
- 모자이크 블록 한 칸은 얼굴 폭의 1/8입니다(UX-042). 세로 칸 수는 얼굴 높이에 맞춥니다. 미리보기와 저장은 같은 규칙을 씁니다.
- 선택이 0개이면 `전체 선택`, 1개 이상이면 `전체 해제` 텍스트 버튼을 보여 줍니다(UX-046). 버튼은 `IcecColors.NeonLime` 색상과 `48.dp` 최소 터치 영역을 사용합니다. 일부만 선택해도 `전체 해제`입니다.
- 하단 목록은 각 얼굴을 원본 사진에서 정사각형으로 잘라 보여 줍니다. 얼굴 크기의 1.4배를 잘라 여백을 둡니다(UX-041).
- 상단 카운터는 검출 수와 선택 수를 보여 줍니다.

### Pinch Zoom과 Pan

- 사진은 기본 배율(화면에 맞춘 크기)의 최대 4배까지 확대합니다.
- 기본 배율에서는 한 손가락 드래그를 무시하고 탭만 얼굴 선택에 씁니다. 확대 상태에서는 짧은 탭이 선택, 드래그가 이동입니다.
- 사진, 얼굴 가이드와 모자이크 미리보기는 같은 변환으로 그립니다. 가이드 두께와 체크 표시는 화면 기준 크기를 유지합니다.
- 확대한 사진이 화면보다 큰 방향은 사진 밖 빈 영역이 보이지 않게 이동을 제한합니다. 작은 방향은 가운데에 둡니다.
- 확대는 손가락 사이 지점을 기준으로 합니다.
- 변환 규칙은 `core/designsystem`의 `ZoomState`에 있고 Result도 같은 규칙을 씁니다. 상태는 저장 가능한 상태로 두어 Result에서 돌아와도 Editor 배율과 위치가 유지됩니다.

### 뒤로가기

- 상단 뒤로가기와 시스템 뒤로가기는 선택 여부와 관계없이 이탈 확인 Dialog를 엽니다(Figma `316:4294`).
- `나가기`는 `IcecColors.AlertRedStrong`(#F87171) 색상을 사용하며, Editor를 닫고 Gallery의 이전 위치로 돌아갑니다. `계속 편집`은 Dialog를 닫고 선택을 유지합니다.

### 저장

- 저장 버튼은 원본 해상도 이미지를 읽고, 선택한 얼굴에 미리보기와 같은 모자이크를 그린 뒤 새 사진으로 저장합니다([ADR-0004](../adr/0004-common-mosaic-rendering.md)). 원본은 바꾸지 않습니다.
- 저장 파일은 메타데이터 없는 JPEG입니다. 방향은 픽셀에 적용되어 있습니다. Android는 `Pictures/ICEC`에 저장합니다.
- 저장 중에는 편집 화면을 흐리게 덮고 보조문 없이 로딩 표시와 제목만 보여 줍니다(Figma `335:2`, UX-045). 얼굴 선택, 저장 재실행과 뒤로가기를 모두 막습니다(UX-014).
- 저장에 성공하면 `Route.Result(savedPhotoId)`를 Editor 위에 추가합니다. Result에서 뒤로 가면 같은 편집 세션으로 돌아옵니다. Result 화면은 [Result Spec](result.md)을 따릅니다.
- iOS는 저장하는 동안 Background 실행 시간을 요청합니다. Android는 프로세스가 살아 있는 동안 ViewModel에서 계속 저장합니다.

## 실패와 복구

| 상황 | 결과 |
| --- | --- |
| 화면 이미지를 읽을 수 없음 | 사진 영역을 비워 둡니다. 얼굴 목록과 선택은 그대로 동작합니다 |
| 저장 공간 부족 | Snackbar `저장 공간이 부족하여 저장할 수 없습니다`(`316:4050`) |
| 저장 권한 없음 | Snackbar `사진 저장 권한 허용이 필요합니다`와 `권한 설정`(`316:3747`). 설정 화면을 엽니다 |
| 원본 읽기·렌더링·인코딩·그 밖의 저장 실패 | Snackbar `이미지 처리에 실패했습니다.`와 `다시 시도`(`316:3897`) |

저장에 실패하면 저장 중 화면을 닫고 선택을 유지합니다(UX-016). Snackbar는 4초 뒤 사라집니다.

## 플랫폼 차이

화면과 모자이크 렌더링은 공통 코드로 구현합니다. 시스템 뒤로가기는 Navigation 3에 포함된 `navigationevent-compose`의 `NavigationBackHandler`로 받습니다.

| 항목 | Android | iOS |
| --- | --- | --- |
| 원본 읽기 | API 28+: `ImageDecoder`(방향 적용). API 24~27: `BitmapFactory`, EXIF 회전 미반영 | `PHImageManagerMaximumSize`로 읽고 방향을 적용해 다시 그림 |
| 저장 | `MediaStore` 새 항목, API 29+는 `IS_PENDING` 사용 | `PHAssetCreationRequest` |
| 저장 권한 | API 29+: 필요 없음. API 28 이하: `WRITE_EXTERNAL_STORAGE`를 사진 권한과 함께 요청 | 사진 보관함 `readWrite` 권한 |
| 오류 판별 | `SecurityException` → 권한, `ENOSPC` → 공간 | `PHPhotosError` 코드 |
| 저장 중 블러 | Android 12+ | 적용 |

## Acceptance Criteria

### ① 선택과 미리보기

- [x] 모든 얼굴은 미선택으로 시작하고, 사진 위 가이드와 목록 항목을 눌러 선택/해제한다
- [x] 선택한 얼굴에 모자이크 미리보기가 바로 보인다
- [x] 선택 0개이면 `전체 선택`과 비활성 저장, 1개 이상이면 `전체 해제`와 활성 저장
- [x] 사진 상자가 이미지 비율에 맞고 12dp 둥근 모서리로 보인다
- [x] 뒤로가기는 이탈 확인 Dialog를 열고, `나가기`는 Gallery, `계속 편집`은 선택을 유지한다
- [x] `EditorTest`: 선택·전체 선택/해제·저장 상태, 이탈 확인, 모자이크 칸 수, 좌표 변환, 얼굴 자르기

### ③ 저장

- [x] 저장한 이미지가 원본과 같은 해상도이고 선택한 얼굴만 모자이크된다. 원본은 바뀌지 않는다
- [x] 저장 중에는 입력이 막히고, 실패하면 선택이 유지되며 Snackbar가 원인별로 보인다
- [x] 저장에 성공하면 Result로 이동하고, 뒤로 가면 같은 편집 세션으로 돌아온다
- [x] `EditorSaveTest`(iOS): 모자이크 블록, 원본 해상도 저장, 오류 매핑과 재시도

### ② Pinch Zoom·Pan

- [x] 최대 4배 확대, 기본 배율에서는 이동 없음
- [x] 확대·이동 후에도 얼굴 탭 선택이 같은 변환을 따른다
- [x] 사진 밖 빈 영역이 보이지 않는다
- [x] `ZoomTest`: 기본 배율 이동 무시, 최대 배율, 이동 제한, 손가락 기준 확대
