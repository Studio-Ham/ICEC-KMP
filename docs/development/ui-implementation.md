# UI 구현 규칙

> 상태: Accepted
> SSOT: Git Repository
> 최종 검토일: 2026-11-21

이 문서는 Compose Multiplatform UI와 Android 릴리스 축소 구현 규칙을 정의합니다. 제품 요구사항과 UX 정책은 [ICEC LLM Wiki](../references/llm-wiki.md)를 따릅니다. 시각 디자인 원본은 [제품·UX·디자인 참조](../references/product-design.md)의 Figma 문서를 따릅니다. Composable 기본 규칙은 루트 [`AGENTS.md`](../../AGENTS.md) 5절을 따릅니다.

## 1. 터치 영역

- 모든 탭 대상은 가로와 세로 모두 `48.dp` 이상의 터치 영역을 가져야 합니다.
- 보이는 크기보다 터치 영역이 작으면 보이는 크기를 유지한 채 누르는 영역만 넓힙니다.
- 아이콘 버튼은 바깥 컨테이너 또는 `padding(...)` 앞의 `Modifier`에 `size(48.dp)`와 `clickable(role = Role.Button, ...)`을 적용합니다.
- 그다음 안쪽 `padding(...)`이나 자식 요소로 아이콘 여백을 맞춥니다. (`EditorScreen.kt`, `FaceAnalysisScreen.kt`, `GalleryScreen.kt`)
- 작은 텍스트 칩과 보조 액션은 바깥 `Box`에 `sizeIn(minWidth = 48.dp, minHeight = 48.dp)`와 `clickable(...)`을 둡니다.
- 시각 요소는 바깥 `Box` 안쪽에서 크기, 배경과 테두리를 그립니다. (`EditorScreen.kt` `SelectAllChip`, `SaveErrorSnackbar` 액션, `GalleryScreen.kt` `TextAction`)
- 칩 주변에 `48.dp` 사각 리플을 보이지 않으려면 `clickable`에 `interactionSource = null, indication = null`을 넘깁니다.
- 전체 폭 버튼과 다이얼로그 버튼은 `heightIn(min = 48.dp)` 이상으로 최소 높이를 보장합니다. (`ResultScreen.kt`, `EditorScreen.kt`)

## 2. 큰 글자

- 텍스트를 담은 컨테이너는 고정 `height(...)` 대신 `heightIn(min = ...)`을 사용합니다.
- 시스템 글꼴 크기를 키워도 텍스트가 잘리지 않게 컨테이너 높이가 늘어나야 합니다.
- 상단 바(`56.dp`·`57.dp`), 주요 버튼(`56.dp`), 다이얼로그 버튼(`48.dp`)과 Snackbar(`48.dp`)는 모두 `heightIn(min = ...)`을 사용합니다.
- 가로로도 글자가 커지는 액션은 `sizeIn(minWidth = 48.dp, minHeight = 48.dp)`을 사용합니다. (`GalleryScreen.kt` `TextAction`, `EditorScreen.kt` `SaveErrorSnackbar`)

## 3. Adaptive 레이아웃

- 2단 배치 판정은 `core/designsystem`의 `Adaptive.kt`에 있는 `isTwoPane()`을 사용합니다.
- `isTwoPane(width, height)`는 창 폭 `600.dp` 이상 또는 창 높이 `480.dp` 미만일 때 `true`를 반환합니다. (`AdaptiveTest.kt` 참조)
- 폭 `600.dp` 이상은 태블릿과 펼친 폴더블을 다룹니다.
- 높이 `480.dp` 미만은 가로 방향 휴대폰을 다룹니다.
- `isTwoPane()`은 `LocalWindowInfo.current.containerSize`를 `LocalDensity.current`로 `Dp` 변환해 판정합니다.
- 넓은 화면에서 버튼과 안내 문구가 과하게 늘어나지 않게 `widthIn(max = ContentMaxWidth)`(`480.dp`)를 적용합니다. (`HomeScreen.kt`, `GalleryScreen.kt`, `FaceAnalysisScreen.kt`)
- 사진 그리드는 `GridCells.Adaptive(120.dp)`를 사용합니다. (`GalleryScreen.kt`)
- 2단 편집 패널의 얼굴 목록은 `GridCells.FixedSize(84.dp)`를 사용합니다. (`EditorScreen.kt`)
- 1단 트리와 2단 트리를 전환할 때는 UI 상태를 `if (twoPane)` 분기 바깥으로 끌어올립니다.
- `rememberZoomState()`, `rememberLazyListState()`와 `rememberLazyGridState()`를 분기 바깥에 두어 회전 시 확대·이동과 스크롤 위치를 유지합니다. (`EditorContent`, `ResultContent`)

### 도구 팔레트와 힌지

- 힌지 위치는 `core/designsystem`의 `separatingHinges()`로 읽습니다.
- `separatingHinges()`는 분리형 힌지의 창 좌표(px)를 반환합니다. Android는 `WindowInfoTracker`를 사용합니다. iOS는 빈 목록을 반환합니다.
- 힌지가 본문을 가르는지는 `hingeSplit(area, hinges)`로 판정합니다. `area`는 본문의 `boundsInWindow()`입니다. (`AdaptiveTest.kt` 참조)
- 분리형 힌지가 본문을 가르면 힌지 앞 영역에 사진을 둡니다. 힌지 뒤 영역에 팔레트를 둡니다. 이 규칙은 `isTwoPane()`보다 우선합니다.
- 힌지가 없으면 `isTwoPane()`이 `true`이고 본문 폭이 `580.dp` 이상일 때만 팔레트를 사진 오른쪽에 둡니다.
- 폭 조건은 높이 `480.dp` 미만의 좁은 분할 창을 1단으로 유지합니다.
- 팔레트 벡터(`palette_background.xml`)와 원형 버튼은 한 Composable(`ToolPalette`)로 배치합니다. 힌지가 팔레트와 버튼 사이를 지나가지 않습니다.
- 팔레트 배율은 `0.75`~`1.25`로 제한합니다. 배율 `0.75`에서 버튼 지름은 `48.dp`입니다. (`PhotoPreviewScreen.kt`)
- 자르기 영역의 좌우 여백은 `WindowInsets.systemGestures`와 `16.dp` 중 큰 값을 사용합니다. 자르기 모서리가 Android 가장자리 뒤로가기 제스처 영역에 들어가지 않습니다. (`BasicEditScreen.kt` `CropPhoto`)

## 4. 확대·이동

- 확대·이동은 `core/designsystem`의 `Zoom.kt`에 있는 `ZoomState`, `fitRect()`, `rememberZoomState()`와 `Modifier.zoomable()`을 사용합니다.
- 최대 배율은 `4f`(`ZoomState.MAX_SCALE`)입니다.
- `1x` 배율에서는 한 손가락 이동을 무시합니다.
- 확대 상태에서는 `clampOffset()`으로 사진 밖 빈 영역이 보이지 않게 이동 범위를 제한합니다.
- 사진, 얼굴 가이드, 체크 배지, 모자이크 미리보기와 자르기 상자는 `Canvas` 안에서 `zoom.photoRect(imageSize, size)` 한 번의 변환으로 그립니다. (`EditorScreen.kt` `Photo`, `ResultScreen.kt` `ZoomableImage`, `BasicEditScreen.kt` `CropPhoto`)
- `Canvas` 전체를 `graphicsLayer`로 확대하지 않습니다.
- 선 두께와 배지 반지름은 화면 `dp.toPx()`를 유지합니다.
- 사진 위 탭·드래그 판정도 같은 `zoom.photoRect(imageSize, viewport)`를 호출해 좌표를 계산합니다. (`EditorScreen.kt` `Photo`, `BasicEditScreen.kt` `CropPhoto`)
- 한 손가락 드래그를 콘텐츠가 쓰는 화면은 `Modifier.zoomable(state, singleFingerPan = false)`를 사용합니다. 이때 두 손가락 이상만 확대·이동합니다.
- `singleFingerPan = false`를 쓰면 콘텐츠의 `pointerInput` 뒤에 `zoomable`을 둡니다. 핀치가 시작되면 콘텐츠의 드래그가 취소됩니다. (`BasicEditScreen.kt` 자르기 상자)
- 얼굴 박스가 겹치면 가장 작은 박스를 선택합니다.
- `rememberZoomState()`는 `rememberSaveable`로 상태를 저장해 화면 이동과 구성 변경 뒤에도 배율과 위치를 유지합니다.

## 5. 접근성 의미

- 화면 제목, 빈 상태·오류 제목과 다이얼로그 제목에는 `Modifier.semantics { heading() }`을 붙입니다.
- 진행 상태 안내와 오류 Snackbar에는 `Modifier.semantics { liveRegion = LiveRegionMode.Polite }`를 붙입니다. (`FaceAnalysisScreen.kt`, `EditorScreen.kt`, `GalleryScreen.kt`)
- 여러 자식 텍스트를 한 번에 알려야 하는 진행 오버레이는 `semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }`를 사용합니다. (`EditorScreen.kt` `Saving`)
- 아이콘과 여러 텍스트가 하나의 값을 이루면 상위 컨테이너에 `clearAndSetSemantics { contentDescription = ... }`를 적용해 한 문장으로 읽히게 합니다.
- 예로 `EditorScreen.kt` 상단 카운터는 검출 수와 선택 수를 `Res.string.editor_counter` 한 문장으로 합칩니다.
- 선택 가능한 얼굴 항목은 `semantics`에 `contentDescription`과 `selected` 상태를 설정하고 `Role.Checkbox`로 클릭을 연결합니다. (`EditorScreen.kt` `FaceItem`)
- 사진 그리드 타일은 썸네일 `Image`의 `contentDescription`을 `null`로 두고 타일 컨테이너에 날짜 기반 `contentDescription`과 `Role.Button`을 설정합니다. (`GalleryScreen.kt` `PhotoTile`)
- 인접 텍스트가 의미를 이미 전달하는 장식 아이콘은 `Image(..., contentDescription = null)`로 둡니다.
- 단독 아이콘 버튼(뒤로 가기, 맨 위로 버튼)과 홈 브랜드 로고에만 `stringResource(...)`로 `contentDescription`을 둡니다.

## 6. 햅틱

- 햅틱은 `LocalHapticFeedback.current`를 사용합니다.
- 햅틱은 얼굴 선택 토글과 저장 완료에만 사용합니다. (`EditorScreen.kt`)
- 얼굴을 선택할 때는 `HapticFeedbackType.ToggleOn`, 선택을 해제할 때는 `HapticFeedbackType.ToggleOff`를 호출합니다.
- 저장 완료(`EditorEffect.Saved`) 시에는 화면 이동 직전에 `HapticFeedbackType.Confirm`을 호출합니다.
- 일반 버튼 이동, 스크롤, 뒤로 가기와 오류 표시에는 햅틱을 사용하지 않습니다.

## 7. 문자열·아이콘

- UI 문자열은 각 모듈의 `src/commonMain/composeResources/` 아래 `values/strings.xml`(영어)과 `values-ko/strings.xml`(한국어)에 둡니다.
- 각 모듈 `build.gradle.kts`에서 `compose.resources { packageOfResClass = "..." }`를 지정합니다.
- 공통 UI에서는 `stringResource()`, `pluralStringResource()`와 `stringArrayResource()`로 문자열을 읽습니다.
- Composable 코드에 사용자에게 보이는 문자열을 직접 쓰지 않습니다.
- Android 런처 이름(`androidApp/src/main/res/values/strings.xml`의 `app_name`)처럼 플랫폼 매니페스트가 직접 읽는 문자열만 플랫폼 리소스에 둡니다.
- Figma에서 내보낸 SVG 아이콘과 벡터 그래픽은 Android VectorDrawable XML로 변환해 `src/commonMain/composeResources/drawable/`에 둡니다.
- 여러 모듈이 함께 쓰는 아이콘(`ic_gallery`, `ic_retry`)은 `core/designsystem/src/commonMain/composeResources/drawable/`에 둡니다.
- `core/designsystem/build.gradle.kts`에서 `publicResClass = true`를 설정해 다른 모듈이 `com.studioham.icec.designsystem.resources.Res`로 접근하게 합니다.
- 버튼 안의 아이콘은 `16.dp` 크기를 사용합니다.
- 변환한 벡터는 공통 코드에서 `painterResource(Res.drawable.<이름>)`로 읽습니다.
- 시각 에셋 원본 관리는 [제품·UX·디자인 참조](../references/product-design.md)를 따릅니다.

## 8. 릴리스 축소 (R8)

- `androidApp/build.gradle.kts`의 `release` 빌드는 `isMinifyEnabled = true`와 `isShrinkResources = true`를 켭니다.
- AGP 8 기본값인 R8 full mode는 코드에서 직접 참조하지 않는 생성자와 클래스를 제거합니다.
- ML Kit은 Firebase `ComponentDiscovery`가 매니페스트 메타데이터에서 Registrar 클래스 이름을 읽어 리플렉션으로 기본 생성자를 호출합니다. (`FaceRegistrar`, `CommonComponentRegistrar`, `VisionCommonRegistrar`)
- 라이브러리 consumer 규칙이 이 생성자를 보존하지 않으면 릴리스 빌드 실행 시 `NullPointerException`이 발생합니다.
- 따라서 `androidApp/proguard-rules.pro`에 다음 keep 규칙을 유지합니다.

```proguard
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
```

- Metro는 컴파일 타임에 의존성 그래프를 생성하므로 리플렉션용 keep 규칙이 필요하지 않습니다.
- Compose Multiplatform 리소스는 `assets/composeResources/`에 들어가므로 `isShrinkResources`가 제거하지 않습니다.
- `androidApp/proguard-rules.pro`는 최소로 유지합니다.
- 릴리스 빌드 크래시나 누락 클래스 보고를 확인한 경우에만 keep 규칙을 추가합니다.
- 새 라이브러리를 추가하거나 DI·리플렉션·네이티브 SDK 구성을 바꾸면 Debug 빌드 외에 Release APK를 빌드해 실제 실행을 확인합니다.
