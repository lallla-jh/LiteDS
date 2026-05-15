# LiteDS Phase 4 — 아트웍 + 플레이타임 + 최근 플레이 섹션

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** GameTDB에서 커버아트를 자동 다운로드하고, 홈 화면 ROM 카드에 플레이타임/최근 플레이 날짜를 표시하며, 최근 플레이 섹션(가로 스크롤 카드 10개)을 홈 화면 상단에 추가한다.

**Architecture:** `GameTDBArtworkRepository`가 ROM 헤더(offset 0x0C, 4바이트)에서 게임 코드를 추출해 GameTDB에서 커버아트를 다운로드·캐싱한다. `RomListViewModel`이 `recentlyPlayed: StateFlow<List<Rom>>`를 노출하고, `RomListFragment`에 Compose 기반 최근 플레이 섹션이 RecyclerView 상단에 추가된다. 아트웍은 ROM 카드(그리드/리스트 모두)에 Coil로 비동기 로딩된다. `Rom.lastPlayed` + `Rom.totalPlayTime`은 이미 추적·저장 중이므로 데이터 레이어 변경 없이 UI만 추가한다.

**Tech Stack:** Kotlin, `java.net.HttpURLConnection`(네트워크), Coil (`io.coil-kt:coil`), Jetpack Compose (Material 2), Hilt

**Project root:** `C:\Users\park\claude_project\melonds-joystick\melonDS-android\`

---

## File Map

| Task | 파일 | 종류 |
|------|------|------|
| Task 1 | `app/src/main/java/me/magnum/melonds/domain/repositories/ArtworkRepository.kt` | Create |
| Task 1 | `app/src/main/java/me/magnum/melonds/impl/GameTDBArtworkRepository.kt` | Create |
| Task 1 | `app/src/main/java/me/magnum/melonds/di/ArtworkModule.kt` | Create |
| Task 1 | `app/src/main/java/me/magnum/melonds/ui/romlist/RomListViewModel.kt` | Modify |
| Task 2 | `app/src/main/java/me/magnum/melonds/ui/romlist/RomFormatUtils.kt` | Create |
| Task 2 | `app/src/main/res/layout/item_rom_grid.xml` | Modify |
| Task 2 | `app/src/main/java/me/magnum/melonds/ui/romlist/RomListFragment.kt` | Modify |
| Task 3 | `app/src/main/java/me/magnum/melonds/ui/romlist/RecentlyPlayedSection.kt` | Create |
| Task 3 | `app/src/main/res/layout/rom_list_fragment.xml` | Modify |
| Task 3 | `app/src/main/java/me/magnum/melonds/ui/romlist/RomListFragment.kt` | Modify (2차) |
| Task 3 | `app/src/main/java/me/magnum/melonds/ui/romlist/RomListViewModel.kt` | Modify (2차) |

---

## Task 1: ArtworkRepository — GameTDB 커버아트 다운로드·캐싱

**Files:**
- Create: `app/src/main/java/me/magnum/melonds/domain/repositories/ArtworkRepository.kt`
- Create: `app/src/main/java/me/magnum/melonds/impl/GameTDBArtworkRepository.kt`
- Create: `app/src/main/java/me/magnum/melonds/di/ArtworkModule.kt`
- Modify: `app/src/main/java/me/magnum/melonds/ui/romlist/RomListViewModel.kt`

---

- [ ] **Step 1: ArtworkRepository 인터페이스 생성**

`app/src/main/java/me/magnum/melonds/domain/repositories/ArtworkRepository.kt` 신규 생성:

```kotlin
package me.magnum.melonds.domain.repositories

import me.magnum.melonds.domain.model.rom.Rom
import java.io.File

interface ArtworkRepository {
    /**
     * ROM 게임 코드를 이용해 GameTDB에서 커버아트를 가져온다.
     * 캐시가 있으면 즉시 반환, 없으면 다운로드 후 반환. 실패 시 null.
     */
    suspend fun getArtworkFile(rom: Rom): File?
}
```

---

- [ ] **Step 2: GameTDBArtworkRepository 구현체 생성**

`app/src/main/java/me/magnum/melonds/impl/GameTDBArtworkRepository.kt` 신규 생성:

```kotlin
package me.magnum.melonds.impl

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.magnum.melonds.domain.model.rom.Rom
import me.magnum.melonds.domain.repositories.ArtworkRepository
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class GameTDBArtworkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : ArtworkRepository {

    companion object {
        private const val GAMETDB_BASE = "https://art.gametdb.com/ds/coverS"
        private val REGIONS = listOf("KO", "EN", "US", "JA")
    }

    override suspend fun getArtworkFile(rom: Rom): File? = withContext(Dispatchers.IO) {
        val gameCode = readGameCode(rom.uri) ?: return@withContext null
        val cacheFile = File(context.cacheDir, "artwork/$gameCode.jpg")
        if (cacheFile.exists() && cacheFile.length() > 0) return@withContext cacheFile
        cacheFile.parentFile?.mkdirs()
        downloadArtwork(gameCode, cacheFile)
    }

    private fun downloadArtwork(gameCode: String, target: File): File? {
        for (region in REGIONS) {
            val urlStr = "$GAMETDB_BASE/$region/$gameCode.jpg"
            var conn: HttpURLConnection? = null
            try {
                conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.connectTimeout = 5_000
                conn.readTimeout = 10_000
                conn.connect()
                if (conn.responseCode == 200) {
                    conn.inputStream.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (target.length() > 0) return target
                    target.delete()
                }
            } catch (_: Exception) {
                target.delete()
            } finally {
                conn?.disconnect()
            }
        }
        return null
    }

    private fun readGameCode(uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val header = ByteArray(16)
            if (stream.read(header) < 16) return null
            val code = String(header, 12, 4, Charsets.US_ASCII)
            if (code.all { it.isLetterOrDigit() || it == ' ' }) code.trim().takeIf { it.length == 4 } else null
        }
    } catch (_: Exception) {
        null
    }
}
```

---

- [ ] **Step 3: Hilt 바인딩 모듈 생성**

`app/src/main/java/me/magnum/melonds/di/ArtworkModule.kt` 신규 생성:

```kotlin
package me.magnum.melonds.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.magnum.melonds.domain.repositories.ArtworkRepository
import me.magnum.melonds.impl.GameTDBArtworkRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ArtworkModule {
    @Binds
    @Singleton
    abstract fun bindArtworkRepository(impl: GameTDBArtworkRepository): ArtworkRepository
}
```

---

- [ ] **Step 4: RomListViewModel에 ArtworkRepository 주입 + getArtwork() 추가**

`app/src/main/java/me/magnum/melonds/ui/romlist/RomListViewModel.kt` 에서:

1. 클래스 헤더에 `artworkRepository: ArtworkRepository` 파라미터 추가:

```kotlin
@HiltViewModel
class RomListViewModel @Inject constructor(
    private val romsRepository: RomsRepository,
    private val settingsRepository: SettingsRepository,
    private val romIconProvider: RomIconProvider,
    private val uriPermissionManager: UriPermissionManager,
    private val directoryAccessValidator: DirectoryAccessValidator,
    private val artworkRepository: ArtworkRepository,
) : ViewModel() {
```

2. import 추가:

```kotlin
import me.magnum.melonds.domain.repositories.ArtworkRepository
```

3. 클래스 바디 내 `getRomIcon()` 함수 **바로 아래**에 추가:

```kotlin
    suspend fun getArtwork(rom: Rom): File? = artworkRepository.getArtworkFile(rom)
```

import 추가:
```kotlin
import java.io.File
```

---

- [ ] **Step 5: 컴파일 확인**

```bash
cd "C:/Users/park/claude_project/melonds-joystick/melonDS-android"
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:compileGitHubProdDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

에러 발생 시:
- `@Binds` abstract class에 `@Inject` 생성자 있는 impl이 필요 — `GameTDBArtworkRepository`에 `@Inject` 있는지 확인
- `ArtworkRepository` import 누락 확인

---

- [ ] **Step 6: 커밋**

```bash
cd "C:/Users/park/claude_project/melonds-joystick/melonDS-android"
git add app/src/main/java/me/magnum/melonds/domain/repositories/ArtworkRepository.kt \
        app/src/main/java/me/magnum/melonds/impl/GameTDBArtworkRepository.kt \
        app/src/main/java/me/magnum/melonds/di/ArtworkModule.kt \
        app/src/main/java/me/magnum/melonds/ui/romlist/RomListViewModel.kt
git commit -m "feat: add GameTDB artwork repository with local cache"
```

---

## Task 2: ROM 카드 UI 개선 — 아트웍 + 플레이타임 + 최근 플레이 날짜

**Files:**
- Create: `app/src/main/java/me/magnum/melonds/ui/romlist/RomFormatUtils.kt`
- Modify: `app/src/main/res/layout/item_rom_grid.xml`
- Modify: `app/src/main/java/me/magnum/melonds/ui/romlist/RomListFragment.kt`

---

- [ ] **Step 1: RomFormatUtils.kt 생성 — 시간 포맷 유틸**

`app/src/main/java/me/magnum/melonds/ui/romlist/RomFormatUtils.kt` 신규 생성:

```kotlin
package me.magnum.melonds.ui.romlist

import java.util.Date
import kotlin.time.Duration

/** "⏱ 2h 30m" 또는 "⏱ 45m" 형식. 1분 미만이면 빈 문자열. */
fun Duration.toPlayTimeString(): String {
    val totalMinutes = inWholeMinutes
    if (totalMinutes < 1) return ""
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "⏱ ${hours}h ${minutes}m" else "⏱ ${minutes}m"
}

/** "방금 전", "5분 전", "2시간 전", "3일 전", "2개월 전" 형식. */
fun Date.toRelativeTimeString(): String {
    val diffMs = System.currentTimeMillis() - time
    return when {
        diffMs < 60_000L -> "방금 전"
        diffMs < 3_600_000L -> "${diffMs / 60_000L}분 전"
        diffMs < 86_400_000L -> "${diffMs / 3_600_000L}시간 전"
        diffMs < 2_592_000_000L -> "${diffMs / 86_400_000L}일 전"
        else -> "${diffMs / 2_592_000_000L}개월 전"
    }
}
```

---

- [ ] **Step 2: item_rom_grid.xml 수정 — playtime + lastPlayed 텍스트뷰 추가**

`app/src/main/res/layout/item_rom_grid.xml` 전체를 아래로 교체:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/layoutRomItem"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="4dp"
    android:clickable="true"
    android:focusable="true"
    app:cardCornerRadius="8dp"
    app:cardElevation="2dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <ImageView
            android:id="@+id/imageRomIcon"
            android:layout_width="match_parent"
            android:layout_height="130dp"
            android:scaleType="centerCrop"
            android:background="?android:attr/colorControlHighlight" />

        <TextView
            android:id="@+id/textRomName"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:paddingStart="10dp"
            android:paddingEnd="10dp"
            android:paddingTop="8dp"
            android:paddingBottom="2dp"
            android:textSize="13sp"
            android:maxLines="1"
            android:ellipsize="end"
            android:textColor="?android:attr/textColorPrimary" />

        <TextView
            android:id="@+id/textPlayTime"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:paddingStart="10dp"
            android:paddingEnd="10dp"
            android:paddingBottom="2dp"
            android:textSize="11sp"
            android:textColor="?android:attr/textColorSecondary"
            android:visibility="gone" />

        <TextView
            android:id="@+id/textLastPlayed"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:paddingStart="10dp"
            android:paddingEnd="10dp"
            android:paddingBottom="8dp"
            android:textSize="11sp"
            android:textColor="?android:attr/textColorSecondary"
            android:visibility="gone" />

    </LinearLayout>
</androidx.cardview.widget.CardView>
```

---

- [ ] **Step 3: GridRomViewHolder 수정 — 아트웍 + 통계 표시**

`RomListFragment.kt` 의 `GridRomViewHolder` 내부 전체를 아래로 교체:

현재 위치: `inner class GridRomViewHolder(` 블록 전체 (약 line 336-377)

```kotlin
        inner class GridRomViewHolder(
            itemView: View,
            private val coroutineScope: CoroutineScope,
            onRomClick: (Rom) -> Unit,
            onRomLongClick: (Rom) -> Unit,
        ) : RecyclerView.ViewHolder(itemView) {

            private val imageViewRomIcon = itemView.findViewById<ImageView>(R.id.imageRomIcon)
            private val textViewRomName = itemView.findViewById<TextView>(R.id.textRomName)
            private val textViewPlayTime = itemView.findViewById<TextView>(R.id.textPlayTime)
            private val textViewLastPlayed = itemView.findViewById<TextView>(R.id.textLastPlayed)
            private lateinit var currentRom: Rom
            private var romIconLoadJob: Job? = null

            init {
                itemView.setOnClickListener { onRomClick(currentRom) }
                itemView.setOnLongClickListener { onRomLongClick(currentRom); true }
            }

            fun cleanup() { romIconLoadJob?.cancel() }

            fun setRom(rom: Rom, isEnabled: Boolean) {
                currentRom = rom
                textViewRomName.text = rom.config.customName ?: rom.name

                val playTimeStr = rom.totalPlayTime.toPlayTimeString()
                textViewPlayTime.text = playTimeStr
                textViewPlayTime.isVisible = playTimeStr.isNotEmpty()

                val lastPlayedStr = rom.lastPlayed?.toRelativeTimeString() ?: ""
                textViewLastPlayed.text = lastPlayedStr
                textViewLastPlayed.isVisible = lastPlayedStr.isNotEmpty()

                imageViewRomIcon.setImageDrawable(null)
                romIconLoadJob?.cancel()
                romIconLoadJob = coroutineScope.launch {
                    // 아트웍 먼저 시도, 없으면 DS 카트 아이콘 사용
                    val artworkFile = romListViewModel.getArtwork(rom)
                    if (artworkFile != null) {
                        imageViewRomIcon.load(artworkFile) {
                            crossfade(true)
                            if (!isEnabled) {
                                transformations(coil.transform.GrayscaleTransformation())
                            }
                        }
                    } else {
                        val romIcon = romListViewModel.getRomIcon(rom)
                        val iconDrawable = romIcon.bitmap?.toDrawable(itemView.resources)?.apply {
                            paint.isFilterBitmap = romIcon.filtering == RomIconFiltering.LINEAR
                            if (!isEnabled) {
                                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                                alpha = 127
                            }
                        }
                        imageViewRomIcon.setImageDrawable(iconDrawable)
                    }
                }

                itemView.setViewEnabledRecursive(isEnabled)
            }
        }
```

import 추가 (`RomListFragment.kt` 상단):
```kotlin
import coil.load
import coil.transform.GrayscaleTransformation
import androidx.core.view.isVisible
```

---

- [ ] **Step 4: RomViewHolder (리스트 모드) 수정 — 아트웍 + 통계 표시**

`RomViewHolder.setRom()` 함수에서 현재:
```kotlin
textViewRomPath.text = rom.fileName
```

아래로 교체:
```kotlin
val playTimeStr = rom.totalPlayTime.toPlayTimeString()
val lastPlayedStr = rom.lastPlayed?.toRelativeTimeString() ?: ""
textViewRomPath.text = listOfNotNull(
    playTimeStr.ifEmpty { null },
    lastPlayedStr.ifEmpty { null },
).joinToString("  ").ifEmpty { rom.fileName }
```

그리고 `romIconLoadJob` coroutine 블록 내부에서 `imageViewRomIcon.setImageDrawable(iconDrawable)` 전에 아트웍 시도:

기존:
```kotlin
                romIconLoadJob = coroutineScope.launch {
                    val romIcon = romListViewModel.getRomIcon(rom)
                    val iconDrawable = romIcon.bitmap?.toDrawable(itemView.resources)?.apply {
                        ...
                    }
                    imageViewRomIcon.setImageDrawable(iconDrawable)
                }
```

교체:
```kotlin
                romIconLoadJob = coroutineScope.launch {
                    val artworkFile = romListViewModel.getArtwork(rom)
                    if (artworkFile != null) {
                        imageViewRomIcon.load(artworkFile) {
                            crossfade(true)
                            size(coil.size.Size(144, 144))
                            if (!isEnabled) transformations(GrayscaleTransformation())
                        }
                    } else {
                        val romIcon = romListViewModel.getRomIcon(rom)
                        val iconDrawable = romIcon.bitmap?.toDrawable(itemView.resources)?.apply {
                            paint.isFilterBitmap = romIcon.filtering == RomIconFiltering.LINEAR
                            if (isEnabled) {
                                colorFilter = null
                                alpha = 255
                            } else {
                                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                                alpha = 127
                            }
                        }
                        imageViewRomIcon.setImageDrawable(iconDrawable)
                    }
                }
```

---

- [ ] **Step 5: 컴파일 확인**

```bash
cd "C:/Users/park/claude_project/melonds-joystick/melonDS-android"
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:compileGitHubProdDebugKotlin 2>&1 | tail -25
```

Expected: `BUILD SUCCESSFUL`

에러 발생 시:
- `coil.load` unresolved → `import coil.load` 확인
- `GrayscaleTransformation` → `import coil.transform.GrayscaleTransformation`
- `coil.size.Size` → `import coil.size.Size`
- `toPlayTimeString()` / `toRelativeTimeString()` → `RomFormatUtils.kt`가 같은 패키지인지 확인

---

- [ ] **Step 6: 커밋**

```bash
cd "C:/Users/park/claude_project/melonds-joystick/melonDS-android"
git add app/src/main/java/me/magnum/melonds/ui/romlist/RomFormatUtils.kt \
        app/src/main/res/layout/item_rom_grid.xml \
        app/src/main/java/me/magnum/melonds/ui/romlist/RomListFragment.kt
git commit -m "feat: show artwork, playtime, and last played date on ROM cards"
```

---

## Task 3: 최근 플레이 섹션 — 홈 화면 상단 가로 스크롤 카드

**Files:**
- Create: `app/src/main/java/me/magnum/melonds/ui/romlist/RecentlyPlayedSection.kt`
- Modify: `app/src/main/res/layout/rom_list_fragment.xml`
- Modify: `app/src/main/java/me/magnum/melonds/ui/romlist/RomListViewModel.kt`
- Modify: `app/src/main/java/me/magnum/melonds/ui/romlist/RomListFragment.kt`

---

- [ ] **Step 1: RomListViewModel에 recentlyPlayed StateFlow 추가**

`RomListViewModel.kt` 에서 `_roms` 선언 바로 **아래** (`val roms = _roms.asStateFlow()` 아래)에 추가:

```kotlin
    val recentlyPlayed: StateFlow<List<Rom>> = _roms
        .map { romList ->
            romList
                ?.filter { it.lastPlayed != null }
                ?.sortedByDescending { it.lastPlayed }
                ?.take(10)
                ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
```

필요한 import 추가:
```kotlin
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
```

---

- [ ] **Step 2: RecentlyPlayedSection.kt 생성**

`app/src/main/java/me/magnum/melonds/ui/romlist/RecentlyPlayedSection.kt` 신규 생성:

```kotlin
package me.magnum.melonds.ui.romlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import me.magnum.melonds.domain.model.rom.Rom
import java.io.File

// 플레이스홀더 배경색 팔레트 (게임 이름 해시 기반으로 선택)
private val placeholderColors = listOf(
    Color(0xFF5C6BC0), // Indigo
    Color(0xFF26A69A), // Teal
    Color(0xFF8D6E63), // Brown
    Color(0xFF78909C), // Blue Grey
    Color(0xFF66BB6A), // Green
    Color(0xFFF4511E), // Deep Orange
    Color(0xFF5E35B1), // Deep Purple
    Color(0xFF039BE5), // Light Blue
)

@Composable
fun RecentlyPlayedSection(
    recentlyPlayed: List<Rom>,
    getArtwork: suspend (Rom) -> File?,
    onRomClick: (Rom) -> Unit,
) {
    if (recentlyPlayed.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "최근 플레이",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(recentlyPlayed, key = { it.uri.toString() }) { rom ->
                RecentlyPlayedCard(
                    rom = rom,
                    getArtwork = getArtwork,
                    onClick = { onRomClick(rom) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
    }
}

@Composable
private fun RecentlyPlayedCard(
    rom: Rom,
    getArtwork: suspend (Rom) -> File?,
    onClick: () -> Unit,
) {
    var artworkFile by remember(rom.uri.toString()) { mutableStateOf<File?>(null) }
    LaunchedEffect(rom.uri.toString()) {
        artworkFile = getArtwork(rom)
    }

    val placeholderColor = remember(rom.name) {
        placeholderColors[Math.abs(rom.name.hashCode()) % placeholderColors.size]
    }

    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = 2.dp,
    ) {
        Column {
            // 커버아트 영역 (80dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(placeholderColor),
                contentAlignment = Alignment.Center,
            ) {
                if (artworkFile != null) {
                    AsyncImage(
                        model = artworkFile,
                        contentDescription = rom.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = (rom.config.customName ?: rom.name).take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                }
            }

            // 게임명 + 최근 플레이
            Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                Text(
                    text = rom.config.customName ?: rom.name,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp,
                )
                rom.lastPlayed?.let { date ->
                    Text(
                        text = date.toRelativeTimeString(),
                        fontSize = 10.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}
```

---

- [ ] **Step 3: rom_list_fragment.xml 수정 — LinearLayout + ComposeView 추가**

`app/src/main/res/layout/rom_list_fragment.xml` 전체를 아래로 교체:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    tools:context=".ui.romlist.RomListFragment"
    tools:menu="@menu/rom_list_menu">

    <!-- 최근 플레이 섹션 (Compose) — 비어있으면 자동으로 0dp 높이 -->
    <androidx.compose.ui.platform.ComposeView
        android:id="@+id/composeRecentlyPlayed"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

    <!-- ROM 목록 (SwipeRefresh + RecyclerView) -->
    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
            android:id="@+id/swipeRefreshRoms"
            android:layout_width="match_parent"
            android:layout_height="match_parent">

            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/listRoms"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:clipToPadding="false"
                tools:listitem="@layout/item_rom_configurable" />
        </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>

        <TextView
            android:id="@+id/textRomListEmpty"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:visibility="gone"
            style="?android:attr/textAppearanceMedium"
            android:text="@string/no_roms_found" />
    </FrameLayout>
</LinearLayout>
```

---

- [ ] **Step 4: RomListFragment에 최근 플레이 섹션 와이어링**

`RomListFragment.kt` 의 `onViewCreated()` 메서드 내부 **맨 끝** (마지막 `lifecycleScope.launch` 블록 이후)에 추가:

```kotlin
        // 최근 플레이 Compose 섹션
        binding.composeRecentlyPlayed.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MelonTheme {
                    val recentlyPlayed by romListViewModel.recentlyPlayed.collectAsState()
                    RecentlyPlayedSection(
                        recentlyPlayed = recentlyPlayed,
                        getArtwork = { rom -> romListViewModel.getArtwork(rom) },
                        onRomClick = { rom ->
                            romListViewModel.setRomLastPlayedNow(rom)
                            romSelectedListener?.invoke(rom)
                        },
                    )
                }
            }
        }
```

필요한 import 추가 (`RomListFragment.kt`):
```kotlin
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import me.magnum.melonds.ui.theme.MelonTheme
```

---

- [ ] **Step 5: 전체 빌드 + ADB 설치**

```bash
cd "C:/Users/park/claude_project/melonds-joystick/melonDS-android"
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew assembleGitHubProdDebug 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`

```bash
"C:/Users/park/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r "app/build/outputs/apk/gitHubProd/debug/app-gitHub-prod-debug.apk"
```

Expected: `Success`

에러 발생 시:
- `recentlyPlayed` stateIn import 누락 → `kotlinx.coroutines.flow.stateIn` 확인
- `collectAsState` → `androidx.compose.runtime.collectAsState`
- `ViewCompositionStrategy` → `androidx.compose.ui.platform.ViewCompositionStrategy`
- `MelonTheme` → `me.magnum.melonds.ui.theme.MelonTheme`

---

- [ ] **Step 6: 커밋**

```bash
cd "C:/Users/park/claude_project/melonds-joystick/melonDS-android"
git add app/src/main/java/me/magnum/melonds/ui/romlist/RecentlyPlayedSection.kt \
        app/src/main/res/layout/rom_list_fragment.xml \
        app/src/main/java/me/magnum/melonds/ui/romlist/RomListViewModel.kt \
        app/src/main/java/me/magnum/melonds/ui/romlist/RomListFragment.kt
git commit -m "feat: add recently played section with cover art to home screen"
```

---

## 자가 검토 (Spec Coverage)

| SPEC.md 항목 | 커버 태스크 |
|-------------|------------|
| 최근 플레이 섹션 (가로 스크롤 카드) | Task 3 (`RecentlyPlayedSection`, `LazyRow`) |
| 플레이타임 추적 표시 | Task 2 (`toPlayTimeString()`, grid/list 카드) |
| 최근 플레이 날짜 상대시간 표시 | Task 2 (`toRelativeTimeString()`) |
| GameTDB 아트웍 자동 다운로드 | Task 1 (`GameTDBArtworkRepository`) |
| Coil 기반 이미지 캐싱 | Task 1 (cacheDir 캐싱) + Task 2 (`imageView.load()`) |
| 아트웍 없을 때 플레이스홀더 | Task 3 (`placeholderColors` 해시 기반) |
| 최근 플레이 카드 탭 → 즉시 실행 | Task 3 (`onRomClick` → `romSelectedListener`) |
| 최근 플레이 최대 10개 | Task 1 (`.take(10)` in StateFlow) |
| 플레이타임 추적 자체 | 이미 구현됨 (`EmulatorViewModel` 1초 루프) |
| 최근 플레이 날짜 기록 | 이미 구현됨 (`RomListFragment.onRomClicked`) |
