package me.magnum.melonds.ui.romlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.magnum.melonds.R
import me.magnum.melonds.domain.model.rom.Rom

private val placeholderColors = listOf(
    Color(0xFF5C6BC0),
    Color(0xFF26A69A),
    Color(0xFF8D6E63),
    Color(0xFF78909C),
    Color(0xFF66BB6A),
    Color(0xFFF4511E),
    Color(0xFF5E35B1),
    Color(0xFF039BE5),
)

@Composable
fun RecentlyPlayedSection(
    recentlyPlayed: List<Rom>,
    onRomClick: (Rom) -> Unit,
) {
    if (recentlyPlayed.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.recently_played_title),
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
                RecentlyPlayedCard(rom = rom, onClick = { onRomClick(rom) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
    }
}

@Composable
private fun RecentlyPlayedCard(rom: Rom, onClick: () -> Unit) {
    val placeholderColor = remember(rom.name) {
        placeholderColors[Math.abs(rom.name.hashCode()) % placeholderColors.size]
    }
    val displayName = rom.config.customName ?: rom.name

    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = 2.dp,
    ) {
        Column {
            // 컬러 플레이스홀더 (커버아트 없음)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(placeholderColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = displayName.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
            }

            Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                Text(
                    text = displayName,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp,
                )
                rom.lastPlayed?.let { date ->
                    val context = LocalContext.current
                    Text(
                        text = date.toRelativeTimeString(context),
                        fontSize = 10.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}
