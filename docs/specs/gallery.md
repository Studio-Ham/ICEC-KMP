# Gallery

> 상태: Accepted
> SSOT: Git Repository
> 관련 요구사항: [Wiki Gallery UX](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/gallery.md) · Figma `[승인] 화면 2` (`316:2412`, `316:2477`, `316:2506`, `316:2532`, `340:294`, `340:326`)
> 관련 결정: [UX-020~023](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-020), [UX-029~031](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-029), [UX-044](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-044), [UX-045](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-045)
> 최종 검토일: 2026-11-21

## 목적

사용자가 기기 사진 중 편집 가능한 정지 이미지 한 장을 고르게 합니다. 사진은 기기 밖으로 보내지 않습니다.

## 범위

구현은 네 단계로 나눕니다.

| 단계 | Issue | 내용 |
| --- | --- | --- |
| ① | #12 | 모듈, Metro, 사진 권한 흐름(PermissionRequired) |
| ② | #13 | 사진 목록, 썸네일, 날짜 묶음, Empty, LoadFailed |
| ③ | #14 | 사진 선택, 개별 사진 실패, 스크롤 위치 복원, Foreground 갱신 |
| ④ | #45 | 사진 목록 페이징(페이지 크기 `60`, 남은 항목 임계값 `30`, UX-044) |

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
| `Granted` | 전체 허용 | 사진 목록 |
| `Limited` | 일부 사진만 허용 | 허용된 사진만 보이는 사진 목록 |

### PermissionRequired 동작

- 화면에 들어올 때 시스템 팝업을 띄우지 않습니다. 사전 안내 화면을 먼저 보여 줍니다(UX-020).
- CTA는 `Blocked`이면 OS의 앱 설정 화면을 엽니다. 그 밖의 상태에서는 시스템 권한 요청을 실행합니다(UX-022).
- 거부해도 같은 화면을 유지합니다. 별도 `PermissionDenied` 화면은 없습니다(UX-021).
- 화면이 다시 활성화되면(OS 설정에서 돌아온 경우 포함) 권한을 다시 읽습니다.
- 상단 뒤로가기는 Home으로 돌아갑니다.

### 사진 목록 계약

- `PhotoLibrary.photos(offset, limit)`는 `PhotoPage(photos, next, total)`를 최신순으로 반환합니다. 읽을 수 없으면 예외를 던집니다.
- `offset`은 플랫폼 원본 목록 위치입니다. `next`는 다음 페이지의 `offset`이며, 마지막 페이지이면 `null`입니다.
- 플랫폼이 미지원 형식을 거르면 한 페이지의 `photos` 길이가 `limit`보다 짧을 수 있습니다.
- `GalleryViewModel`은 페이지 크기 상수(`PAGE_SIZE`)로 `60`을 씁니다(UX-044).
- `GalleryViewModel`은 첫 페이지를 받으면 바로 표시합니다.
- `GalleryScreen`은 남은 항목이 `30`개 이하이면 `GalleryViewModel.onLoadMore()`를 호출합니다.
- `GalleryViewModel.onLoadMore()`는 진행 중인 읽기가 없고 `next`가 있으면 다음 페이지를 붙입니다. 이어 붙인 사진은 `id`로 중복을 제거한 뒤 날짜별로 묶습니다.
- `GalleryScreen` 상단 사진 수는 `PhotoPage.total`을 표시합니다. `Empty` 상태에서는 상단 사진 수를 숨기고 보조 본문 없이 제목만 표시합니다([UX-045](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-045)).
- `LoadFailed` 상태는 한 줄 본문과 다시 시도 버튼을 표시합니다([UX-045](https://github.com/Studio-Ham/ICEC-LLM-WiKi/blob/main/docs/policies/ux-decisions.md#ux-045)).
- `PhotoLibrary.thumbnail()`은 타일 크기 이상의 썸네일을 반환합니다. 읽을 수 없으면 null을 반환하고 타일은 빈 배경으로 둡니다.
- 촬영일이 없으면 Android는 추가일, iOS는 생성일을 씁니다. 날짜 묶음은 기기 시간대 기준입니다.
- `PhotoLibrary.isReadable()`은 사진 원본을 읽을 수 있는지 확인합니다. iOS는 iCloud 원본 다운로드를 허용합니다.

### 사진 선택과 갱신

- 타일을 누르면 `GalleryViewModel`이 원본을 확인합니다. 읽을 수 있으면 `GalleryEffect.OpenPhoto`를 보냅니다.
- `composeApp`은 `OpenPhoto`를 받으면 `Route.FaceAnalysis(photoId)`를 back stack에 추가합니다. FaceAnalysis 구현 전까지는 임시 화면을 보여 줍니다.
- 읽을 수 없으면 `GalleryEffect.PhotoUnavailable`을 보내고 `refresh()`로 목록을 다시 읽습니다. 화면은 Snackbar(`340:326`)를 3초 동안 보여 줍니다(UX-030).
- FaceAnalysis에서 돌아오면 같은 스크롤 위치를 유지합니다. Navigation 3 entry의 saveable 상태가 `LazyGridState`를 보존합니다.
- 화면이 다시 활성화되면 `refresh()`가 권한과 목록을 다시 읽습니다(UX-031).
- `refresh()`는 진행 중인 읽기를 취소하고 `offset = 0`부터 이미 읽은 범위(`maxOf(60, loadedRange)`)를 한 번에 다시 읽습니다. 이미 목록이 있으면 `Loading`으로 바꾸지 않고 결과만 교체합니다.

## 실패와 복구

| 상황 | 결과 |
| --- | --- |
| 시스템 팝업에서 거부 | 같은 화면 유지. Android는 두 번째 거부부터 `Blocked` |
| OS 설정에서 허용하고 복귀 | 권한을 다시 읽고 사진 목록 상태로 전환 |
| 첫 페이지 또는 `refresh()` 조회 실패 | `LoadFailed`로 전환. 다시 시도 성공 시 Content 또는 Empty |
| 다음 페이지 조회 실패 | 기존 목록과 `next`를 유지. 표시한 사진이 0장이면 `appendFailed`로 다시 시도 화면 표시 |
| 선택한 사진을 읽을 수 없음 | Gallery 유지, Snackbar 표시, `refresh()`로 목록 다시 읽기(UX-030) |
| 허용 후 OS 설정에서 취소 | Foreground 복귀 시 `PermissionRequired`로 전환(UX-031) |

## 플랫폼 차이

| 항목 | Android | iOS |
| --- | --- | --- |
| 요청 권한 | API 34+: `READ_MEDIA_IMAGES`, `READ_MEDIA_VISUAL_USER_SELECTED` · API 33: `READ_MEDIA_IMAGES` · API 29~32: `READ_EXTERNAL_STORAGE` · API 28 이하: `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`(Editor 저장용) | PhotoKit `readWrite` 접근 수준 |
| 제한 접근 | `READ_MEDIA_VISUAL_USER_SELECTED`만 허용되면 `Limited` | `PHAuthorizationStatusLimited` |
| 재요청 판단 | 요청 이력(SharedPreferences)과 `shouldShowRequestPermissionRationale`로 `NotDetermined`·`Denied`·`Blocked` 구분 | 시스템 팝업은 한 번만 표시. 거부·제한은 `Blocked` |
| 목록 정렬과 페이징 | MediaStore를 `DATE_TAKEN DESC, DATE_ADDED DESC, _ID DESC`로 정렬하고 cursor 위치로 페이지 읽기 | 페이지마다 `PHAsset.fetchAssetsWithMediaType`(`creationDate` 내림차순)를 호출해 해당 범위만 읽기 |
| 미지원 미디어 제외 | MIME 타입(`image/jpeg`·`image/png`·`image/heic`·`image/heif`)으로 조회 | `playbackStyle`로 GIF를 먼저 거르고, 해당 페이지 자산만 `assetResourcesForAsset` 첫 리소스 UTI로 GIF·RAW 제외 |
| 썸네일 | `loadThumbnail`(API 29+), 이하 `Thumbnails.getThumbnail` | `PHImageManager` 요청 후 JPEG로 변환해 Skia 이미지 생성 |
| OS 설정 이동 | `ACTION_APPLICATION_DETAILS_SETTINGS` | `UIApplicationOpenSettingsURLString` |
| 권한 안내 문구 | 앱 화면 문구만 사용 | `NSPhotoLibraryUsageDescription`(현재 영어 1개) |

### 알려진 차이

- iOS의 `PhotoPage.total`에는 숨긴 GIF·RAW 자산이 포함될 수 있습니다. `IosPhotoLibrary`는 `PHAsset.fetchAssetsWithMediaType`의 전체 결과 수를 `total`로 반환합니다.
- `IosPhotoLibrary`는 페이지 범위 밖 자산에는 `assetResourcesForAsset`를 호출하지 않습니다.
- 이전 iOS 구현은 전체 사진을 한 번에 읽고 사진마다 `assetResourcesForAsset`를 호출했습니다. 그래서 첫 사진 표시까지 몇 초가 걸렸습니다.

## Acceptance Criteria

### ① 권한 흐름

- [x] 권한이 없으면 사전 안내를 보여 주고, 시스템 팝업은 CTA 이후에만 뜬다
- [x] 거부하면 같은 화면을 유지한다
- [x] 재요청이 불가능하면 CTA가 OS 설정을 연다
- [x] OS 설정에서 허용하고 돌아오면 화면이 갱신된다
- [x] `GalleryViewModelTest`: 요청 가능 시 시스템 요청, `Blocked` 시 설정 이동

### ② 사진 목록

- [x] JPEG·PNG·HEIC/HEIF 정지 이미지만 최신순으로 보여 준다. GIF·RAW 등 미지원 미디어는 숨긴다
- [x] 촬영일 기준으로 묶는다. 오늘·어제·올해·다른 해 라벨을 쓴다
- [x] 3열 정사각 타일, 2dp 간격, 최상단 이동 버튼(Figma `316:2412`)
- [x] 사진이 0장이면 Empty(`316:2506`), 조회에 실패하면 LoadFailed(`340:294`)와 다시 시도
- [x] `GalleryViewModelTest`: 날짜 묶음, 실패 후 다시 시도, 천 단위 구분

### ③ 사진 선택과 갱신

- [x] 사진을 누르면 FaceAnalysis 경로로 이동한다(임시 화면)
- [x] 원본을 읽을 수 없으면 Snackbar를 보여 주고 삭제된 사진을 목록에서 뺀다
- [x] FaceAnalysis에서 돌아오면 스크롤 위치를 유지한다
- [x] Foreground 복귀 시 권한과 목록을 다시 읽는다
- [x] `GalleryViewModelTest`: 읽을 수 있는 사진 열기, 읽을 수 없는 사진 안내와 다시 읽기

### ④ 사진 목록 페이징 (#45)

- [x] 첫 페이지(60장)를 받으면 바로 표시한다
- [x] 남은 항목이 30개 이하이면 다음 페이지를 이어 붙이고, 끝에서 멈춘다
- [x] 진행 중인 페이지 읽기가 있으면 중복 호출을 무시한다
- [x] `refresh()`는 이미 읽은 범위를 한 번에 다시 읽는다
- [x] 다음 페이지 실패 시 기존 목록을 유지하고 재시도할 수 있다
- [x] `GalleryViewModelTest`: 첫 페이지 즉시 표시, 다음 페이지 추가·종료, 중복 호출 방지, `refresh()` 범위 재조회, 실패 시 목록 유지와 재시도
