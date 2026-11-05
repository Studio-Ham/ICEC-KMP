# ICEC 개발 SSOT

> 상태: Accepted
> 소유 위치: Git Repository
> 원본: [Notion 기술 설계 및 개발 기획서](https://app.notion.com/p/3d44d984a2188123b909ce5fb78a82ac)
> 최종 검토일: 2026-11-05

이 디렉터리는 ICEC 구현에 적용되는 아키텍처, 기술 명세, 개발 규칙과 의사결정 기록의 단일 기준입니다.

## SSOT 소유권

| 대상 | SSOT | 역할 |
|---|---|---|
| 제품 범위와 요구사항 | Notion PRD | 사용자 문제, MVP, Non-Goal |
| 사용자 흐름과 UX 정책 | Notion UX | 상태, 상호작용, 실패 복구, UX 결정 |
| 시각 디자인 | Figma | 화면, 컴포넌트, Variant, Prototype, Design Token |
| 기술 기준과 구현 | Git Repository | Architecture, Spec, ADR, 코드, 테스트, CI |
| 개발 백로그 | GitHub Issues | 승인된 개발 작업과 Acceptance Criteria |
| 실행 중 작업 | Orca Worktree | 작업 공간, 담당, Checkpoint |

[ICEC LLM Wiki](references/llm-wiki.md)는 Notion·Figma 원본을 검색하기 위한 사본이며 SSOT가 아닙니다. Wiki와 원본이 다르면 원본을 따릅니다.

같은 내용을 여러 SSOT에 장기간 중복하지 않습니다. 제품이나 UX가 변경되면 Notion을 먼저 갱신하고, 영향을 받는 Repository 문서와 구현을 같은 변경 단위에서 맞춥니다. 시각 디자인이 변경되면 Figma를 먼저 확정한 뒤 Compose 구현 계약을 갱신합니다.

Repository 문서와 코드가 충돌하는 상태는 우선순위로 덮지 않고 결함으로 취급합니다. 실제 동작과 의도 중 무엇이 맞는지 확인한 뒤 같은 PR 또는 사용자가 승인한 동등한 변경 단위에서 함께 수정합니다.

## 문서 지도

### Architecture

- [Architecture v1](architecture/overview.md)
- [모듈 경계](architecture/module-boundaries.md)
- [애플리케이션 아키텍처](architecture/application-architecture.md)
- [플랫폼 기능 경계](architecture/platform-capabilities.md)

### Development

- [개발 워크플로우](development/workflow.md)
- [품질 전략](development/quality.md)
- [기술 백로그](development/technical-backlog.md)

### Records and contracts

- [기술 명세](specs/README.md)
- [ADR](adr/README.md)
- [제품·UX·디자인 참조](references/product-design.md)
- [ICEC LLM Wiki 참조 절차](references/llm-wiki.md)
- [Legacy 문서](legacy/DESIGN.md)

## Agent 확인 순서

1. 루트 `AGENTS.md`
2. 이 인덱스와 작업에 관련된 Architecture 문서
3. 관련 Spec과 Accepted ADR
4. 연결된 Notion 요구사항과 승인된 Figma 노드. [ICEC LLM Wiki](references/llm-wiki.md)로 검색한 뒤 표시된 원본에서 확인합니다.
5. 현재 코드와 테스트

대화 기록이나 임시 작업 메모는 이 목록의 문서를 대체하지 않습니다.
