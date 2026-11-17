# 품질 전략

> 상태: Accepted
> SSOT: Git Repository
> 최종 검토일: 2026-11-17

## 테스트 계층

1. `commonMain` 단위 테스트: `Action → State → Effect`, 모자이크 정책과 네비게이션 규칙.
2. 플랫폼 통합 테스트: 실제 FaceDetector, MosaicRenderer, 사진 접근과 저장 경로.
3. 골든 이미지 테스트: 동일 입력과 파라미터에서 승인된 결과 이미지 회귀 확인.
4. Compose UI 테스트: `Home → Gallery → FaceAnalysis → Editor(Saving) → Result`, 얼굴 선택·해제와 오류 상태.
5. 성능 벤치마크: 얼굴 검출, 모자이크 렌더링, 저장 시간과 peak memory.

얼굴 검출은 모델 특성을 고려해 검출 수와 bounding box 허용 오차를 검증합니다. 결정적인 픽셀 처리가 가능한 모자이크 렌더러는 승인된 출력과 직접 비교합니다.

## UX 검증 기준

- 분석 화면과 복구 경로는 [ADR-0001](../adr/0001-face-analysis-route.md)을 따릅니다. 정상 검출, 미검출, 오류와 시간 초과를 구분합니다. 같은 사진 재시도와 Gallery 복귀를 검증합니다.
- 저장·뒤로가기·Result 이동은 [Wiki Editor UX](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/editor.md)와 [Result UX](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/result.md)를 따릅니다. 저장 중 입력 차단, 실제 저장 성공 후 Result 진입과 저장 실패 시 편집 상태 유지를 검증합니다.
- 프로세스 생존 중 상태 유지와 종료 후 초기화는 [UX-034](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-034)와 [UX-040](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-040)을 따릅니다. Background 복귀와 Cold Start를 구분합니다. 중복 분석·저장이 발생하지 않는지 검증합니다. 종료된 세션을 복원하지 않는지 검증합니다.

이 목록은 구현 후 검증할 범위입니다. 플랫폼 실행 제약, 취소와 결과 무효화 방식은 관련 Spec에서 구체화합니다.

## CI

- GitHub Actions에서 PR 품질 게이트와 `main` 또는 수동 benchmark workflow를 분리합니다.
- PR 단계에서는 Android/iOS 빌드, 공통·플랫폼 테스트, 정적 분석, 커버리지와 승인되지 않은 시각 회귀를 blocking gate로 사용합니다.
- 시각 회귀가 발생하면 diff 이미지를 artifact로 보존합니다.
- 무거운 iOS 통합 테스트와 실기기 검증은 CI 환경을 확인한 뒤 blocking 범위를 확정합니다.
- 성능 벤치마크는 변동성과 실행 시간을 고려해 모든 PR의 blocking gate로 사용하지 않습니다.

## 성능 기준 환경

- Android와 iOS에서 대표 기준 기기 한 대씩을 공식 baseline으로 지정합니다.
- 기준 기기는 실제 보유 장비와 테스트 가능성을 확인한 뒤 확정합니다.
- 동일한 이미지 세트, 반복 횟수, warm-up 조건과 Release 성격의 빌드 조건을 사용합니다.
- 다른 기기의 결과는 참고 측정값으로 분리하고 공식 baseline과 섞지 않습니다.
- OS, 기기, 빌드 설정과 측정 도구 버전을 결과와 함께 기록합니다.

## 버전 정책

- Kotlin, Gradle, AGP, Compose Multiplatform, ML Kit와 Metro는 도입 시점에 호환성을 확인한 최신 Stable 조합을 사용합니다.
- Preview 버전은 Stable 대안이 부족하고 명시적인 필요가 있을 때만 허용합니다.
- Preview 도입 이유와 롤백 기준은 ADR 또는 관련 기술 문서에 기록합니다.
- 버전 업그레이드는 별도 Issue와 변경 단위로 추적합니다.

## 로깅과 Crash Reporting

- 공통 로깅 계약과 플랫폼 구현을 분리합니다.
- Android와 iOS에서 치명적 크래시와 중요한 non-fatal 오류를 추적합니다.
- 얼굴 검출, 모자이크 렌더링과 저장 성능을 확인할 최소 trace/event를 기록합니다.
- 원본 사진, 얼굴 이미지와 개인 식별 가능 데이터는 로그나 Crash Reporting payload에 포함하지 않습니다.
- 사용자 행동 Analytics는 초기 범위에서 최소화하고 실제 필요가 생기면 별도 결정으로 확장합니다.

## 로컬 검증

Gradle Wrapper가 구성된 이후 코드 변경에는 다음 명령을 사용합니다.

```shell
./gradlew spotlessApply spotlessCheck
./gradlew assembleDebug
./gradlew test
```

문서만 변경하거나 Wrapper가 없는 단계에서는 `git diff --check`, 내부 링크, 외부 SSOT 링크와 문서 인덱스를 검증합니다.
