# ICEC LLM Wiki 참조 절차

> 상태: Reference
> SSOT: 각 연결된 원본
> Wiki 저장소: [Studio-Ham/ICEC-LLM-WiKi](https://github.com/Studio-Ham/ICEC-LLM-WiKi)
> 최종 검토일: 2026-11-05

ICEC LLM Wiki는 Notion 제품·UX 정책의 사본, `UX-###` 결정 참조, Figma 링크와 계획 문서를 검색하기 위한 보조 지식 저장소입니다. **SSOT가 아닙니다.** [SSOT 소유권](../README.md#ssot-소유권)은 그대로입니다. 제품·UX는 Notion, 시각 디자인은 Figma, 기술 기준과 구현은 이 저장소가 원본입니다. Wiki 내용과 원본이 다르면 원본을 따릅니다.

## 준비

- Wiki 저장소의 로컬 경로는 작업 환경에서 확인합니다. 아래 예시는 그 경로를 `ICEC_WIKI_ROOT`에 둔다고 가정합니다.
- Python 3.10 이상의 표준 라이브러리만 사용합니다. 설치할 패키지·외부 서비스·API 키가 없습니다.
- Wiki 저장소를 찾을 수 없으면 Wiki 확인을 건너뛰고 아래 원본을 직접 확인합니다. 결과 보고에 건너뛴 이유를 남깁니다.

```bash
wiki() { PYTHONPATH="$ICEC_WIKI_ROOT" python3 -m tools.wiki --root "$ICEC_WIKI_ROOT" "$@"; }
```

## 작업 시작 시 확인

[Agent 확인 순서](../README.md#agent-확인-순서)에서 Notion 요구사항과 Figma 노드를 찾을 때 Wiki를 사용합니다.

1. 작업 키워드나 `UX-###`로 현재 유효한 지식을 검색합니다.

   ```bash
   wiki search 권한 거부 --current
   wiki search UX-022
   ```

2. 결과의 ID로 본문과 출처를 엽니다. 결과와 열람 화면에는 원본 URL, 대조일과 경고가 표시됩니다.

   ```bash
   wiki read UX-022
   wiki read policy.gallery#permissionrequired
   ```

3. 표시된 원본 링크로 Notion 요구사항과 Figma 노드를 확인합니다. 다음 경고가 있으면 Wiki 내용을 근거로 쓰지 않고 원본을 직접 확인합니다.
   - `대체됨`·`폐기`: 대체한 결정이나 현재 기준을 확인합니다.
   - `출처 미대조`·`출처 N일 경과`: 원본과 다를 수 있습니다.
   - `승인 대기 제안`: 승인되지 않은 변경입니다.
4. 결과가 없으면(종료 코드 1) Wiki에 근거가 없다는 뜻입니다. 정책을 추측하지 않고 원본을 확인합니다. 원본에서도 확인되지 않으면 사용자에게 확인합니다.
5. 기술 기준은 Wiki가 아니라 이 저장소의 [Architecture](../architecture/overview.md), [Spec](../specs/README.md), [ADR](../adr/README.md)을 확인합니다. Wiki는 이 문서들의 위치와 상태만 기록하고 본문을 복제하지 않습니다.
6. PR의 `Decisions / Deviations`에는 근거로 사용한 `UX-###`와 원본 링크를 기록합니다. Wiki ID는 보조 참조로만 적습니다.

## 지식 변경이 필요한 경우

에이전트는 Notion, Figma, Wiki의 원본 정책을 자동으로 수정하지 않습니다. 차이를 발견하면 다음 경로로 검토를 요청합니다.

| 상황 | 처리 |
|---|---|
| Wiki 사본이 Notion·Figma 원본과 다름 | 원본을 기준으로 작업합니다. Wiki 저장소의 `proposals/`에 제안을 남기거나 사본 갱신을 요청합니다. |
| 제품·UX 정책 변경이 필요함 | 사용자 승인 후 Notion 원본을 먼저 갱신합니다. 그 다음 Repository 문서와 Wiki 사본을 맞춥니다. |
| 시각 디자인 변경이 필요함 | Figma에서 먼저 확정한 뒤 Compose 구현 계약을 갱신합니다. |
| 기술 기준 변경이 필요함 | 이 저장소의 PR로 변경합니다. Architecture v1을 바꾸면 [ADR](../adr/README.md)을 작성합니다. |
| Wiki 내용이 이 저장소의 기술 문서와 다름 | 이 저장소의 문서를 따릅니다. Wiki 쪽 수정을 제안합니다. |

Wiki의 제안 작성 형식과 반영 절차는 Wiki 저장소의 `docs/wiki/usage.md`(승인 대기 변경)를 따릅니다.
