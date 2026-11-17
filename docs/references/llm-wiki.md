# ICEC LLM Wiki 참조 절차

> 상태: Reference
> SSOT: 제품·UX는 ICEC LLM Wiki
> Wiki 저장소: [Studio-Ham/ICEC-LLM-WiKi](https://github.com/Studio-Ham/ICEC-LLM-WiKi)
> 최종 검토일: 2026-11-17

ICEC LLM Wiki는 ICEC 제품 요구사항·UX 정책·`UX-###` 결정의 **SSOT**입니다. 2026-11-17 사용자 결정으로 Notion에서 옮겼고 Notion은 더 이상 관리하지 않습니다. 시각 디자인은 Figma, 기술 기준과 구현은 이 저장소가 원본입니다([SSOT 소유권](../README.md#ssot-소유권)). Wiki에는 Figma 링크와 계획 문서도 있습니다.

## 준비

- Wiki 저장소의 로컬 경로는 작업 환경에서 확인합니다. 아래 예시는 그 경로를 `ICEC_WIKI_ROOT`에 둔다고 가정합니다.
- Python 3.10 이상의 표준 라이브러리만 사용합니다. 설치할 패키지·외부 서비스·API 키가 없습니다.
- Wiki 저장소를 찾을 수 없으면 [GitHub의 Wiki 저장소](https://github.com/Studio-Ham/ICEC-LLM-WiKi)에서 `docs/policies/`를 직접 읽습니다. 결과 보고에 그 이유를 남깁니다.

```bash
wiki() { PYTHONPATH="$ICEC_WIKI_ROOT" python3 -m tools.wiki --root "$ICEC_WIKI_ROOT" "$@"; }
```

## 작업 시작 시 확인

[Agent 확인 순서](../README.md#agent-확인-순서)에서 제품·UX 요구사항과 Figma 노드를 찾을 때 Wiki를 사용합니다.

1. 작업 키워드나 `UX-###`로 현재 유효한 지식을 검색합니다.

   ```bash
   wiki search 권한 거부 --current
   wiki search UX-022
   ```

2. 결과의 ID로 본문을 엽니다. 결과와 열람 화면에는 문서 종류와 경고가 표시됩니다. Fact 문서에는 외부 원본 URL과 대조일도 표시됩니다.

   ```bash
   wiki read UX-022
   wiki read policy.gallery#permissionrequired
   ```

3. `policy`·`decision` 문서는 Wiki 자체가 원본입니다. 시각 결과는 표시된 Figma 노드에서 확인합니다. 다음 경고가 있으면 해당 내용을 현재 기준으로 쓰지 않습니다.
   - `대체됨`·`폐기`: 대체한 결정이나 현재 기준을 확인합니다.
   - `출처 미대조`·`출처 N일 경과`: Fact 문서가 Figma·이 저장소와 다를 수 있습니다. 외부 원본을 직접 확인합니다.
   - `승인 대기 제안`: 승인되지 않은 변경입니다.
4. 결과가 없으면(종료 코드 1) Wiki에 근거가 없다는 뜻입니다. 정책을 추측하지 않습니다. 사용자에게 확인합니다.
5. 기술 기준은 Wiki가 아니라 이 저장소의 [Architecture](../architecture/overview.md), [Spec](../specs/README.md), [ADR](../adr/README.md)을 확인합니다. Wiki는 이 문서들의 위치와 상태만 기록하고 본문을 복제하지 않습니다.
6. PR의 `Decisions / Deviations`에는 근거로 사용한 `UX-###`와 Wiki 문서 링크를 기록합니다.

## 지식 변경이 필요한 경우

에이전트는 Wiki의 정책과 Figma 디자인을 자동으로 수정하지 않습니다. 차이를 발견하면 다음 경로로 검토를 요청합니다.

| 상황 | 처리 |
| --- | --- |
| Wiki의 Figma 참조(Fact)가 Figma와 다름 | Figma를 기준으로 작업합니다. Wiki 저장소의 `proposals/`에 갱신 제안을 남깁니다. |
| 제품·UX 정책 변경이 필요함 | Wiki 저장소의 `proposals/`에 제안을 남깁니다. 사용자 승인 후 Wiki 정책을 먼저 갱신합니다. 그 다음 Repository 문서와 구현을 맞춥니다. |
| 시각 디자인 변경이 필요함 | Figma에서 먼저 확정한 뒤 Compose 구현 계약을 갱신합니다. |
| 기술 기준 변경이 필요함 | 이 저장소의 PR로 변경합니다. Architecture v1을 바꾸면 [ADR](../adr/README.md)을 작성합니다. |
| Wiki 내용이 이 저장소의 기술 문서와 다름 | 해당 **기술 기준**은 이 저장소의 문서를 따릅니다. Wiki 쪽 수정을 제안합니다. |
| Wiki의 Confirmed UX 결정이 이 저장소의 Navigation·Architecture 문서와 다름 | 어느 쪽도 임의로 무시하지 않습니다. Wiki UX 결정과 기술 경계를 대조하고 기술 문서·ADR을 갱신해 정합성을 복구합니다([ADR-0001](../adr/0001-face-analysis-route.md) 참조). |

Wiki의 제안 작성 형식과 반영 절차는 Wiki 저장소의 `docs/wiki/usage.md`(승인 대기 변경)를 따릅니다.
