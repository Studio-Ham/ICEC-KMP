# ICEC

ICEC(Mosaic Editor Creator)는 기기 사진에서 얼굴을 자동 검출하고, 사용자가 선택한 얼굴에만 모자이크를 적용해 새 이미지로 저장하는 Kotlin Multiplatform 앱입니다.

## 개발 기준

이 저장소는 ICEC의 **개발 SSOT(Single Source of Truth)** 입니다. 승인된 아키텍처, 실행 가능한 기술 명세, 개발 규칙과 ADR은 [개발 SSOT 인덱스](docs/README.md)에서 관리합니다.

- 제품 요구사항과 UX 정책: [ICEC LLM Wiki](https://github.com/Studio-Ham/ICEC-LLM-WiKi)
- 화면, 컴포넌트와 디자인 토큰: Figma
- 아키텍처, 기술 명세와 구현: Git Repository

## 현재 상태

Project Bootstrap 단계입니다. Android(`androidApp`)와 iOS(`iosApp`)에서 공통 Compose 화면(`composeApp`)을 실행하는 최소 골격이 있습니다. 기능 구현은 아직 시작하지 않았습니다. 로컬 빌드와 검증 명령은 [품질 전략](docs/development/quality.md#로컬-검증)을 따릅니다.

## 주요 문서

- [Architecture v1](docs/architecture/overview.md)
- [모듈 경계](docs/architecture/module-boundaries.md)
- [애플리케이션 아키텍처](docs/architecture/application-architecture.md)
- [플랫폼 기능 경계](docs/architecture/platform-capabilities.md)
- [개발 워크플로우](docs/development/workflow.md)
- [품질 전략](docs/development/quality.md)
- [기술 명세](docs/specs/README.md)
- [ADR](docs/adr/README.md)
