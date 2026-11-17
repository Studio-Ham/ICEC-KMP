# 제품·UX·디자인 참조

> 상태: Reference
> SSOT: 제품·UX는 ICEC LLM Wiki, 시각 디자인은 Figma
> 최종 검토일: 2026-11-17

## ICEC LLM Wiki

2026-11-17 사용자 결정으로 제품·UX 원본을 Notion에서 ICEC LLM Wiki로 옮겼습니다. Notion은 더 이상 관리하지 않습니다.

- [제품 요구사항 정의서](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/prd.md): 제품 범위, MVP와 Non-Goal의 SSOT.
- [UX 기획서](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-overview.md): 사용자 흐름, 상태와 UX 정책의 허브.
- [UX Decisions](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md): `UX-###` 결정의 상태, 근거와 요약.
- [프로젝트 진행 현황](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/planning/context.md): 단계별 상태와 현재·다음 작업을 요약합니다.

## Figma

2026-11-05 사용자 결정에 따라 다음 세 파일이 시각 디자인의 원본입니다. 각 파일의 운영 가이드는 `Accepted` 상태이고 기준일은 2026-09-10입니다. 2026-11-17에 가이드와 화면 노드를 대조했습니다.

- [ICEC-MVP](https://www.figma.com/design/K8uqTjJDkBnC1lNxfuICv0/ICEC-MVP?node-id=118-4): 화면·상태별 상세 디자인.
- [ICEC-Design-System](https://www.figma.com/design/Jn0xf2CaB8ZMPWWo3K0Ina/ICEC-Design-System?node-id=3-3): 공통 컴포넌트와 디자인 토큰.
- [ICEC-Assets](https://www.figma.com/design/etY1iHAlX8Ie0geV0WdIv6/ICEC-Assets?node-id=8-1807): 디자인 에셋.

운영 가이드는 다음 경계와 순서를 정합니다.

- ICEC-MVP는 화면 레이아웃, 상태 표현과 사용자 플로우를 소유합니다.
- ICEC-Design-System은 토큰과 TopAppBar·CTA·Snackbar 컴포넌트를 소유합니다.
- ICEC-Assets는 에셋의 시각 원본을 소유합니다. 실행 가능한 에셋 파일과 플랫폼 리소스는 이 저장소에서 관리합니다.
- 변경은 Wiki 확정, Figma 갱신, 이 저장소의 Spec·Compose 갱신, 시각·동작 검증의 순서로 진행합니다.

Figma 가이드 원문은 아직 제품·UX 원본을 Notion으로 적습니다. 이 역할은 Wiki가 맡습니다. 가이드의 `Accepted` 상태는 개별 화면의 승인을 뜻하지 않습니다. 2026-11-17 기준으로 화면 프레임에는 승인 표시가 없습니다. Figma로 옮겨진 사실만으로 화면을 승인본이나 구현 준비 완료로 보지 않습니다. 구현 전에 관련 화면·상태의 노드와 승인 여부를 확인합니다. 화면·상태 노드 대응과 확인된 불일치는 [ICEC LLM Wiki의 디자인 참조](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/design/references.md)에서 찾습니다. Variables, Styles, Components의 세부 구성은 디자인 작업에서 정비합니다.

## 연결 원칙

구현은 임의로 시각 결과를 추측하지 않습니다. 관련 Wiki 요구사항과 승인된 Figma 노드를 확인한 뒤 Repository의 Compose 계약과 코드를 함께 갱신합니다. Figma Code Connect를 도입한다면 반복 사용되는 디자인 시스템 컴포넌트에 한정하고 별도 ADR로 결정합니다.
