# 기술 백로그

> 상태: Planned
> SSOT: Git Repository
> 최종 검토일: 2026-11-28

아래 항목은 Architecture v1의 확정 구현 규칙이 아닙니다. 구현 전에 설계를 구체화하고, 확정 결과를 Spec 또는 ADR로 승격해야 합니다.

확정된 사용자 동작은 ICEC LLM Wiki의 화면별 UX와 UX 결정을 따릅니다. 아래에서 검토하는 대상은 해당 동작의 구현 방법입니다.

## 세션과 작업 수명주기

- [UX-034](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-034)·[UX-040](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-040)에 따른 프로세스 생존 중 작업 유지, 프로세스 종료 후 새 세션 시작과 Navigation 초기화 구현
- [Editor UX](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/editor.md)의 분석·저장 결과 보관, Foreground 복귀 시 반영과 중복 실행 방지
- OS 일시 정지·실행 시간 만료·프로세스 종료 구분과 플랫폼별 실행 제약 처리
- 분석 취소 가능 범위, 이전 결과 무효화와 재시도·Timeout 경합 처리
- [Gallery UX](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/gallery.md)의 권한·목록 재조회, 유효한 스크롤 anchor 유지와 삭제된 anchor 복구 구현
- [Result UX](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/result.md)의 저장 직전 Editor 세션·Zoom/Pan 복원과 back stack 조립

## Editor

- Pinch Zoom과 Pan의 상태 모델 및 제스처 우선순위
- 이미지 좌표계와 화면 좌표계 변환, inverse transform과 얼굴 hit-test
- Zoom/Pan 중 사진, Bounding Box와 Mosaic Preview의 정합성
- 선택된 얼굴, Zoom/Pan과 저장 직전 상태의 생명주기 및 복원 범위

## 미디어와 렌더링

- EXIF orientation과 Android/iOS 좌표계 차이: 원본·검출 정규화는 [ADR-0005](../adr/0005-basic-photo-edit.md)로 확정. Android API 24~28 썸네일은 `MINI_KIND` 대신 축소 디코딩과 같은 방향 행렬을 사용(2026-11-28). 구형 에뮬레이터 실측은 미실행
- 48MP급·파노라마 사진의 기본 편집 저장 메모리와 시간
- 고해상도 이미지의 decode, downsampling, memory pressure와 OOM 대응
- 대용량 사진 저장의 메모리 상한. 현재는 원본 해상도 이미지 두 장을 메모리에 올립니다([ADR-0004](../adr/0004-common-mosaic-rendering.md))
- MediaStore와 Photos 저장 결과의 공통 모델 및 오류 매핑

## 플랫폼 연동

- Android/iOS 제한 사진 접근 권한과 재요청 가능 상태 모델
- iOS 26 Liquid Glass 적용 범위와 하위 버전 폴백
- Metro의 iOS 그래프 조립과 빌드 호환성

## 검증 기반

- 대표 Android/iOS 기준 기기
- 승인된 테스트 이미지와 골든 이미지 세트
- 벤치마크 반복 횟수, warm-up과 회귀 허용 범위
- CI에서 실행할 iOS 통합 테스트의 blocking 범위
