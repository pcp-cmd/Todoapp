package com.todoapp.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatRelativeDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = timestamp - now
        val days = diff / (1000 * 60 * 60 * 24)

        return when {
            days < 0 -> "已过期"
            days == 0L -> "今天"
            days == 1L -> "明天"
            days < 7 -> "${days}天后"
            else -> formatDate(timestamp)
        }
    }
}
