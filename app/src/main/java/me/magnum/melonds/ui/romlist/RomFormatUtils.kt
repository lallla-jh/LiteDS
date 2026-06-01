package me.magnum.melonds.ui.romlist

import android.content.Context
import me.magnum.melonds.R
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

/** Localized relative time, e.g. "Just now", "5 min ago", "2 hr ago", "3 days ago", "2 months ago". */
fun Date.toRelativeTimeString(context: Context): String {
    val diffMs = System.currentTimeMillis() - time
    return when {
        diffMs < 60_000L -> context.getString(R.string.time_just_now)
        diffMs < 3_600_000L -> context.getString(R.string.time_minutes_ago, (diffMs / 60_000L).toInt())
        diffMs < 86_400_000L -> context.getString(R.string.time_hours_ago, (diffMs / 3_600_000L).toInt())
        diffMs < 2_592_000_000L -> context.getString(R.string.time_days_ago, (diffMs / 86_400_000L).toInt())
        else -> context.getString(R.string.time_months_ago, (diffMs / 2_592_000_000L).toInt())
    }
}
