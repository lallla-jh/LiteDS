# Phase 5B — 일시정지 메뉴 배속 스피너 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 일시정지 메뉴 하단에 `‹ 무제한 ›` 형태의 배속 스피너 행을 추가하여, 게임 중 패스트포워드 최대 속도를 즉시 변경할 수 있게 함.

**Architecture:** `EmulatorViewModel`에 `setFastForwardSpeed(Float)` 추가 → `SharedPreferences`에 저장 → `PauseMenuBottomSheetFragment`에 `SpeedSpinnerRow` composable 추가 → ViewModel에서 초기값 StateFlow 노출.

**Tech Stack:** Kotlin, Jetpack Compose (Material 2), Hilt

---

## File Map

| 역할 | 파일 |
|------|------|
| Create | (없음) |
| Modify | `app/src/main/java/me/magnum/melonds/ui/emulator/EmulatorViewModel.kt` |
| Modify | `app/src/main/java/me/magnum/melonds/ui/emulator/ui/PauseMenuBottomSheetFragment.kt` |

---

## Task 1 — `EmulatorViewModel`에 배속 변경 함수 추가

**Files:**
- Modify: `app/src/main/java/me/magnum/melonds/ui/emulator/EmulatorViewModel.kt`

배속 값 순서: `1.5f → 2.0f → 3.0f → 4.0f → 8.0f → -1.0f(무제한) → 1.5f`  
SharedPreferences 키: `"fast_forward_speed_multiplier"` (기존 키 재사용)  
초기값 읽기: `settingsRepository.getFastForwardSpeedMultiplier()`

- [ ] **Step 1: `EmulatorViewModel`에 배속 StateFlow와 변경 함수 추가**

`EmulatorViewModel.kt` 에서 기존 StateFlow 선언들 근처에 추가:

```kotlin
// fast-forward speed — pause menu spinner
private val _fastForwardSpeed = MutableStateFlow(settingsRepository.getFastForwardSpeedMultiplier())
val fastForwardSpeed: StateFlow<Float> = _fastForwardSpeed.asStateFlow()

fun setFastForwardSpeed(multiplier: Float) {
    _fastForwardSpeed.value = multiplier
    viewModelScope.launch(Dispatchers.IO) {
        settingsRepository.setFastForwardSpeedMultiplier(multiplier)
    }
}
```

> **Note:** `settingsRepository.setFastForwardSpeedMultiplier(multiplier)` 는 Task 1 Step 2에서 추가할 메서드임.

- [ ] **Step 2: `SettingsRepository` 인터페이스에 setter 추가**

`app/src/main/java/me/magnum/melonds/domain/repositories/SettingsRepository.kt`:

`getFastForwardSpeedMultiplier(): Float` 바로 아래에 추가:
```kotlin
fun setFastForwardSpeedMultiplier(multiplier: Float)
```

- [ ] **Step 3: `SharedPreferencesSettingsRepository`에 setter 구현 추가**

`app/src/main/java/me/magnum/melonds/impl/SharedPreferencesSettingsRepository.kt` 에서 `getFastForwardSpeedMultiplier()` 구현 바로 아래에 추가:

```kotlin
override fun setFastForwardSpeedMultiplier(multiplier: Float) {
    preferences.edit { putString("fast_forward_speed_multiplier", multiplier.toString()) }
}
```

> `preferences.edit { }` 는 `androidx.core.content.edit` 확장 함수. 이미 import 되어 있음 (`import androidx.core.content.edit` 확인).

- [ ] **Step 4: 빌드 확인**

```bash
cd app && ../gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/java/me/magnum/melonds/domain/repositories/SettingsRepository.kt \
        app/src/main/java/me/magnum/melonds/impl/SharedPreferencesSettingsRepository.kt \
        app/src/main/java/me/magnum/melonds/ui/emulator/EmulatorViewModel.kt
git commit -m "feat: add setFastForwardSpeedMultiplier to ViewModel and repository"
```

---

## Task 2 — 일시정지 메뉴에 `SpeedSpinnerRow` 추가

**Files:**
- Modify: `app/src/main/java/me/magnum/melonds/ui/emulator/ui/PauseMenuBottomSheetFragment.kt`

스피너 UI 형태:
```
⚡ 패스트포워드    ‹  무제한  ›
```
- 기존 `PauseActionButton` 행들과 같은 `16.dp` horizontal padding 내에 위치
- 현재값은 가운데 민트색 텍스트
- `‹` / `›` 탭 시 이전/다음 값으로 즉시 전환

배속 순서 및 표시 레이블:
```
-1.0f → "무제한"
1.5f  → "1.5×"
2.0f  → "2×"
3.0f  → "3×"
4.0f  → "4×"
8.0f  → "8×"
```

- [ ] **Step 1: `SpeedSpinnerRow` composable 추가**

`PauseMenuBottomSheetFragment.kt` 파일의 맨 아래 (SaveSlotCard 이후)에 private composable 추가:

```kotlin
@Composable
private fun SpeedSpinnerRow(
    currentSpeed: Float,
    mintColor: Color,
    onSpeedChange: (Float) -> Unit,
) {
    val speedCycle = listOf(-1.0f, 1.5f, 2.0f, 3.0f, 4.0f, 8.0f)
    val labels = mapOf(
        -1.0f to "무제한",
        1.5f  to "1.5×",
        2.0f  to "2×",
        3.0f  to "3×",
        4.0f  to "4×",
        8.0f  to "8×",
    )
    val currentIndex = speedCycle.indexOf(currentSpeed).takeIf { it >= 0 } ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.ElectricBolt,
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "패스트포워드",
            fontSize = 14.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.weight(1f),
        )
        // 이전 버튼
        IconButton(
            onClick = {
                val prev = speedCycle[(currentIndex - 1 + speedCycle.size) % speedCycle.size]
                onSpeedChange(prev)
            },
            modifier = Modifier.size(36.dp),
        ) {
            Text("‹", fontSize = 20.sp, color = mintColor, fontWeight = FontWeight.Bold)
        }
        // 현재값
        Text(
            text = labels[currentSpeed] ?: "무제한",
            fontSize = 14.sp,
            color = mintColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.widthIn(min = 52.dp),
            textAlign = TextAlign.Center,
        )
        // 다음 버튼
        IconButton(
            onClick = {
                val next = speedCycle[(currentIndex + 1) % speedCycle.size]
                onSpeedChange(next)
            },
            modifier = Modifier.size(36.dp),
        ) {
            Text("›", fontSize = 20.sp, color = mintColor, fontWeight = FontWeight.Bold)
        }
    }
}
```

import 추가 필요:
```kotlin
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.ui.unit.dp       // 이미 import됨
import androidx.compose.ui.text.style.TextAlign  // 이미 import됨
import androidx.compose.foundation.layout.widthIn
```

- [ ] **Step 2: `PauseMenuSheetContent` — `SpeedSpinnerRow` 연결**

`PauseMenuSheetContent` 시그니처에 ViewModel 접근 추가 (activityViewModels 불가 — composable이므로 파라미터로 받음):

현재 `PauseMenuSheetContent` 시그니처:
```kotlin
@Composable
private fun PauseMenuSheetContent(
    state: PauseMenuState,
    onResume: () -> Unit,
    onSave: (SaveStateSlot) -> Unit,
    onLoad: (SaveStateSlot) -> Unit,
    onReset: () -> Unit,
    onExit: () -> Unit,
    onCheats: () -> Unit,
    onDeleteSlot: (SaveStateSlot) -> Unit,
)
```

변경 후 (파라미터 2개 추가):
```kotlin
@Composable
private fun PauseMenuSheetContent(
    state: PauseMenuState,
    fastForwardSpeed: Float,
    onResume: () -> Unit,
    onSave: (SaveStateSlot) -> Unit,
    onLoad: (SaveStateSlot) -> Unit,
    onReset: () -> Unit,
    onExit: () -> Unit,
    onCheats: () -> Unit,
    onDeleteSlot: (SaveStateSlot) -> Unit,
    onSpeedChange: (Float) -> Unit,
)
```

- [ ] **Step 3: `PauseMenuSheetContent` 내부 — SpeedSpinnerRow 삽입**

`PauseMenuSheetContent` 내부에서 `Spacer(Modifier.height(16.dp))` (컬럼 맨 마지막) 바로 위에 추가:

```kotlin
// 저장 슬롯 스트립 블록 이후, 마지막 Spacer 이전에 삽입
Spacer(Modifier.height(8.dp))
Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
Spacer(Modifier.height(4.dp))
SpeedSpinnerRow(
    currentSpeed = fastForwardSpeed,
    mintColor = mintColor,
    onSpeedChange = onSpeedChange,
)

Spacer(Modifier.height(16.dp))
```

- [ ] **Step 4: Fragment의 `setContent` 블록 — 새 파라미터 전달**

`PauseMenuBottomSheetFragment.onCreateView()` 내 `setContent` 블록에서 `PauseMenuSheetContent` 호출 수정:

현재:
```kotlin
val state by viewModel.pauseMenuState.collectAsState()
state?.let { pauseState ->
    PauseMenuSheetContent(
        state = pauseState,
        onResume = { ... },
        // ... 기타 파라미터
    )
}
```

변경 후:
```kotlin
val state by viewModel.pauseMenuState.collectAsState()
val fastForwardSpeed by viewModel.fastForwardSpeed.collectAsState()
state?.let { pauseState ->
    PauseMenuSheetContent(
        state = pauseState,
        fastForwardSpeed = fastForwardSpeed,
        onResume = { ... },
        // ... 기존 파라미터 유지
        onSpeedChange = { viewModel.setFastForwardSpeed(it) },
    )
}
```

- [ ] **Step 5: 빌드 확인**

```bash
cd app && ../gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 커밋**

```bash
git add app/src/main/java/me/magnum/melonds/ui/emulator/ui/PauseMenuBottomSheetFragment.kt
git commit -m "feat: add fast-forward speed spinner to pause menu"
```

---

## Self-Review

**Spec coverage:**
- ✅ 배속 순서 사이클: `-1(무제한) → 1.5 → 2 → 3 → 4 → 8 → -1` (양방향)
- ✅ UI 형태: `⚡ 패스트포워드  ‹  무제한  ›` — 기존 옵션 행과 유사 스타일
- ✅ 현재값 민트색 텍스트 표시
- ✅ 변경 시 SharedPreferences 저장 (`fast_forward_speed_multiplier` 키 재사용)
- ✅ 에뮬레이터 코어 즉시 반영 API 없음 → 다음 패스트포워드 활성화 시점에 반영 (스펙 허용)
- ✅ 메뉴 열릴 때 `getFastForwardSpeedMultiplier()` 초기값 로드 (StateFlow 초기화 시 읽음)

**Placeholder scan:** 없음

**Type consistency:**
- `fastForwardSpeed: StateFlow<Float>` — ViewModel 선언과 Fragment 수집 타입 일치
- `setFastForwardSpeed(Float)` — ViewModel 함수명과 SpinnerRow `onSpeedChange` 람다 연결 일치
- `speedCycle: List<Float>` — `currentIndex` 계산, `prev`/`next` 색인 모두 Float 기준

**ElectricBolt icon 가용성 확인:** `Icons.Default.ElectricBolt`는 Material Icons Extended 라이브러리에 포함. 프로젝트가 이미 `androidx.compose.material:material-icons-extended` 의존성을 사용 중인지 확인 필요. 없으면 `Icons.Default.Speed` 또는 `Icons.Default.FastForward`로 대체 가능.
