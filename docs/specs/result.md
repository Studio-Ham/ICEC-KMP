# Result

> 상태: Accepted
> SSOT: Git Repository
> 관련 요구사항: [Wiki Result UX](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/result.md) · Figma `[승인] 화면 5: 저장 완료` (`81:3156`)
> 관련 결정: [UX-043](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-043), [ADR-0004](../adr/0004-common-mosaic-rendering.md)
> 최종 검토일: 2026-11-19

## 목적

사진 보관함에 실제로 저장된 익명화 결과를 보여 주고 다음 행동을 고르게 합니다.

## 범위

- 저장 완료 안내, 저장된 결과 이미지, `다시 편집하기`, `다른 사진 편집하기`, `홈으로 이동`
- Pinch Zoom과 Pan은 Editor와 같은 `ZoomState` 규칙(최대 4배, 사진 밖 빈 영역 숨김)을 씁니다. Result의 배율은 Editor 배율을 바꾸지 않습니다.
- 공유 기능은 MVP에서 제외합니다(Wiki Result UX).

## 계약

- `feature/result`가 `ResultScreen`을 제공합니다. 상태가 저장 결과 이미지뿐이므로 ViewModel은 두지 않습니다.
- `Route.Result(savedPhotoId)`로 진입합니다. Editor 저장 성공 후에만 Editor 위에 추가됩니다.
- 이미지는 `PhotoLibrary.thumbnail(savedPhotoId, 2048)`로 읽은 실제 저장 결과입니다. Editor 미리보기를 쓰지 않습니다.
- 이미지 상자는 이미지 비율에 맞추고 12dp 둥근 모서리로 자릅니다. 1dp 테두리를 두르고 배경을 채우지 않습니다.
- one-pane 레이아웃은 문구와 이미지 사이에 24dp 간격을 둡니다. 이미지 최대 폭은 384dp로 제한합니다.
- two-pane 레이아웃은 왼쪽 영역에 이미지를 채우고 최대 폭을 384dp로 제한하지 않습니다.
- `다시 편집하기`는 Result를 back stack에서 뺍니다. 아래에 남은 Editor가 저장 직전 선택을 그대로 보여 줍니다.
- 시스템 뒤로가기는 `NavigationBackHandler`로 소비하고 Result 화면을 유지합니다([UX-043](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-043)).
- `다른 사진 편집하기`는 back stack을 Home만 남기고 새 `Route.Gallery(session)`을 추가합니다. 새 session이라 Gallery 상태와 스크롤 위치를 새로 만들고 최신 사진부터 보여 줍니다.
- `홈으로 이동`은 back stack을 Home만 남깁니다. Home에서 뒤로 가면 앱을 나갑니다.
- 다시 저장하면 새 사진이 하나 더 생깁니다. 이전 저장 결과는 그대로 둡니다.

## 실패와 복구

| 상황 | 결과 |
| --- | --- |
| 저장 결과 이미지를 읽을 수 없음 | 사진 영역을 비워 둡니다. 세 행동은 그대로 동작합니다 |

## 플랫폼 차이

공통 코드로 구현합니다. `NavigationBackHandler`가 시스템 뒤로가기를 소비하고 화면을 유지합니다([UX-043](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-043)).

## Acceptance Criteria

- [x] 실제 저장 결과를 이미지 비율에 맞춰 12dp 둥근 모서리로 보여 준다
- [x] one-pane에서 문구와 이미지 간격이 24dp이고 이미지 최대 폭은 384dp이다
- [x] `다시 편집하기`는 같은 편집 세션(선택 유지)으로 돌아간다
- [x] 시스템 뒤로가기와 iOS 스와이프 뒤로가기는 Result 화면을 유지한다
- [x] `다른 사진 편집하기`는 새 Gallery를 열고, 그 Gallery에서 뒤로 가면 Home이다
- [x] `홈으로 이동` 뒤 뒤로가기는 앱을 나간다
- [x] Pinch Zoom·Pan(#25)
