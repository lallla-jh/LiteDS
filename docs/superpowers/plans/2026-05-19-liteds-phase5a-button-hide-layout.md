# Phase 5A — 보조 버튼 숨김 + 그립존 최적화 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** "보조 버튼 숨기기" SharedPreferences 토글을 추가하고, `DefaultLayoutProvider`의 Portrait/Landscape 기본 레이아웃을 개선하여 D-pad/ABXY를 32dp 위로 올리고 L/R/SEL/STA 크기를 확대. 토글이 켜지면 L/R/SEL/STA를 제거하고 D-pad/ABXY를 160dp로 자동 확대.

**Architecture:** `SettingsRepository` 인터페이스에 `getHideAuxiliaryButtons()` 추가 → `SharedPreferencesSettingsRepository` 구현 → `DefaultLayoutProvider` 생성자에 `SettingsRepository` 주입 → `buildDefaultPortraitLayout()`/`buildDefaultLandscapeLayout()` 내에서 `hideAuxiliary` 분기.

**Tech Stack:** Kotlin, AndroidX Preference (SwitchPreference), Hilt DI

---

## File Map

| 역할 | 파일 |
|------|------|
| Create | (없음) |
| Modify | `app/src/main/res/values/strings.xml` |
| Modify | `app/src/main/res/xml/pref_general.xml` |
| Modify | `app/src/main/java/me/magnum/melonds/domain/repositories/SettingsRepository.kt` |
| Modify | `app/src/main/java/me/magnum/melonds/impl/SharedPreferencesSettingsRepository.kt` |
| Modify | `app/src/main/java/me/magnum/melonds/di/MelonModule.kt` |
| Modify | `app/src/main/java/me/magnum/melonds/impl/layout/DefaultLayoutProvider.kt` |

---

## Task 1 — 설정 인터페이스·구현·UI 추가

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/xml/pref_general.xml`
- Modify: `app/src/main/java/me/magnum/melonds/domain/repositories/SettingsRepository.kt`
- Modify: `app/src/main/java/me/magnum/melonds/impl/SharedPreferencesSettingsRepository.kt`

- [ ] **Step 1: `SettingsRepository` 인터페이스에 메서드 추가**

`app/src/main/java/me/magnum/melonds/domain/repositories/SettingsRepository.kt` 의 `isSustainedPerformanceModeEnabled()` 바로 아래에 추가:

```kotlin
fun getHideAuxiliaryButtons(): Boolean
```

결과 (인터페이스 일부):
```kotlin
fun getFastForwardSpeedMultiplier(): Float
fun isRewindEnabled(): Boolean
fun isSustainedPerformanceModeEnabled(): Boolean
fun getHideAuxiliaryButtons(): Boolean
```

- [ ] **Step 2: `SharedPreferencesSettingsRepository`에 구현 추가**

`app/src/main/java/me/magnum/melonds/impl/SharedPreferencesSettingsRepository.kt` 에서 `isSustainedPerformanceModeEnabled()` 구현 바로 아래에 추가:

```kotlin
override fun getHideAuxiliaryButtons(): Boolean {
    return preferences.getBoolean("hide_auxiliary_buttons", false)
}
```

- [ ] **Step 3: `strings.xml`에 문자열 추가**

`app/src/main/res/values/strings.xml` 에 다른 설정 문자열들 옆에 추가:

```xml
<string name="hide_auxiliary_buttons">보조 버튼 숨기기</string>
<string name="hide_auxiliary_buttons_summary">게임 중 L, R, Select, Start 버튼을 숨깁니다</string>
```

- [ ] **Step 4: `pref_general.xml`에 SwitchPreference 추가**

`app/src/main/res/xml/pref_general.xml` 에서 `enable_sustained_performance` SwitchPreference 바로 아래에 추가:

```xml
<SwitchPreference
        android:key="hide_auxiliary_buttons"
        android:title="@string/hide_auxiliary_buttons"
        android:summary="@string/hide_auxiliary_buttons_summary"
        app:iconSpaceReserved="false"
        android:defaultValue="false" />
```

- [ ] **Step 5: 빌드 확인**

```bash
cd app && ../gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 커밋**

```bash
git add app/src/main/res/values/strings.xml \
        app/src/main/res/xml/pref_general.xml \
        app/src/main/java/me/magnum/melonds/domain/repositories/SettingsRepository.kt \
        app/src/main/java/me/magnum/melonds/impl/SharedPreferencesSettingsRepository.kt
git commit -m "feat: add hide_auxiliary_buttons setting toggle"
```

---

## Task 2 — `DefaultLayoutProvider` DI 변경 및 Portrait 레이아웃 개선

**Files:**
- Modify: `app/src/main/java/me/magnum/melonds/di/MelonModule.kt`
- Modify: `app/src/main/java/me/magnum/melonds/impl/layout/DefaultLayoutProvider.kt`

- [ ] **Step 1: `MelonModule.kt` — `SettingsRepository` 파라미터 추가**

현재 코드 (lines 177–185 부근):
```kotlin
@Provides
@Singleton
fun provideDefaultLayoutBuilder(@ApplicationContext context: Context, screenUnitsConverter: ScreenUnitsConverter): DefaultLayoutProvider {
    return DefaultLayoutProvider(context, screenUnitsConverter)
}
```

변경 후:
```kotlin
@Provides
@Singleton
fun provideDefaultLayoutBuilder(
    @ApplicationContext context: Context,
    screenUnitsConverter: ScreenUnitsConverter,
    settingsRepository: SettingsRepository,
): DefaultLayoutProvider {
    return DefaultLayoutProvider(context, screenUnitsConverter, settingsRepository)
}
```

- [ ] **Step 2: `DefaultLayoutProvider` 클래스 선언 변경**

현재:
```kotlin
class DefaultLayoutProvider(
    private val context: Context,
    private val screenUnitsConverter: ScreenUnitsConverter,
) {
```

변경 후:
```kotlin
class DefaultLayoutProvider(
    private val context: Context,
    private val screenUnitsConverter: ScreenUnitsConverter,
    private val settingsRepository: SettingsRepository,
) {
```

import 추가 (파일 상단):
```kotlin
import me.magnum.melonds.domain.repositories.SettingsRepository
```

- [ ] **Step 3: `buildDefaultPortraitLayout()` 시그니처에 `hideAuxiliary` 추가 및 내부 로직 변경**

현재 시그니처:
```kotlin
private fun buildDefaultPortraitLayout(width: Int, height: Int, insets: Insets, singleScreenComponent: LayoutComponent? = null): ScreenLayout {
```

변경 후:
```kotlin
private fun buildDefaultPortraitLayout(
    width: Int,
    height: Int,
    insets: Insets,
    hideAuxiliary: Boolean = false,
    singleScreenComponent: LayoutComponent? = null,
): ScreenLayout {
```

- [ ] **Step 4: Portrait 레이아웃 — 버튼 크기/위치 변수 변경**

현재 (lines 110–113 부근):
```kotlin
val largeButtonsSize = screenUnitsConverter.dpToPixels(140f).toInt()
val lrButtonsSize = screenUnitsConverter.dpToPixels(50f).toInt()
val smallButtonsSize = screenUnitsConverter.dpToPixels(40f).toInt()
val spacing4dp = screenUnitsConverter.dpToPixels(4f).toInt()
```

변경 후:
```kotlin
val largeButtonsSize = screenUnitsConverter.dpToPixels(if (hideAuxiliary) 160f else 140f).toInt()
val lrButtonsSize = screenUnitsConverter.dpToPixels(60f).toInt()
val selStaButtonsSize = screenUnitsConverter.dpToPixels(48f).toInt()
val smallButtonsSize = screenUnitsConverter.dpToPixels(40f).toInt()   // HINGE/TOGGLE/MIC/FF 유지
val spacing4dp = screenUnitsConverter.dpToPixels(4f).toInt()
val verticalOffset = screenUnitsConverter.dpToPixels(if (hideAuxiliary) 48f else 32f).toInt()
```

- [ ] **Step 5: Portrait 레이아웃 — D-pad/ABXY Y축 이동 및 L/R/SEL/STA 조건부 포함**

현재 반환 블록 (lines 147–164 부근):
```kotlin
val dpadView = Rect(safeLeft, height - safeBottom - largeButtonsSize, largeButtonsSize, largeButtonsSize)
val buttonsView = Rect(width - safeRight - largeButtonsSize, height - safeBottom - largeButtonsSize, largeButtonsSize, largeButtonsSize)

return ScreenLayout(
    listOf(
        *screenComponents,
        PositionedLayoutComponent(dpadView, LayoutComponent.DPAD),
        PositionedLayoutComponent(buttonsView, LayoutComponent.BUTTONS),
        PositionedLayoutComponent(Rect(safeLeft, utilityButtonsTop, lrButtonsSize, lrButtonsSize), LayoutComponent.BUTTON_L),
        PositionedLayoutComponent(Rect(width - safeRight - lrButtonsSize, utilityButtonsTop, lrButtonsSize, lrButtonsSize), LayoutComponent.BUTTON_R),
        PositionedLayoutComponent(Rect(width / 2 - smallButtonsSize - spacing4dp / 2, height - safeBottom - smallButtonsSize, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_SELECT),
        PositionedLayoutComponent(Rect(width / 2 + spacing4dp / 2, height - safeBottom - smallButtonsSize, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_START),
        PositionedLayoutComponent(Rect(width / 2 - (smallButtonsSize * 2.0 + spacing4dp * 1.5).toInt(), utilityButtonsTop, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_HINGE),
        PositionedLayoutComponent(Rect(width / 2 - smallButtonsSize - (spacing4dp / 2.0).toInt(), utilityButtonsTop, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_TOGGLE_SOFT_INPUT),
        PositionedLayoutComponent(Rect(width / 2 + (spacing4dp / 2.0).toInt(), utilityButtonsTop, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_MICROPHONE_TOGGLE),
        PositionedLayoutComponent(Rect(width / 2 + smallButtonsSize + (spacing4dp * 1.5).toInt(), utilityButtonsTop, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_FAST_FORWARD_TOGGLE),
    )
)
```

변경 후:
```kotlin
val dpadY = height - safeBottom - largeButtonsSize - verticalOffset
val dpadView = Rect(safeLeft, dpadY, largeButtonsSize, largeButtonsSize)
val buttonsView = Rect(width - safeRight - largeButtonsSize, dpadY, largeButtonsSize, largeButtonsSize)

val components = mutableListOf(
    *screenComponents,
    PositionedLayoutComponent(dpadView, LayoutComponent.DPAD),
    PositionedLayoutComponent(buttonsView, LayoutComponent.BUTTONS),
    PositionedLayoutComponent(Rect(width / 2 - (smallButtonsSize * 2.0 + spacing4dp * 1.5).toInt(), utilityButtonsTop, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_HINGE),
    PositionedLayoutComponent(Rect(width / 2 - smallButtonsSize - (spacing4dp / 2.0).toInt(), utilityButtonsTop, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_TOGGLE_SOFT_INPUT),
    PositionedLayoutComponent(Rect(width / 2 + (spacing4dp / 2.0).toInt(), utilityButtonsTop, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_MICROPHONE_TOGGLE),
    PositionedLayoutComponent(Rect(width / 2 + smallButtonsSize + (spacing4dp * 1.5).toInt(), utilityButtonsTop, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_FAST_FORWARD_TOGGLE),
)

if (!hideAuxiliary) {
    components += listOf(
        PositionedLayoutComponent(Rect(safeLeft, dpadY, lrButtonsSize, lrButtonsSize), LayoutComponent.BUTTON_L),
        PositionedLayoutComponent(Rect(width - safeRight - lrButtonsSize, dpadY, lrButtonsSize, lrButtonsSize), LayoutComponent.BUTTON_R),
        PositionedLayoutComponent(Rect(width / 2 - selStaButtonsSize - spacing4dp / 2, height - safeBottom - selStaButtonsSize, selStaButtonsSize, selStaButtonsSize), LayoutComponent.BUTTON_SELECT),
        PositionedLayoutComponent(Rect(width / 2 + spacing4dp / 2, height - safeBottom - selStaButtonsSize, selStaButtonsSize, selStaButtonsSize), LayoutComponent.BUTTON_START),
    )
}

return ScreenLayout(components)
```

- [ ] **Step 6: `buildDefaultLayout()` 호출 지점에서 `hideAuxiliary` 전달**

`buildDefaultLayout()` 내에서 portrait 분기를 찾아 `hideAuxiliary` 값을 전달. 현재 호출:
```kotlin
buildDefaultPortraitLayout(width, height, mainDisplayInsets)
buildDefaultPortraitLayout(width, height, mainDisplayInsets, singleScreenComponent)
```

변경 후 — 두 호출 모두 수정:
```kotlin
val hideAuxiliary = settingsRepository.getHideAuxiliaryButtons()
// ...
buildDefaultPortraitLayout(width, height, mainDisplayInsets, hideAuxiliary = hideAuxiliary)
buildDefaultPortraitLayout(width, height, mainDisplayInsets, hideAuxiliary = hideAuxiliary, singleScreenComponent = singleScreenComponent)
```

> **Note:** `buildDefaultLayout()` 상단에서 `hideAuxiliary`를 한 번만 읽고 landscape 쪽에도 동일하게 전달할 것 (Task 3에서 처리).

- [ ] **Step 7: 빌드 확인**

```bash
cd app && ../gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: 커밋**

```bash
git add app/src/main/java/me/magnum/melonds/di/MelonModule.kt \
        app/src/main/java/me/magnum/melonds/impl/layout/DefaultLayoutProvider.kt
git commit -m "feat: optimize portrait grip zone, add hideAuxiliary branch"
```

---

## Task 3 — Landscape 레이아웃 동일 적용

**Files:**
- Modify: `app/src/main/java/me/magnum/melonds/impl/layout/DefaultLayoutProvider.kt`

- [ ] **Step 1: `buildDefaultLandscapeLayout()` 시그니처에 `hideAuxiliary` 추가**

현재:
```kotlin
private fun buildDefaultLandscapeLayout(width: Int, height: Int, insets: Insets, singleScreenComponent: LayoutComponent? = null): ScreenLayout {
```

변경 후:
```kotlin
private fun buildDefaultLandscapeLayout(
    width: Int,
    height: Int,
    insets: Insets,
    hideAuxiliary: Boolean = false,
    singleScreenComponent: LayoutComponent? = null,
): ScreenLayout {
```

- [ ] **Step 2: Landscape — 버튼 크기/위치 변수 변경**

현재 (lines 177–180 부근):
```kotlin
val largeButtonsSize = screenUnitsConverter.dpToPixels(140f).toInt()
val lrButtonsSize = screenUnitsConverter.dpToPixels(50f).toInt()
val smallButtonsSize = screenUnitsConverter.dpToPixels(40f).toInt()
val spacing4dp = screenUnitsConverter.dpToPixels(4f).toInt()
```

변경 후:
```kotlin
val largeButtonsSize = screenUnitsConverter.dpToPixels(if (hideAuxiliary) 160f else 140f).toInt()
val lrButtonsSize = screenUnitsConverter.dpToPixels(60f).toInt()
val selStaButtonsSize = screenUnitsConverter.dpToPixels(48f).toInt()
val smallButtonsSize = screenUnitsConverter.dpToPixels(40f).toInt()
val spacing4dp = screenUnitsConverter.dpToPixels(4f).toInt()
val verticalOffset = screenUnitsConverter.dpToPixels(if (hideAuxiliary) 48f else 32f).toInt()
```

- [ ] **Step 3: Landscape — D-pad/ABXY Y축 이동 및 L/R/SEL/STA 조건부 포함**

현재 반환 블록 (lines 205–222 부근):
```kotlin
val dpadView = Rect(safeLeft, height - safeBottom - largeButtonsSize, largeButtonsSize, largeButtonsSize)
val buttonsView = Rect(width - safeRight - largeButtonsSize, height - safeBottom - largeButtonsSize, largeButtonsSize, largeButtonsSize)

return ScreenLayout(
    listOf(
        *screenComponents,
        PositionedLayoutComponent(dpadView, LayoutComponent.DPAD),
        PositionedLayoutComponent(buttonsView, LayoutComponent.BUTTONS),
        PositionedLayoutComponent(Rect(safeLeft, safeTop, lrButtonsSize, lrButtonsSize), LayoutComponent.BUTTON_L),
        PositionedLayoutComponent(Rect(width - safeRight - lrButtonsSize, safeTop, lrButtonsSize, lrButtonsSize), LayoutComponent.BUTTON_R),
        PositionedLayoutComponent(Rect((width - spacing4dp) / 2 - smallButtonsSize, height - safeBottom - smallButtonsSize, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_SELECT),
        PositionedLayoutComponent(Rect((width + spacing4dp) / 2, height - safeBottom - smallButtonsSize, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_START),
        PositionedLayoutComponent(Rect(width / 2 - (smallButtonsSize * 2.0 + spacing4dp * 1.5).toInt(), safeTop, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_HINGE),
        PositionedLayoutComponent(Rect(width / 2 - smallButtonsSize - (spacing4dp / 2.0).toInt(), safeTop, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_TOGGLE_SOFT_INPUT),
        PositionedLayoutComponent(Rect(width / 2 + (spacing4dp / 2.0).toInt(), safeTop, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_MICROPHONE_TOGGLE),
        PositionedLayoutComponent(Rect(width / 2 + smallButtonsSize + (spacing4dp * 1.5).toInt(), safeTop, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_FAST_FORWARD_TOGGLE),
    )
)
```

변경 후:
```kotlin
val dpadY = height - safeBottom - largeButtonsSize - verticalOffset
val dpadView = Rect(safeLeft, dpadY, largeButtonsSize, largeButtonsSize)
val buttonsView = Rect(width - safeRight - largeButtonsSize, dpadY, largeButtonsSize, largeButtonsSize)

val components = mutableListOf(
    *screenComponents,
    PositionedLayoutComponent(dpadView, LayoutComponent.DPAD),
    PositionedLayoutComponent(buttonsView, LayoutComponent.BUTTONS),
    PositionedLayoutComponent(Rect(width / 2 - (smallButtonsSize * 2.0 + spacing4dp * 1.5).toInt(), safeTop, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_HINGE),
    PositionedLayoutComponent(Rect(width / 2 - smallButtonsSize - (spacing4dp / 2.0).toInt(), safeTop, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_TOGGLE_SOFT_INPUT),
    PositionedLayoutComponent(Rect(width / 2 + (spacing4dp / 2.0).toInt(), safeTop, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_MICROPHONE_TOGGLE),
    PositionedLayoutComponent(Rect(width / 2 + smallButtonsSize + (spacing4dp * 1.5).toInt(), safeTop, smallButtonsSize, smallButtonsSize), LayoutComponent.BUTTON_FAST_FORWARD_TOGGLE),
)

if (!hideAuxiliary) {
    components += listOf(
        PositionedLayoutComponent(Rect(safeLeft, dpadY, lrButtonsSize, lrButtonsSize), LayoutComponent.BUTTON_L),
        PositionedLayoutComponent(Rect(width - safeRight - lrButtonsSize, dpadY, lrButtonsSize, lrButtonsSize), LayoutComponent.BUTTON_R),
        PositionedLayoutComponent(Rect((width - spacing4dp) / 2 - selStaButtonsSize, height - safeBottom - selStaButtonsSize, selStaButtonsSize, selStaButtonsSize), LayoutComponent.BUTTON_SELECT),
        PositionedLayoutComponent(Rect((width + spacing4dp) / 2, height - safeBottom - selStaButtonsSize, selStaButtonsSize, selStaButtonsSize), LayoutComponent.BUTTON_START),
    )
}

return ScreenLayout(components)
```

- [ ] **Step 4: `buildDefaultLayout()` — landscape 호출에 `hideAuxiliary` 전달**

`buildDefaultLayout()` 내 landscape 분기 호출 찾아 수정:
```kotlin
// 기존:
buildDefaultLandscapeLayout(width, height, mainDisplayInsets)
buildDefaultLandscapeLayout(width, height, mainDisplayInsets, singleScreenComponent)

// 변경 후 (hideAuxiliary는 Task 2 Step 6에서 이미 읽은 변수 재사용):
buildDefaultLandscapeLayout(width, height, mainDisplayInsets, hideAuxiliary = hideAuxiliary)
buildDefaultLandscapeLayout(width, height, mainDisplayInsets, hideAuxiliary = hideAuxiliary, singleScreenComponent = singleScreenComponent)
```

- [ ] **Step 5: 빌드 확인**

```bash
cd app && ../gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 커밋**

```bash
git add app/src/main/java/me/magnum/melonds/impl/layout/DefaultLayoutProvider.kt
git commit -m "feat: optimize landscape grip zone, apply hideAuxiliary to landscape layout"
```

---

## Self-Review

**Spec coverage:**
- ✅ 기능 1: `getHideAuxiliaryButtons()` 인터페이스 + 구현, `pref_general.xml` SwitchPreference, 문자열 리소스
- ✅ 기능 3 Portrait: D-pad/ABXY `+32dp`(기본) / `+48dp`(숨김) 리프트, 크기 140→160dp(숨김), L/R 60dp, SEL/STA 48dp, 숨김 시 제거
- ✅ 기능 3 Landscape: 동일 비율 적용
- ✅ `MelonModule.kt` DI 수정으로 `SettingsRepository` 주입

**Placeholder scan:** 없음 — 모든 스텝에 실제 코드 포함

**Type consistency:**
- `selStaButtonsSize`: Task 2 Step 4, Step 5, Task 3 Step 2, Step 3 모두 동일 이름
- `verticalOffset`: 모든 스텝에서 일관 사용
- `hideAuxiliary`: 시그니처 파라미터명과 호출 named arg 일치

**Potential issue:** `buildDefaultFoldingPortraitLayout()` 등 다른 private 메서드들도 `largeButtonsSize` 등을 로컬로 선언함. 이 메서드들은 foldable 화면 전용이므로 스펙 제외 범위("Foldable / 멀티디스플레이 레이아웃 변경 없음")에 해당 — 수정 불필요.
