# LiteDS Phase 5 — 조작 UX 개선 설계

## 기능 범위

3가지 독립 기능. 각각 별도 플랜으로 구현.

| # | 기능 | 크기 |
|---|------|------|
| 1 | 보조 버튼(L/R/SEL/STA) 숨김 토글 | Small |
| 2 | 일시정지 메뉴 배속 스피너 | Small |
| 3 | 기본 레이아웃 그립존 최적화 + 숨김 연동 | Medium |

---

## 기능 1 — 보조 버튼 숨김 토글

### 목표
게임에 따라 불필요한 Select/Start/L/R 버튼을 일괄 숨길 수 있는 설정 추가.

### 설계

**설정 키:** `"hide_auxiliary_buttons"` (Boolean, 기본값 `false`)

**저장 위치:** `SharedPreferencesSettingsRepository` — 기존 SharedPreferences 파일

**설정 UI 위치:** General 설정 화면 (`pref_general.xml`) 에 `SwitchPreference` 추가
- 제목: `"보조 버튼 숨기기"`
- 요약: `"게임 중 L, R, Select, Start 버튼을 숨깁니다"`
- 키: `hide_auxiliary_buttons`

**런타임 적용:** `DefaultLayoutProvider`가 레이아웃 계산 시 이 설정을 읽어 분기
- `false` (기본) → 기존 버튼 위치 포함한 레이아웃 반환
- `true` → `BUTTON_L`, `BUTTON_R`, `BUTTON_SELECT`, `BUTTON_START` 컴포넌트를 레이아웃에서 제외 + D-pad/ABXY 크기 자동 확대 (기능 3 연동)

**연동:** 기능 3의 레이아웃 계산 함수가 이 설정값을 파라미터로 받아 동작 분기.

### 영향 파일
- `app/src/main/res/xml/pref_general.xml` — SwitchPreference 추가
- `app/src/main/res/values/strings.xml` — 문자열 추가
- `app/src/main/java/me/magnum/melonds/domain/repositories/SettingsRepository.kt` — `getHideAuxiliaryButtons(): Boolean` 추가
- `app/src/main/java/me/magnum/melonds/impl/SharedPreferencesSettingsRepository.kt` — 구현 추가
- `app/src/main/java/me/magnum/melonds/impl/layout/DefaultLayoutProvider.kt` — 레이아웃 분기 (기능 3과 함께)

---

## 기능 2 — 일시정지 메뉴 배속 스피너

### 목표
게임 중 일시정지 메뉴에서 바로 배속을 변경할 수 있도록 스피너 UI 추가. 기존 General 설정에만 있던 `fast_forward_speed_multiplier`를 인게임에서 접근 가능하게 함.

### 설계

**개념:** 패스트포워드 버튼을 누를 때의 최대 속도 한도 설정. 게임 재생 중 항상 이 속도로 동작하는 것이 아니라, 패스트포워드를 활성화했을 때 최대 속도를 결정.

**배속 순서 (사이클):** `1.5×` → `2×` → `3×` → `4×` → `8×` → `무제한` → `1.5×`

값 매핑 (기존 `fast_forward_speed_multiplier` 값 재사용):
```
1.5× → multiplier = 1.5f
2×   → multiplier = 2.0f
3×   → multiplier = 3.0f
4×   → multiplier = 4.0f
8×   → multiplier = 8.0f
무제한 → multiplier = -1.0f  (기본값)
```

**UI 형태:** 일시정지 메뉴 하단에 행 추가
```
⚡ 패스트포워드    ‹  무제한  ›
```
- 기존 pause menu 옵션 버튼들과 동일한 시각 스타일 (Row, 아이콘 + 레이블 + ‹ 값 ›)
- 현재 설정값은 가운데에 텍스트로 표시 (민트색)
- `‹` / `›` 탭 시 이전/다음 값으로 즉시 전환
- 변경 즉시 `SharedPreferences`에 저장 (`fast_forward_speed_multiplier` 키 재사용)
- 에뮬레이터 코어 적용은 다음 패스트포워드 활성화 시점에 반영 (런타임 즉시 반영 API 있으면 추가)

**현재 설정값 불러오기:** 메뉴 열릴 때 `settingsRepository.getFastForwardSpeedMultiplier()` 호출로 초기값 설정.

### 영향 파일
- `app/src/main/java/me/magnum/melonds/ui/emulator/ui/PauseMenuBottomSheetFragment.kt` — 스피너 행 Composable 추가
- `app/src/main/java/me/magnum/melonds/ui/emulator/EmulatorViewModel.kt` — 배속 변경 함수 추가 (`setFastForwardSpeed(multiplier: Float)`)
- `app/src/main/res/values/strings.xml` — 필요 시 문자열 추가

### 데이터 흐름
```
사용자 탭 ‹/›
  → PauseMenuBottomSheetFragment 로컬 상태 업데이트
  → EmulatorViewModel.setFastForwardSpeed(multiplier) 호출
    → settingsRepository 저장
    → 에뮬레이터 코어에 즉시 반영
```

---

## 기능 3 — 기본 레이아웃 그립존 최적화

### 목표
`DefaultLayoutProvider`의 Portrait/Landscape 기본 레이아웃을 개선하여 엄지가 자연스럽게 닿는 위치에 버튼 배치. 보조 버튼 숨김 설정 연동.

### 설계

#### 버튼 크기 변경 (Portrait 기준)

| 버튼 | 현재 | 개선 B (표시) | B+ (숨김 시) |
|------|------|-------------|------------|
| D-pad / ABXY | 140dp | 140dp | 160dp |
| L / R | 50dp | 60dp | 제거 |
| SELECT / START | 40dp | 48dp | 제거 |

#### 수직 위치 변경

- **현재:** D-pad/ABXY가 화면 최하단 기준 배치 (bottom 기준 약 10dp)
- **개선:** D-pad/ABXY를 **32dp 위로** 이동 — 엄지 자연 그립 영역(화면 하단 40~60% 구간)에 위치
- **B+ (숨김 시):** D-pad/ABXY를 추가로 **16dp 더** 위로 이동 (총 48dp), 크기 확대로 생긴 여유 공간 활용

#### L/R 위치
- 기존: D-pad/ABXY 바로 위 (고정)
- 개선: D-pad와 나란히 수직 중심 맞춤 → L은 D-pad 상단 기준 정렬, R은 ABXY 상단 기준 정렬

#### SELECT/START 위치
- 중앙 하단 고정 유지, 크기만 40→48dp로 확대
- B+에서는 렌더링 생략

#### 분기 로직

```kotlin
// DefaultLayoutProvider.kt 수정 방향
fun buildDefaultPortraitLayout(
    screenSize: Size,
    safeInsets: Insets,
    hideAuxiliary: Boolean,   // ← 추가 파라미터
): UILayout {
    val dpadSize = if (hideAuxiliary) 160.dp else 140.dp
    val verticalOffset = if (hideAuxiliary) 48.dp else 32.dp  // 기존 대비 올리는 양

    // hideAuxiliary = true 이면 L/R/SEL/STA 컴포넌트 포함하지 않음
}
```

`DefaultLayoutProvider`는 `SettingsRepository`를 주입받아 `hideAuxiliary` 값을 내부에서 직접 읽거나, 호출 시 파라미터로 전달받는다. 기존 호출 지점과의 호환성을 위해 **파라미터 방식** 채택.

#### Landscape 대응
Portrait 기준 동일 비율로 Landscape 레이아웃에도 적용. `buildDefaultLandscapeLayout()`에 동일 분기 추가.

### 영향 파일
- `app/src/main/java/me/magnum/melonds/impl/layout/DefaultLayoutProvider.kt` — 핵심 수정
- `app/src/main/java/me/magnum/melonds/domain/repositories/SettingsRepository.kt` — `getHideAuxiliaryButtons()` (기능 1 공유)
- `app/src/main/java/me/magnum/melonds/impl/SharedPreferencesSettingsRepository.kt` — 구현 (기능 1 공유)

---

## 구현 순서 권장

1. **기능 1** — 설정 토글 추가 (DB/설정 레이어)
2. **기능 3** — DefaultLayoutProvider 개선 (기능 1 설정값 소비)
3. **기능 2** — 일시정지 메뉴 스피너 (독립적)

기능 1·3은 같은 설정 키를 공유하므로 묶어서 하나의 플랜으로 처리.  
기능 2는 독립적이므로 별도 플랜 또는 동시 진행 가능.

---

## 제외 범위

- GameTDB 아트웍, 네트워크 연동 없음
- 커스텀 레이아웃 에디터 변경 없음 (DEFAULT 레이아웃만 수정)
- 개별 버튼 선택적 숨김 없음 (일괄 숨김만)
- Foldable / 멀티디스플레이 레이아웃 변경 없음 (Portrait/Landscape만)
