# 도구 팔레트와 기본 편집

> 상태: Accepted
> SSOT: Git Repository
> 관련 요구사항: [Wiki 기본 사진 편집 기획](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/planning/basic-photo-edit/plan.md) · [정책 변경 제안](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/proposals/2026-11-27-basic-photo-edit-policy.md) · Figma `[탐색]` `623:13` (`623:87`, `623:123`, `623:172`)
> 관련 결정: [D1–D6 결정 결과](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/planning/basic-photo-edit/plan.md#decided), [UX-014](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-014), [UX-016](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-016), [UX-043](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-043), [ADR-0005](../adr/0005-basic-photo-edit.md)
> 최종 검토일: 2026-11-28

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

- 미리보기 이미지는 `PhotoLibrary.preview(photoId, 2048)`입니다. 모자이크 Editor·Result와 같은 경로입니다. 플랫폼이 방향을 바로 세우고 원본 비율과 긴 변 2048px 이하를 보장합니다([플랫폼 기능 경계](../architecture/platform-capabilities.md#이미지와-모자이크-렌더링)). 사진 상자는 이미지 비율, 12dp 둥근 모서리와 1dp 테두리를 사용합니다.
- 버튼의 원과 이름은 하나의 이름 있는 버튼입니다. 이름을 눌러도 동작합니다.
- 팔레트 외형은 `palette_background.xml`(Figma `623:101`, 358×222, `evenOdd` path) 한 장입니다. 두 구멍은 확장 슬롯입니다.
- `편집`·`모자이크` 원형 버튼은 Compose로 그립니다. 위치는 358×222 기준 앵커 `(84, 93)`·`(168, 92)`에 팔레트 배율을 곱합니다.
- 팔레트 배율은 0.75~1.25입니다. 0.75 이상에서 버튼 지름은 48dp 이상이고 두 버튼의 터치 영역은 겹치지 않습니다.

### 적응형 배치

| 조건 | 배치 |
| --- | --- |
| 분리형 세로 힌지가 본문을 가름 | 힌지 앞쪽에 사진, 뒤쪽에 팔레트 |
| 분리형 가로 힌지가 본문을 가름 | 위에 사진, 아래에 팔레트 |
| `isSideBySide()` (`isTwoPane()`이고 창 폭 580dp 이상) | 왼쪽에 사진, 오른쪽 열(390dp와 본문 폭의 절반 중 작은 값)에 팔레트 |
| 그 밖(휴대폰 세로, 좁은 분할 창) | 위에 사진, 아래에 팔레트(본문 높이의 45%, 198.5~254dp, 본문 높이의 60% 이하) |

- 힌지는 `core/designsystem`의 `separatingHinges()`와 `hingeSplit()`으로 판정합니다. iOS에는 힌지가 없습니다.
- 팔레트 벡터와 버튼은 한 Composable이므로 힌지가 둘 사이를 지나가지 않습니다.
- 기본 편집 화면도 `isSideBySide()`로 1단과 2단(오른쪽 조절 열, 390dp와 창 폭의 절반 중 작은 값)을 바꿉니다. 높이 480dp 미만의 좁은 분할 창은 1단입니다.
- 조절부(비율·도구·`편집 완료`)는 공간이 부족하면 세로로 스크롤합니다. 1단에서 조절부는 본문 높이의 60% 이하이므로 사진은 40% 이상을 유지합니다.
- 팔레트가 배율 0.75로도 들어가지 않는 작은 영역(높이 약 330dp 미만의 짧은 창)은 48dp 원형 버튼 두 개만 표시합니다. 큰 글꼴로 버튼이 영역보다 높으면 세로로 스크롤합니다.
- 힌지는 자체 축(높이가 폭 이상이면 세로)으로 판정합니다. 힌지가 본문을 끝에서 끝까지 가로지르고 양쪽에 각각 160dp 이상을 남길 때만 분할합니다. 본문 가장자리나 모서리에만 걸친 힌지는 무시하고 일반 배치를 사용합니다. 그래서 사진이나 버튼이 0dp가 되지 않습니다. 160dp는 버튼 두 개 배치(배율 0.75에서 2 × 63dp + 16dp)와 16dp 여백의 사진이 들어가는 크기입니다.
- 비율 칩(최대 76dp)과 도구 버튼(최대 104dp)은 좁은 창에서 줄의 폭을 나눠 모두 표시합니다.

### 기본 편집 상태

- `BasicEdit(turns, flipped, crop, ratio)`는 비파괴 편집입니다. 적용 순서는 반시계 90도 회전 → 좌우 반전 → 자르기입니다(D3, D6).
- `crop`은 회전·반전한 사진 기준 0~1 비율입니다. 화면 크기와 미리보기 해상도에 관계없이 같은 값을 씁니다.
- 비율은 `자유`, `1:1`, `4:3`, `16:9`입니다(D5). 비율을 고르면 사진 안 최대 크기의 가운데 영역이 됩니다. `자유`는 현재 영역을 유지합니다.
- `90도 회전`은 화면의 사진을 반시계로 90도 돌리고 자르기 영역을 현재 비율의 최대 사각형으로 초기화합니다(D6). 반전 상태에서는 반전 뒤의 화면 기준으로 반시계이므로 `turns`가 1 줄어듭니다.
- `좌우 반전`은 자르기 영역을 좌우 대칭으로 옮깁니다(D6).
- 자르기 상자는 모서리 드래그로 크기를 바꾸고 안쪽 드래그로 옮깁니다. 모서리 터치 범위는 ±24dp이고 상자의 최소 크기는 64dp입니다. 사진 가장자리의 모서리는 사진 안쪽 절반만 잡힙니다.
- 자르기 값은 항상 0~1 안으로 맞춥니다. Float 반올림이 경계를 넘어도 이동 범위가 비지 않습니다.
- 사진 좌우 여백은 시스템 제스처 영역과 16dp 중 큰 값입니다. Android 제스처 내비게이션의 가장자리 뒤로가기가 모서리 드래그를 가로채지 않습니다(2026-11-27 Android 에뮬레이터에서 확인).
- 사진은 모자이크 Editor와 같은 `ZoomState` 규칙으로 최대 4배까지 확대·이동합니다. 두 손가락은 확대·이동입니다.
- 회전과 창 크기 변경 뒤에도 그리기와 제스처는 현재 사진 크기와 화면 크기로 이동량을 다시 제한합니다(`ZoomState.transform`, `zoomable(key)`).
- 1배에서는 한 손가락 드래그가 자르기 상자를 움직입니다. 사진은 이동하지 않습니다.
- 확대 상태에서는 한 손가락 드래그가 사진을 이동합니다. 자르기 모서리에서 시작한 드래그만 자르기 크기를 바꿉니다. 사진 밖 빈 영역은 보이지 않습니다.
- 자르기 값은 확대 배율과 관계없이 사진 기준 비율입니다. 드래그 이동량은 현재 화면의 사진 크기(`ZoomState.photoRect`)로 나눕니다.
- 화면 이미지는 `original()`을 긴 변 2048px로 줄인 것입니다. 그래서 미리보기와 저장의 방향·비율이 같습니다.

### 이탈과 저장

- 변경이 없으면 뒤로가기는 확인 없이 팔레트로 돌아갑니다. 변경이 있으면 이탈 확인 Dialog를 엽니다(D1).
- `편집 완료`는 항상 활성입니다. 사진을 읽은 뒤에는 변경이 없어도 새 사진으로 저장합니다(D2, 2026-11-27 사용자 변경). 읽는 중이거나 읽지 못한 상태의 입력은 무시합니다.
- 저장은 `original()`에 `edited()`를 적용하고 `save()`로 새 JPEG를 만듭니다. 원본은 바꾸지 않습니다.
- 저장 중에는 Editor와 같은 로딩 Dialog로 입력과 뒤로가기를 막습니다(D4, UX-014). 한 번에 하나의 저장만 실행합니다.
- 이탈 Dialog와 저장 Dialog가 열려 있으면 뒤의 화면은 스크린 리더에 노출하지 않습니다.
- 이탈 Dialog가 열려 있으면 저장 오류 Snackbar를 숨깁니다. 그래서 터치와 스크린 리더 모두 Dialog 뒤의 `다시 시도`·`권한 설정`을 실행할 수 없습니다. Snackbar의 4초 타이머는 계속 진행합니다. ViewModel도 이탈 Dialog가 열려 있으면 저장과 Snackbar 동작을 무시하므로 저장 중과 이탈 Dialog가 함께 열리지 않습니다(모자이크 Editor와 같음).
- 실패할 때마다 `saveFailures`가 늘어납니다. `다시 시도`가 바로 다시 실패해도 새 Snackbar는 4초를 따로 표시합니다(모자이크 Editor와 같음).

## 실패와 복구

| 상황 | 결과 |
| --- | --- |
| 팔레트 미리보기를 읽을 수 없음 | 사진 영역을 비웁니다. 버튼은 그대로 동작합니다 |
| 기본 편집 화면 이미지를 읽을 수 없음 | 사진 영역을 비웁니다. 편집 입력은 무시합니다 |
| 저장 공간 부족·권한 없음·그 밖의 실패 | Editor와 같은 원인별 Snackbar. 편집 상태를 유지합니다(D4, UX-016) |

## 플랫폼 차이

| 항목 | Android | iOS |
| --- | --- | --- |
| 원본 방향 | API 28+: `ImageDecoder`. API 24~27: `BitmapFactory` + `ExifInterface` 8방향 행렬(EXIF를 읽지 못하면 회전 없음) | `PHImageManagerMaximumSize` + `upright()` |
| 팔레트 미리보기(`preview`) | API 28+: `ImageDecoder` 목표 크기(긴 변 2048px). API 24~27: 2의 거듭제곱 `inSampleSize`(긴 변 2048px 이하) + 같은 8방향 행렬 | `AspectFit` + `ResizeModeExact`(긴 변 2048px), iCloud 다운로드 허용, 저화질 결과 건너뜀, 방향이 있으면 `upright()`, 백그라운드 변환 |
| 힌지 | `WindowInfoTracker`의 분리형 `FoldingFeature` | 없음 |
| 저장 | `MediaStore`(`Pictures/ICEC`) | `PHAssetCreationRequest` |

## Acceptance Criteria

- [x] Gallery 사진 → 팔레트 → `편집` 또는 `모자이크`로 갈라진다
- [x] 모자이크 경로의 모든 이탈은 팔레트를 거치지 않고 Gallery로 돌아간다(`BackStackTest`)
- [x] 회전 4종 × 반전 2종의 픽셀 이동이 정확하고 자르기는 원본 해상도로 저장된다(`BasicEditSaveTest`)
- [x] 회전은 반전 뒤에도 화면 기준 반시계이고 자르기를 비율 최대 사각형으로 초기화한다. 반전은 자르기를 대칭으로 옮긴다(`BasicEditTest`)
- [x] 고정 비율 상자를 경계까지 늘린 뒤 옮겨도 예외가 없다(`BasicEditTest`)
- [x] 회전·창 축소 뒤 확대 이동량이 다시 제한된다(`ZoomTest`)
- [x] 읽기 전 `편집 완료`는 무시하고, 읽은 뒤 무변경 저장과 즉시 재실패를 구분한다(`BasicEditTest`, `BasicEditSaveTest`)
- [x] 미리보기와 원본 해상도의 자르기 경계 차이는 원본 기준 2px 이하다(`BasicEditTest`)
- [x] `편집 완료`는 항상 활성이다. 변경이 없으면 뒤로가기는 바로 나간다(`BasicEditTest`, `testBasicEditFlow`)
- [x] 두 손가락 핀치로 확대하고, 확대 상태의 한 손가락 드래그는 사진을 이동한다(`testBasicEditFlow`)
- [x] 저장 실패 시 편집 상태를 유지한다(`BasicEditSaveTest`)
- [x] 세로·가로 힌지 분할. 가장자리·모서리에만 걸친 힌지와 160dp 미만 영역은 분할하지 않는다(`AdaptiveTest`)
- [x] 이탈 Dialog가 열려 있으면 저장과 Snackbar 재시도를 무시한다(`EditorTest`, `BasicEditSaveTest`)
- [x] Android 미리보기 긴 변은 2048px 이하다. 12MP·48MP·파노라마의 샘플 크기를 계산으로 검증한다(`DecodeSizeTest`)
- [ ] Android API 24·27·28·34 에뮬레이터에서 EXIF 방향 1~8 사진의 미리보기·저장·얼굴 박스 방향 확인
- [ ] 폴더블 에뮬레이터(탁상·책 자세)와 iPad Split View에서 배치와 상태 유지 확인
- [ ] 48MP급·파노라마 사진의 저장 메모리와 시간 측정

## 알려진 제한

다음 항목은 별도 정책 결정이나 큰 설계 변경이 필요하므로 PR #64에서 고치지 않습니다(2026-11-28 재검증).

- 사진 가장자리의 자르기 모서리는 확대 중에도 사진 안쪽 절반만 잡힙니다. 사진 밖 터치를 받으려면 자르기 좌표계를 다시 설계해야 합니다.
- 이탈·저장 Dialog는 화면 위 오버레이입니다. 스크린 리더와 터치는 막지만 키보드·D-pad 포커스는 뒤의 화면으로 이동할 수 있습니다. 막으려면 모달 구조 전체를 바꿔야 합니다.
- 이탈 Dialog가 열린 동안 시스템 뒤로가기는 Dialog를 닫지 않습니다. 모자이크 Editor와 같은 기존 동작이며 Wiki 정책을 바꾸지 않습니다.
- 문서 머리의 Wiki 기획·정책 제안 링크는 Wiki 원격 저장소에 게시되기 전까지 열리지 않습니다.
