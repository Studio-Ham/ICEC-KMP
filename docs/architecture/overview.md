# Architecture v1

> 상태: Accepted
> SSOT: Git Repository
> 원본: [Notion 기술 설계 및 개발 기획서](https://app.notion.com/p/3d44d984a2188123b909ce5fb78a82ac)
> 승인일: 2026-09-08
> 이관 검토일: 2026-09-10

## 목표

- 실제 동작하는 Android/iOS 제품을 완성하면서 KMP 플랫폼 경계, 네이티브 연동, 온디바이스 ML, 성능 측정, 테스트 자동화와 CI를 명시적인 결과물로 관리합니다.
- 최신 기술의 채택 자체보다 해결하려는 문제와 트레이드오프를 기록하고 빌드, 테스트와 성능 결과로 검증합니다.
- 빠른 출시보다 기술 실험의 근거와 재현 가능한 포트폴리오 결과를 우선합니다.

## 플랫폼과 UI

- Kotlin Multiplatform으로 Android와 iOS를 지원합니다.
- Home, Gallery, FaceAnalysis, Editor, Result의 제품 UI는 Compose Multiplatform으로 최대한 공통 구현합니다.
- iOS 최소 지원 버전은 ML Kit iOS 지원 하한에 맞춰 15.5 이상으로 둡니다.
- 플랫폼 경험을 살릴 가치가 있는 제한된 컨트롤에만 Native interop을 허용합니다.
- iOS 26 이상에서 일부 내비게이션 컨트롤에 Liquid Glass를 적용하고 iOS 15.5~25에서는 같은 기능의 일반 스타일로 폴백합니다.

## 핵심 원칙

- 제품 요구사항은 Notion PRD가 소유합니다.
- 플랫폼 구현은 인터페이스와 명확한 모듈 경계 뒤에 둡니다.
- ViewModel과 feature가 `Bitmap`, `UIImage`, `CGImage` 같은 네이티브 타입에 직접 의존하지 않게 합니다.
- 기술 결정과 트레이드오프는 ADR로 기록합니다.
- 위험도가 높은 interop과 성능 요소는 구현 범위를 작게 나누고 각 단계에서 검증합니다.
- 추상화와 모듈은 책임 분리, 의존성 경계 또는 테스트 격리라는 실제 목적이 있을 때 추가합니다.

## 현재 단계

Architecture v1은 승인되었습니다. Repository 개발 SSOT를 기반으로 정식 Project Bootstrap을 준비합니다. 별도의 iOS ML Kit 선행 검증 단계는 두지 않으며, 해당 연동은 실제 기능 구현 과정에서 검증합니다.

Architecture v1의 경계나 핵심 기술 선택을 변경할 때는 [ADR](../adr/README.md)을 먼저 작성하고 관련 현재 기준 문서를 함께 갱신합니다.

- 2026-11-06: 별도 `FaceAnalysis` Route 도입 및 기존 `feature/editor` 모듈 내 역할 분리는 [ADR-0001](../adr/0001-face-analysis-route.md)에 기록했습니다. 원본 UX 결정 [UX-037](https://app.notion.com/p/3ec4d984a218816eb3b7f4d9a9906b26)을 반영한 후속 설계이며, 앱 구현 완료를 뜻하지 않습니다.
