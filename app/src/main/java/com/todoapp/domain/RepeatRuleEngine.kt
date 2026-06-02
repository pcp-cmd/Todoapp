package com.todoapp.domain

import java.util.Calendar

object RepeatRuleEngine {

    fun calculateNextDate(rule: String, currentDate: Long): Long {
        val parsed = parseRule(rule)
        val calendar = Calendar.getInstance().apply { timeInMillis = currentDate }

        val freq = parsed["FREQ"] ?: return currentDate
        val interval = parsed["INTERVAL"]?.toIntOrNull() ?: 1

        when (freq) {
            "DAILY" -> {
                calendar.add(Calendar.DAY_OF_MONTH, interval)
            }
            "WEEKLY" -> {
                val byDay = parsed["BYDAY"]?.split(",")
                if (byDay != null) {
                    val targetDays = byDay.mapNotNull { dayStringToCalendarDay(it) }.toSet()
                    do {
                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                    } while (calendar.get(Calendar.DAY_OF_WEEK) !in targetDays)
                } else {
                    calendar.add(Calendar.WEEK_OF_YEAR, interval)
                }
            }
            "MONTHLY" -> {
                val byMonthDay = parsed["BYMONTHDAY"]?.toIntOrNull()
                val byWorkDay = parsed["BYWORKDAY"]?.toIntOrNull()

                if (byMonthDay != null) {
                    calendar.add(Calendar.MONTH, interval)
                    calendar.set(Calendar.DAY_OF_MONTH, byMonthDay)
                } else if (byWorkDay != null) {
                    calendar.add(Calendar.MONTH, interval)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    var workDayCount = 0
                    while (workDayCount < byWorkDay) {
                        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                        if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                            workDayCount++
                            if (workDayCount < byWorkDay) {
                                calendar.add(Calendar.DAY_OF_MONTH, 1)
                            }
                        } else {
                            calendar.add(Calendar.DAY_OF_MONTH, 1)
                        }
                    }
                } else {
                    calendar.add(Calendar.MONTH, interval)
                }
            }
            "YEARLY" -> {
                calendar.add(Calendar.YEAR, interval)
            }
        }

        return calendar.timeInMillis
    }

    fun parseRule(rule: String): Map<String, String> {
        return rule.split(";")
            .mapNotNull { part ->
                val keyValue = part.split("=")
                if (keyValue.size == 2) keyValue[0] to keyValue[1] else null
            }
            .toMap()
    }

    private fun dayStringToCalendarDay(day: String): Int? {
        return when (day.trim().uppercase()) {
            "SU" -> Calendar.SUNDAY
            "MO" -> Calendar.MONDAY
            "TU" -> Calendar.TUESDAY
            "WE" -> Calendar.WEDNESDAY
            "TH" -> Calendar.THURSDAY
            "FR" -> Calendar.FRIDAY
            "SA" -> Calendar.SATURDAY
            else -> null
        }
    }
}
