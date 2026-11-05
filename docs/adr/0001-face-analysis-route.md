# ADR-0001: FaceAnalysis 별도 Navigation Route 도입

> 상태: Accepted
> 결정일: 2026-11-06
> 대체 대상: Architecture v1의 Editor 내부 얼굴 분석 Navigation 방식
> 기준 원본: [Notion UX-037](https://app.notion.com/p/3ec4d984a218816eb3b7f4d9a9906b26), [UX-038](https://app.notion.com/p/3ec4d984a218819081cfeee89e6114c0)

## Context

[애플리케이션 아키텍처](../architecture/application-architecture.md)의 2026-09-10 기준 Navigation 규칙은 얼굴 분석을 Editor 내부 상태로 취급했습니다. 하지만 Notion UX-037 (2026-10-01, Confirmed)은 이전 UX-001을 대체하며 `Gallery → FaceAnalysis → Editor → Result` 흐름과 독립된 FaceAnalysis 화면을 확정했습니다. UX-038은 실패와 시간 초과를 별도 안내 화면으로 확정했습니다. 실제 구현 전 기술 설계와 승인된 UX를 일치시켜야 합니다.

## Options

1. 기존 Editor 내부 분석 상태 유지: 모듈은 단순하지만 Confirmed UX-037에 어긋납니다.
2. FaceAnalysis를 별도 Route로 분리하고 기존 `feature/editor` 모듈에서 함께 관리: UX 정책을 따르면서 지금 단계에서 불필요한 Gradle 모듈 증가를 피합니다.
3. FaceAnalysis 전용 Gradle 모듈 추가: 책임 경계는 명확하지만 정식 Bootstrap 전인 현재에는 별도 모듈의 비용·효용이 검증되지 않았습니다.

## Decision

**2번을 선택합니다.**

- 별도 `FaceAnalysis` Route에서 얼굴 검출을 수행하고, 성공 시 `Editor`로 전환합니다.
- 얼굴 미검출은 `NoFaceDetected`, 실패는 `AnalysisFailed`, 시간 초과는 `AnalysisTimeout`으로 전환합니다. 오류·시간 초과 화면에서는 같은 사진 재분석 또는 Gallery 복귀를 지원합니다.
- Gallery의 선택·스크롤 위치와 Back 동작은 Notion UX-037 및 UX-038을 따릅니다.
- `feature/editor` 모듈은 초기에는 관련 Route들을 함께 소유하지만 화면별 상태·이벤트는 분리합니다. 향후 독립 모듈 필요성이 확인되면 별도 ADR로 판단합니다.
- 제품·UX 원본은 계속 Notion이며, 이 ADR은 승인된 UX의 **기술적 구현 경계**를 기록합니다.

## Consequences

- [Architecture v1](../architecture/overview.md), [모듈 경계](../architecture/module-boundaries.md), [애플리케이션 아키텍처](../architecture/application-architecture.md)를 같은 PR에서 갱신합니다.
- 이 ADR은 구현 완료를 의미하지 않습니다. 실제 Route, 상태 모델, Platform interop과 테스트는 구현 작업에서 검증합니다.
- FaceAnalysis와 Editor 사이에서 전달하는 이미지·검출 결과 계약은 구현 시 관련 Spec에 기록합니다.

## Verification

- Notion UX-037의 Confirmed 상태와 별도 FaceAnalysis 정상 흐름을 대조합니다.
- Notion UX-038의 분석 오류·시간 초과 복구 흐름을 대조합니다.
- Architecture 문서 및 모듈 경계에서 과거 `Editor(Analyzing)`를 현재 정상 흐름으로 규정하지 않는지 확인합니다.
- Bootstrap 이후 Navigation 및 State/Effect 흐름에 대한 공통 단위 테스트와 Android/iOS UI 검증을 추가합니다.
