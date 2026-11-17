# Gallery

> 상태: Draft
> SSOT: Git Repository
> 관련 요구사항: [Wiki Gallery UX](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/gallery.md) · Figma `[승인] 화면 2` (`316:2412`, `316:2477`, `316:2506`, `316:2532`, `340:294`, `340:326`)
> 관련 결정: [UX-020~023](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-020), [UX-029~031](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-029)
> 최종 검토일: 2026-11-17

## 목적

사용자가 기기 사진 중 편집 가능한 정지 이미지 한 장을 고르게 합니다. 사진은 기기 밖으로 보내지 않습니다.

## 범위

구현은 세 단계로 나눕니다.

| 단계 | Issue | 내용 |
| --- | --- | --- |
| ① | #12 | 모듈, Metro, 사진 권한 흐름(PermissionRequired) |
| ② | #13 | 사진 목록, 썸네일, 날짜 묶음, Empty, LoadFailed |
| ③ | #14 | 사진 선택, 개별 사진 실패, 스크롤 위치 복원, Foreground 갱신 |

제한 접근 안내 UI와 앨범 선택은 MVP에서 제외합니다(Wiki Gallery UX).

## 계약

### 모듈

- `platform/media`는 `PhotoPermission`과 `PhotoAccess`를 제공합니다. Android·iOS 구현은 이 모듈 안에 둡니다.
- `feature/gallery`는 `GalleryViewModel`과 `GalleryScreen`을 제공합니다. 플랫폼 타입에 직접 의존하지 않습니다.
- `composeApp`의 Metro `AppGraph`가 플랫폼 구현을 받아 `GalleryViewModel`에 주입합니다.

### 권한 상태

| `PhotoAccess` | 뜻 | 화면 |
| --- | --- | --- |
| `NotDetermined` | 아직 묻지 않음 | PermissionRequired |
| `Denied` | 거부했지만 시스템 팝업을 다시 띄울 수 있음 | PermissionRequired |
| `Blocked` | OS 설정에서만 허용할 수 있음 | PermissionRequired |
| `Granted` | 전체 허용 | 사진 목록(②) |
| `Limited` | 일부 사진만 허용 | 허용된 사진만 보이는 사진 목록(②) |

### PermissionRequired 동작

- 화면에 들어올 때 시스템 팝업을 띄우지 않습니다. 사전 안내 화면을 먼저 보여 줍니다(UX-020).
- CTA는 `Blocked`이면 OS의 앱 설정 화면을 엽니다. 그 밖의 상태에서는 시스템 권한 요청을 실행합니다(UX-022).
- 거부해도 같은 화면을 유지합니다. 별도 `PermissionDenied` 화면은 없습니다(UX-021).
- 화면이 다시 활성화되면(OS 설정에서 돌아온 경우 포함) 권한을 다시 읽습니다.
- 상단 뒤로가기는 Home으로 돌아갑니다.

## 실패와 복구

| 상황 | 결과 |
| --- | --- |
| 시스템 팝업에서 거부 | 같은 화면 유지. Android는 두 번째 거부부터 `Blocked` |
| OS 설정에서 허용하고 복귀 | 권한을 다시 읽고 사진 목록 상태로 전환 |
| 허용 후 OS 설정에서 취소 | ③에서 Foreground 복귀 시 `PermissionRequired`로 전환(UX-031) |

## 플랫폼 차이

| 항목 | Android | iOS |
| --- | --- | --- |
| 요청 권한 | API 34+: `READ_MEDIA_IMAGES`, `READ_MEDIA_VISUAL_USER_SELECTED` · API 33: `READ_MEDIA_IMAGES` · API 32 이하: `READ_EXTERNAL_STORAGE` | PhotoKit `readWrite` 접근 수준 |
| 제한 접근 | `READ_MEDIA_VISUAL_USER_SELECTED`만 허용되면 `Limited` | `PHAuthorizationStatusLimited` |
| 재요청 판단 | 요청 이력(SharedPreferences)과 `shouldShowRequestPermissionRationale`로 `NotDetermined`·`Denied`·`Blocked` 구분 | 시스템 팝업은 한 번만 표시. 거부·제한은 `Blocked` |
| OS 설정 이동 | `ACTION_APPLICATION_DETAILS_SETTINGS` | `UIApplicationOpenSettingsURLString` |
| 권한 안내 문구 | 앱 화면 문구만 사용 | `NSPhotoLibraryUsageDescription`(현재 영어 1개) |

## Acceptance Criteria

### ① 권한 흐름

- [x] 권한이 없으면 사전 안내를 보여 주고, 시스템 팝업은 CTA 이후에만 뜬다
- [x] 거부하면 같은 화면을 유지한다
- [x] 재요청이 불가능하면 CTA가 OS 설정을 연다
- [x] OS 설정에서 허용하고 돌아오면 화면이 갱신된다
- [x] `GalleryViewModelTest`: 요청 가능 시 시스템 요청, `Blocked` 시 설정 이동

### ②·③

해당 단계의 Issue에서 이 문서를 갱신합니다.
