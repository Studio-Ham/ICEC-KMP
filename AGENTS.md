# AI Agent Harness Instructions

## 1. 역할

Kotlin Multiplatform, Compose Multiplatform과 Android/iOS 플랫폼 연동에 숙련된 모바일 개발자로서 확장 가능하고 유지보수 가능한 코드를 작성합니다.

## 2. Project Context (프로젝트 개요)

- **Project Name**: Icec-Compose (Mosaic Editor Creator)
- **Core MVP**:
  1. 기기에서 이미지를 불러와 사람 얼굴을 자동으로 검출
  2. 검출된 얼굴 중 사용자가 모자이크할 대상을 직접 선택
  3. 선택된 얼굴에만 모자이크를 적용한 뒤 새로운 이미지로 저장
- **Future Expansion**: 이미지 편집 기능 및 카메라 촬영 기능 추가 예정
- **Target**: Android, iOS
- **UI Toolkit**: Compose Multiplatform (Material 3)
- **Language**: Kotlin (최신 버전 유지)

## 3. 개발 SSOT

- 작업 전 [개발 SSOT 인덱스](docs/README.md)와 관련 Architecture, Spec, ADR을 확인합니다.
- 제품 요구사항·UX 정책·UX 결정은 [ICEC LLM Wiki](docs/references/llm-wiki.md)를 따릅니다.
- 시각 디자인은 Figma를 따릅니다.
- 기술 기준과 구현은 이 저장소를 따릅니다.
- Notion은 더 이상 관리하지 않습니다. Notion 내용을 기준으로 사용하지 않습니다.
- 작업 시작 시 [ICEC LLM Wiki 참조 절차](docs/references/llm-wiki.md)에 따라 관련 제품·UX 정책과 `UX-###` 결정을 검색합니다.
- Wiki의 정책과 Figma 디자인을 자동으로 수정하지 않습니다. 변경이 필요하면 [참조 절차의 검토 경로](docs/references/llm-wiki.md#지식-변경이-필요한-경우)를 따릅니다.
- 코드와 Repository 기술 문서가 충돌하면 둘 중 하나를 임의로 우선하지 않고 같은 변경에서 정합성을 복구합니다.
- 기술 결정이 Architecture v1을 변경하면 ADR을 작성하고 현재 기준 문서를 함께 갱신합니다.
- 대화 기록보다 현재 checkout된 저장소 문서를 우선합니다.

## 4. Tech Stack & Architecture (기술 스택 및 아키텍처)

- **Architecture**: UDF 기반 MVVM/MVI 스타일을 사용합니다.
- **Asynchronous**: 비동기 처리는 `Coroutines`와 `StateFlow`/`SharedFlow`를 사용.
- **UI Definition**: 제품 UI는 Compose Multiplatform으로 최대한 공통 구현합니다.
- **Platform Boundary**: Android/iOS SDK와 네이티브 타입은 플랫폼 모듈 내부에 격리합니다.
- **Dependency Management**: 의존성은 반드시 `gradle/libs.versions.toml` (Version Catalog)를 통해 중앙 집중식으로 관리.

## 5. Compose Coding Conventions (컴포즈 코딩 규칙)

- **Modifiers**: 모든 재사용 가능한 Composable 함수는 `modifier: Modifier = Modifier`를 첫 번째 선택 매개변수로 받아야 함.
- **State Hoisting**: 상태를 최대한 위로 끌어올려(Hoisting) Composable을 상태가 없는(Stateless) 형태로 유지. (State는 아래로, Event는 위로 전달)
- **Business Logic**: Composable 내부에는 비즈니스 로직을 두지 않고, ViewModel 또는 도메인 계층에 위임.
- **Previews**: 독립적인 UI 컴포넌트를 작성할 때는 반드시 `@Preview`를 작성하여 시각적 확인이 가능하도록 할 것.

## 6. Agent Workflow & Feedback Loops (에이전트 작업 및 검증 루프)

Gradle Wrapper가 구성된 이후 코드를 수정하거나 작성하면 다음 명령어로 검증합니다:

1. **Linter 검사 및 자동 수정**: `./gradlew spotlessApply spotlessCheck`
2. **빌드 검증**: `./gradlew assembleDebug`
3. **테스트 검증**: `./gradlew test allTests` (테스트 코드가 존재할 경우)
4. Gradle은 JDK 21 이상에서 실행합니다.

문서만 변경하거나 아직 Wrapper가 없다면 `git diff --check`와 링크·문서 구조 검증을 수행하고 Gradle 검증이 적용되지 않는 이유를 결과에 남깁니다.

브랜치, 커밋, Issue, Worktree와 PR 운영은 [개발 워크플로우](docs/development/workflow.md)를 따릅니다.

## 7. Prohibited Patterns (금지된 패턴)

- 안드로이드 `View` 시스템(XML)과의 불필요한 Interop 금지.
- `GlobalScope` 사용 금지 (Lifecycle-aware CoroutineScope 사용).
- UI 문자열은 Compose Multiplatform Resources를 기본으로 관리하고 플랫폼 전용 문자열만 각 플랫폼 리소스에 둡니다.

## 8. 지침서 작성 규칙

- `AGENTS.md`와 작업 절차 등 지침서를 작성하거나 수정할 때는 반드시 `asd-ste100` 스킬을 사용합니다.
- 한 문장에 한 지시만 씁니다. 기존 조건, 예외, 승인 상태와 원본 링크를 유지합니다.
- 한국어 지침에도 짧은 문장, 명확한 주체와 일관된 용어를 적용합니다. 영어용 자동 검사와 별도로 한국어의 의미와 조건을 대조합니다.
