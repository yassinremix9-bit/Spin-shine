package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {
    fun formatCountdown(totalSeconds: Int): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0)
        val minutes = safeSeconds / 60
        val seconds = safeSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun formatRelativeArabic(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diffSeconds = ((nowMillis - timestampMillis) / 1000L).coerceAtLeast(0L)
        return when {
            diffSeconds < 45L -> "الآن"
            diffSeconds < 90L -> "منذ دقيقة"
            diffSeconds < 3600L -> "منذ ${diffSeconds / 60L} دقائق"
            diffSeconds < 7200L -> "منذ ساعة"
            diffSeconds < 86400L -> "منذ ${diffSeconds / 3600L} ساعات"
            else -> {
                val sdf = SimpleDateFormat("dd/MM - hh:mm a", Locale("ar"))
                sdf.format(Date(timestampMillis))
            }
        }
    }

    fun formatEstimatedFinish(remainingSeconds: Int, nowMillis: Long = System.currentTimeMillis()): String {
        val finishMillis = nowMillis + remainingSeconds * 1000L
        val sdf = SimpleDateFormat("hh:mm a", Locale("ar"))
        return sdf.format(Date(finishMillis))
    }
}
