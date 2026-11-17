# Editor

> 상태: Draft
> SSOT: Git Repository
> 관련 요구사항: [Wiki FaceAnalysis · Editor UX](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/editor.md) · [Wiki PRD](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/prd.md) · Figma `[승인] 화면 4` (`316:3606`, `316:4197`, `316:4294`, `335:2`)
> 관련 결정: [UX-039](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-039), [UX-041](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-041), [ADR-0001](../adr/0001-face-analysis-route.md)
> 최종 검토일: 2026-11-18

## 목적

사용자가 검출된 얼굴 중 가릴 얼굴을 고르고, 저장 전에 모자이크 결과를 바로 확인하게 합니다.

## 범위

구현은 세 단계로 나눕니다.

| 단계 | Issue | 내용 |
| --- | --- | --- |
| ① | #24 | 얼굴 선택, 모자이크 미리보기, 얼굴 목록, 전체 선택/해제, 저장 버튼 상태, 이탈 확인 |
| ② | #25 | Pinch Zoom, Pan |
| ③ | #26 | 원본 해상도 모자이크 저장, 저장 중 Dialog, 저장 실패, Result 이동 |

## 계약

### 진입과 상태

- `feature/editor`가 `EditorViewModel`과 `EditorScreen`을 제공합니다. FaceAnalysis와 같은 모듈입니다([ADR-0001](../adr/0001-face-analysis-route.md)).
- `Route.Editor(photoId, faces)`로 진입합니다. FaceAnalysis를 대체하므로 Editor를 나가면 Gallery로 돌아갑니다.
- 화면 이미지는 `PhotoLibrary.thumbnail(photoId, 2048)`입니다. 저장 이미지는 ③에서 원본 해상도로 따로 렌더링합니다.
- 얼굴은 왼쪽부터 정렬합니다. 사진 위 가이드와 하단 목록이 같은 순서를 씁니다.
- 모든 얼굴은 미선택으로 시작합니다(`Detected ≠ Selected`).
- `EditorState`는 얼굴 목록, 선택한 얼굴 집합과 이탈 확인 Dialog 표시 여부를 가집니다. `canSave`는 선택이 1개 이상일 때 참입니다(UX-039).

### 선택과 미리보기

- 사진 위 얼굴 가이드나 하단 목록 항목을 누르면 선택이 바뀝니다. 가이드가 겹치면 더 작은 얼굴을 고릅니다.
- 미선택 얼굴은 파란 1dp 가이드, 선택한 얼굴은 라임 2dp 가이드, 체크 표시와 모자이크 미리보기를 보여 줍니다.
- 모자이크 블록 한 칸은 얼굴 폭의 1/8입니다(2026-11-18 사용자 결정). 세로 칸 수는 얼굴 높이에 맞춥니다. 미리보기와 저장은 같은 규칙을 씁니다.
- 선택이 0개이면 `전체 선택`, 1개 이상이면 `전체 해제`를 보여 줍니다. 일부만 선택해도 `전체 해제`입니다.
- 하단 목록은 각 얼굴을 원본 사진에서 정사각형으로 잘라 보여 줍니다. 얼굴 크기의 1.4배를 잘라 여백을 둡니다(UX-041).
- 상단 카운터는 검출 수와 선택 수를 보여 줍니다.

### 뒤로가기

- 상단 뒤로가기와 시스템 뒤로가기는 선택 여부와 관계없이 이탈 확인 Dialog를 엽니다(Figma `316:4294`).
- `나가기`는 Editor를 닫고 Gallery의 이전 위치로 돌아갑니다. `계속 편집`은 Dialog를 닫고 선택을 유지합니다.

## 실패와 복구

| 상황 | 결과 |
| --- | --- |
| 화면 이미지를 읽을 수 없음 | 사진 영역을 비워 둡니다. 얼굴 목록과 선택은 그대로 동작합니다 |
| 저장 실패 | ③에서 정의합니다 |

## 플랫폼 차이

공통 코드로 구현합니다. 시스템 뒤로가기는 Navigation 3에 포함된 `navigationevent-compose`의 `NavigationBackHandler`로 받습니다.

## Acceptance Criteria

### ① 선택과 미리보기

- [x] 모든 얼굴은 미선택으로 시작하고, 사진 위 가이드와 목록 항목을 눌러 선택/해제한다
- [x] 선택한 얼굴에 모자이크 미리보기가 바로 보인다
- [x] 선택 0개이면 `전체 선택`과 비활성 저장, 1개 이상이면 `전체 해제`와 활성 저장
- [x] 뒤로가기는 이탈 확인 Dialog를 열고, `나가기`는 Gallery, `계속 편집`은 선택을 유지한다
- [x] `EditorTest`: 선택·전체 선택/해제·저장 상태, 이탈 확인, 모자이크 칸 수, 좌표 변환, 얼굴 자르기

### ②·③

해당 단계의 Issue에서 이 문서를 갱신합니다.
