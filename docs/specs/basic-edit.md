# 도구 팔레트와 기본 편집

> 상태: Accepted
> SSOT: Git Repository
> 관련 요구사항: [Wiki 기본 사진 편집 기획](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/planning/basic-photo-edit/plan.md) · [정책 변경 제안](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/proposals/2026-11-27-basic-photo-edit-policy.md) · Figma `[탐색]` `623:13` (`623:87`, `623:123`, `623:172`)
> 관련 결정: [D1–D6 결정 결과](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/planning/basic-photo-edit/plan.md#decided), [UX-014](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-014), [UX-016](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-016), [UX-043](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-043), [ADR-0005](../adr/0005-basic-photo-edit.md)
> 최종 검토일: 2026-11-27

## 목적

사용자가 고른 사진을 미리 보고 기본 편집 또는 모자이크를 고르게 합니다. 기본 편집은 원본에 자르기·90도 회전·좌우 반전을 적용해 새 사진으로 저장합니다.

## 범위

- 다음 업데이트 기능입니다. Figma 시안은 `[탐색]` 상태입니다.
- 정책 문서 반영은 1.0.0 출시 후 Wiki 제안 승인 절차를 따릅니다.
- 얼굴 분석, 모자이크 Editor와 Result 동작은 각 Spec을 따릅니다.

## 계약

### 화면과 back stack

- `feature/editor`가 `PhotoPreviewScreen`, `BasicEditScreen`, `BasicEditViewModel`을 제공합니다([ADR-0005](../adr/0005-basic-photo-edit.md)).
- Gallery에서 읽을 수 있는 사진을 누르면 `composeApp`이 `Route.PhotoPreview(photoId)`를 추가합니다.
- 팔레트 상단 뒤로가기와 시스템 뒤로가기는 Gallery의 이전 위치로 돌아갑니다(D1).
- `편집`은 팔레트 위에 `Route.BasicEdit(photoId)`를 추가합니다.
- `모자이크`는 팔레트를 `Route.FaceAnalysis(photoId)`로 교체합니다. 분석 이탈, 미검출·실패·시간 초과 화면의 복귀와 Editor `나가기`는 Gallery의 이전 위치로 돌아갑니다.
- 기본 편집 저장에 성공하면 `Route.Result(savedPhotoId)`를 기본 편집 위에 추가합니다. `다시 편집하기`는 Result를 빼고 직전 기본 편집 상태를 보여 줍니다.
- 두 편집은 모두 원본 `photoId`에서 시작합니다. 저장 결과를 다른 편집의 입력으로 쓰지 않습니다.

### 도구 팔레트

- 미리보기 이미지는 `PhotoLibrary.thumbnail(photoId, 2048)`입니다. 사진 상자는 이미지 비율, 12dp 둥근 모서리와 1dp 테두리를 사용합니다.
- 팔레트 외형은 `palette_background.xml`(Figma `623:101`, 358×222, `evenOdd` path) 한 장입니다. 두 구멍은 확장 슬롯입니다.
- `편집`·`모자이크` 원형 버튼은 Compose로 그립니다. 위치는 358×222 기준 앵커 `(84, 93)`·`(168, 92)`에 팔레트 배율을 곱합니다.
- 팔레트 배율은 0.75~1.25입니다. 0.75 이상에서 버튼 지름은 48dp 이상이고 두 버튼의 터치 영역은 겹치지 않습니다.

### 적응형 배치

| 조건 | 배치 |
| --- | --- |
| 분리형 세로 힌지가 본문을 가름 | 힌지 앞쪽에 사진, 뒤쪽에 팔레트 |
| 분리형 가로 힌지가 본문을 가름 | 위에 사진, 아래에 팔레트 |
| `isTwoPane()`이고 본문 폭 580dp 이상 | 왼쪽에 사진, 오른쪽 390dp 열에 팔레트 |
| 그 밖(휴대폰 세로, 좁은 분할 창) | 위에 사진, 아래에 팔레트(본문 높이의 45%, 198.5~254dp) |

- 힌지는 `core/designsystem`의 `separatingHinges()`와 `hingeSplit()`으로 판정합니다. iOS에는 힌지가 없습니다.
- 팔레트 벡터와 버튼은 한 Composable이므로 힌지가 둘 사이를 지나가지 않습니다.
- 기본 편집 화면은 모자이크 Editor처럼 `isTwoPane()`으로 1단과 2단(오른쪽 390dp 조절 열)을 바꿉니다.

### 기본 편집 상태

- `BasicEdit(turns, flipped, crop, ratio)`는 비파괴 편집입니다. 적용 순서는 반시계 90도 회전 → 좌우 반전 → 자르기입니다(D3, D6).
- `crop`은 회전·반전한 사진 기준 0~1 비율입니다. 화면 크기와 미리보기 해상도에 관계없이 같은 값을 씁니다.
- 비율은 `자유`, `1:1`, `4:3`, `16:9`입니다(D5). 비율을 고르면 사진 안 최대 크기의 가운데 영역이 됩니다. `자유`는 현재 영역을 유지합니다.
- `90도 회전`은 반시계로 90도 돌리고 자르기 영역을 현재 비율의 최대 사각형으로 초기화합니다(D6).
- `좌우 반전`은 자르기 영역을 좌우 대칭으로 옮깁니다(D6).
- 자르기 상자는 모서리 드래그로 크기를 바꾸고 안쪽 드래그로 옮깁니다. 모서리 터치 범위는 ±24dp이고 상자의 최소 크기는 64dp입니다.
- 사진 좌우 여백은 시스템 제스처 영역과 16dp 중 큰 값입니다. Android 제스처 내비게이션의 가장자리 뒤로가기가 모서리 드래그를 가로채지 않습니다(2026-11-27 Android 에뮬레이터에서 확인).
- 화면 이미지는 `original()`을 긴 변 2048px로 줄인 것입니다. 그래서 미리보기와 저장의 방향·비율이 같습니다.

### 이탈과 저장

- 변경이 없으면 뒤로가기는 확인 없이 팔레트로 돌아갑니다. 변경이 있으면 이탈 확인 Dialog를 엽니다(D1).
- 변경이 없으면 `편집 완료`는 비활성입니다(D2).
- 저장은 `original()`에 `edited()`를 적용하고 `save()`로 새 JPEG를 만듭니다. 원본은 바꾸지 않습니다.
- 저장 중에는 Editor와 같은 로딩 Dialog로 입력과 뒤로가기를 막습니다(D4, UX-014). 한 번에 하나의 저장만 실행합니다.

## 실패와 복구

| 상황 | 결과 |
| --- | --- |
| 팔레트 미리보기를 읽을 수 없음 | 사진 영역을 비웁니다. 버튼은 그대로 동작합니다 |
| 기본 편집 화면 이미지를 읽을 수 없음 | 사진 영역을 비웁니다. 편집 입력은 무시합니다 |
| 저장 공간 부족·권한 없음·그 밖의 실패 | Editor와 같은 원인별 Snackbar. 편집 상태를 유지합니다(D4, UX-016) |

## 플랫폼 차이

| 항목 | Android | iOS |
| --- | --- | --- |
| 원본 방향 | API 28+: `ImageDecoder`. API 24~27: `BitmapFactory` + `ExifInterface` 8방향 행렬 | `PHImageManagerMaximumSize` + `upright()` |
| 힌지 | `WindowInfoTracker`의 분리형 `FoldingFeature` | 없음 |
| 저장 | `MediaStore`(`Pictures/ICEC`) | `PHAssetCreationRequest` |

## Acceptance Criteria

- [x] Gallery 사진 → 팔레트 → `편집` 또는 `모자이크`로 갈라진다
- [x] 모자이크 경로의 모든 이탈은 팔레트를 거치지 않고 Gallery로 돌아간다(`BackStackTest`)
- [x] 회전 4종 × 반전 2종의 픽셀 이동이 정확하고 자르기는 원본 해상도로 저장된다(`BasicEditSaveTest`)
- [x] 회전은 반시계이고 자르기를 비율 최대 사각형으로 초기화한다. 반전은 자르기를 대칭으로 옮긴다(`BasicEditTest`)
- [x] 미리보기와 원본 해상도의 자르기 경계 차이는 원본 기준 2px 이하다(`BasicEditTest`)
- [x] 변경이 없으면 `편집 완료`가 비활성이고 뒤로가기는 바로 나간다(`BasicEditTest`)
- [x] 저장 실패 시 편집 상태를 유지한다(`BasicEditSaveTest`)
- [x] 세로·가로 힌지 분할(`AdaptiveTest`)
- [ ] Android API 24·27·28·34 에뮬레이터에서 EXIF 방향 1~8 사진의 미리보기·저장·얼굴 박스 방향 확인
- [ ] 폴더블 에뮬레이터(탁상·책 자세)와 iPad Split View에서 배치와 상태 유지 확인
- [ ] 48MP급·파노라마 사진의 저장 메모리와 시간 측정
