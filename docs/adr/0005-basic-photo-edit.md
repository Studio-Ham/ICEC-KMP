# ADR-0005: 도구 팔레트와 기본 편집을 feature/editor에 추가

> 상태: Accepted
> 결정일: 2026-11-27
> 대체 대상: 없음 (ADR-0001 유지, ADR-0004 확장)
> 기준 원본: [Wiki 기본 사진 편집 기획](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/planning/basic-photo-edit/plan.md)과 [D1–D6 결정 결과](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/planning/basic-photo-edit/plan.md#decided) · Figma `[탐색]` `623:13`

## Context

다음 업데이트는 `Gallery` 뒤에 사진 미리보기·도구 팔레트를 둡니다. 팔레트에서 `기본 편집`(자르기·90도 회전·좌우 반전) 또는 기존 모자이크 흐름을 고릅니다. 두 편집은 각각 원본에서 시작하고 각각 새 사진을 저장합니다.

이 변경은 Architecture v1의 다음 기준을 바꿉니다.

- [애플리케이션 아키텍처](../architecture/application-architecture.md)의 정상 흐름과 back stack 조립
- [모듈 경계](../architecture/module-boundaries.md)의 `feature/editor` 책임
- [플랫폼 기능 경계](../architecture/platform-capabilities.md)의 원본 방향 처리(Android API 24~27은 EXIF 회전을 적용하지 않았습니다)
- 힌지 감지를 위한 `androidx.window` 직접 사용

## Options

1. **새 `feature/photo-edit` 모듈**: 기하 편집의 변경 이유를 분리합니다. 저장 중 Dialog·Snackbar·이탈 Dialog·CTA와 저장 오류 문자열을 복제해야 합니다.
2. **`feature/editor` 확장**: 같은 편집 기능 모듈에 Route 2개를 더합니다. 위 UI와 문자열, `SaveError` 매핑을 그대로 재사용합니다.
3. **미리보기용 `PhotoLibrary.preview()` 신규 API**: 플랫폼마다 축소 디코딩을 구현합니다. 미리보기와 저장의 방향·비율 일치를 플랫폼마다 따로 검증해야 합니다.
4. **`original()`을 공통 코드에서 축소**: 미리보기와 저장이 같은 원본에서 나오므로 방향·비율이 구조적으로 같습니다. 화면을 열 때 원본을 한 번 더 디코딩합니다.

## Decision

2번과 4번을 선택합니다.

- `feature/editor`가 `PhotoPreview`(팔레트)와 `BasicEdit` Route를 추가로 소유합니다. Route별 상태와 ViewModel은 분리합니다.
- 팔레트의 `모자이크`는 back stack의 `PhotoPreview`를 `FaceAnalysis`로 교체합니다. 그래서 분석 이탈·예외·Editor `나가기`는 기존 `pop`으로 Gallery 이전 위치에 돌아갑니다. `기본 편집`은 팔레트 위에 쌓습니다(D1).
- 기본 편집 상태는 비파괴 모델 `BasicEdit(turns, flipped, crop, ratio)`입니다. 적용 순서는 반시계 90도 회전 → 좌우 반전 → 자르기로 고정합니다(D3, D6).
- 미리보기와 저장은 같은 `Canvas.orient()`로 그립니다. 저장은 `original()`에 `edited()`를 적용하고 `save()`로 새 JPEG를 만듭니다(ADR-0004 방식의 확장).
- Android API 24~27의 `original()`과 얼굴 검출 디코딩에 `android.media.ExifInterface` 방향 행렬을 함께 적용합니다. 플랫폼 모듈끼리 의존하지 않으므로 같은 행렬 함수를 두 모듈에 둡니다.
- `core/designsystem`이 `separatingHinges()`(Android `WindowInfoTracker`, iOS 빈 목록)와 순수 함수 `hingeSplit()`을 제공합니다. `androidx.window:window:1.5.0`은 Compose UI를 통해 이미 앱에 들어 있으므로 새 산출물이 늘지 않습니다.

## Consequences

- Gradle 모듈 수는 그대로입니다. `feature/editor`는 Route 4개를 소유합니다.
- Android API 24~27의 모자이크 저장 결과가 원본 방향으로 바뀝니다. 이전에는 EXIF 회전이 있는 사진이 누운 채 저장되었습니다.
- 기본 편집 화면은 열 때 원본을 디코딩합니다. 저장 시 원본을 다시 디코딩하므로 원본 해상도 이미지가 편집 중 메모리에 남지 않습니다.
- 팔레트 미리보기와 모자이크 Editor는 `thumbnail(photoId, 2048)`을 함께 씁니다. 그래서 방향은 플랫폼 `thumbnail()`에서 맞춥니다(2026-11-28 PR #64 리뷰). Android API 24~28은 `MINI_KIND` 대신 축소 디코딩과 같은 방향 행렬을 쓰고, iOS는 iCloud 다운로드 허용·방향 정규화·백그라운드 변환을 적용합니다. `PhotoLibrary` 인터페이스는 바뀌지 않습니다.
- 48MP급·파노라마 사진의 메모리와 저장 시간은 실기기에서 측정해야 합니다([기술 백로그](../development/technical-backlog.md)).
- 정책 반영은 1.0.0 출시 후 Wiki 정책 변경 제안 승인 절차를 따릅니다.

## Verification

- `BasicEditTest`(공통): 회전·반전·비율·자르기 드래그·픽셀 반올림·이탈 규칙
- `BasicEditSaveTest`(iOS 시뮬레이터): 회전 4종 × 반전 2종의 픽셀 이동, 자르기, 저장 실패 시 편집 유지
- `BackStackTest`(Android host·iOS 시뮬레이터): 모자이크 교체 후 Gallery 복귀, 기본 편집 쌓기와 `다시 편집하기`
- `ZoomTest`: 회전·창 축소 뒤 확대 이동량 재제한
- `AdaptiveTest`: 세로·가로 힌지 분할
- `IcecFlowUITests`: 팔레트를 거친 모자이크 흐름과 `testBasicEditFlow`
