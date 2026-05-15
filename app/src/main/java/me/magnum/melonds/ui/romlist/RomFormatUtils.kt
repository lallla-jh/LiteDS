package me.magnum.melonds.ui.romlist

import java.util.Date
import kotlin.time.Duration

/** "⏱ 2h 30m" or "⏱ 45m". Returns empty string if under 1 minute. */
fun Duration.toPlayTimeString(): String {
    val totalMinutes = inWholeMinutes
    if (totalMinutes < 1) return ""
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "⏱ ${hours}h ${minutes}m" else "⏱ ${minutes}m"
}

/** "방금 전", "5분 전", "2시간 전", "3일 전", "2개월 전" */
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
