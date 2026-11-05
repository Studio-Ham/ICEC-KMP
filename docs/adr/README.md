# Architecture Decision Records

> 상태: Accepted
> SSOT: Git Repository
> 최종 검토일: 2026-09-10

ADR은 Architecture v1 이후 아키텍처 경계, 핵심 기술, 테스트·CI 정책에 의미 있는 변화가 생길 때 작성합니다. Architecture v1 자체는 현재 기준 문서로 이관했으므로 과거 결정을 소급해 여러 ADR로 만들지 않습니다.

## 파일 규칙

- 형식: `NNNN-short-title.md`
- 번호는 `0001`부터 순서대로 증가합니다.
- 상태는 `Proposed`, `Accepted`, `Superseded`, `Rejected` 중 하나입니다.
- Accepted ADR의 결정이 바뀌면 기존 문서를 덮어쓰지 않고 새 ADR에서 대체 관계를 기록합니다.
- 결정과 함께 영향을 받는 Architecture, Spec, 코드와 테스트를 같은 변경 단위에서 갱신합니다.

새 ADR은 [템플릿](template.md)을 복사해 작성합니다.

## 현재 ADR

- [ADR-0001: FaceAnalysis 별도 Navigation Route 도입](0001-face-analysis-route.md) — Accepted (2026-11-06)
