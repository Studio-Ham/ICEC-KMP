# 개발 워크플로우

> 상태: Accepted
> SSOT: Git Repository
> 최종 검토일: 2026-11-17

## 작업 추적

기본 흐름은 다음과 같습니다.

```text
Wiki 요구사항 → GitHub Issue → Orca Worktree → Git Branch → Pull Request → CI/Review → Issue Close
```

- [ICEC LLM Wiki](../references/llm-wiki.md)는 제품 Roadmap과 UX 요구사항을 관리합니다.
- 승인되어 개발로 전환된 작업은 GitHub Issue를 장기 백로그와 Acceptance Criteria의 기준으로 사용합니다.
- Orca는 현재 실행 중인 작업의 격리된 작업 공간, 담당과 Checkpoint를 관리합니다.
- 하나의 Issue, Worktree, Branch와 PR을 가능한 한 1:1로 연결합니다.
- 사용자가 특정 작업에 PR 생략이나 직접 푸시를 명시적으로 요청하면 해당 범위에서 그 지시를 우선합니다.

## Git Flow

- `main`: 실제 프로덕트 기준.
- `develop`: 다음 출시를 위한 통합 기준.
- 작업 브랜치는 `develop`에서 생성합니다.
- Release 브랜치는 `develop`, hotfix 브랜치는 `main`에서 생성합니다.

| 작업 | 브랜치 예시 |
| --- | --- |
| 기능 | `feat/24-editor` |
| 수정 | `fix/31-gallery-permission` |
| 리팩터링 | `refactor/18-navigation` |
| 문서 | `docs/repository-ssot` |
| 빌드 | `build/12-project-bootstrap` |
| 벤치마크 | `benchmark/31-mosaic` |
| 릴리스 | `release/android-1.0.0-ios-1.0.0` |
| QA | `qa/42-editor-flow` |
| 긴급 수정 | `hotfix/51-save-crash` |

브랜치 이름은 실행 에이전트가 아니라 완료할 결과를 나타냅니다. Issue가 있다면 작업 유형 뒤에 Issue 번호와 짧은 목적을 둡니다.

## Commit

형식은 `[type]: [job]`입니다.

- `feat`: 새로운 기능
- `init`: Composable, ViewModel 등 초기 파일
- `fix`: 기존 기능 수정
- `chore`: 이동, 이름 변경, 포맷과 가독성
- `refactor`: 동작을 바꾸지 않는 내부 개선
- `build`: Gradle, Version Catalog, build-logic와 의존성
- `docs`: 문서
- `hotfix`: hotfix 브랜치의 긴급 수정

## Pull Request

제목에는 `[Feature]`, `[Fix]`, `[Refactor]`, `[Build]`, `[Docs]`, `[Chore]`, `[QA]`, `[Release]`, `[Hot Fix]` 중 작업 성격에 맞는 접두어를 사용합니다.

본문에는 관련 이슈, 작업한 내용과 PR 포인트를 기록합니다. CI 품질 게이트와 코드·요구사항 검토에서 blocking 문제가 해소된 뒤 Ready to Merge로 판단하며 `main` 병합은 사용자가 최종 승인합니다.

## 문서 동기화

- 코드 변경으로 기술 명세가 바뀌면 관련 Repository 문서를 같은 변경 단위에서 갱신합니다.
- Architecture v1의 경계나 핵심 기술 선택이 바뀌면 ADR을 추가합니다.
- 제품·UX 변경은 사용자 승인 후 Wiki 정책을 먼저 갱신합니다.
- 시각 변경은 Figma의 화면, Component, Token과 Variant를 먼저 확정합니다.
- 단계가 바뀌면 [Wiki 진행 현황](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/planning/context.md)을 실제 결과와 맞춥니다. 절차는 [진행 현황 동기화 규칙](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/wiki/progress-sync.md)을 따릅니다.

## Agent 운영

- 역할명과 무관하게 모든 Agent는 같은 Repository SSOT와 검증 절차를 따릅니다.
- Codex는 구현, 아키텍처와 기술 검증을 중심으로 검토합니다.
- Antigravity는 제품 요구사항, UX와 문서 일관성을 중심으로 검토합니다.
- 반복되는 절차가 실제로 확인되기 전에는 프로젝트 Skill을 만들지 않습니다.
- Skill이 필요해지면 역할명이 아니라 `architecture-decision`, `feature-implementation`, `design-implementation`, `verification`, `documentation-sync`처럼 재사용 가능한 작업을 기준으로 구성합니다.
