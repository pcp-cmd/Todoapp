package com.todoapp.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.todoapp.data.entity.Task

private const val TAG = "TodoApp-Calendar"

class CalendarRepository(private val context: Context) {

    fun eventExists(eventId: Long): Boolean {
        val projection = arrayOf(CalendarContract.Events._ID)
        val selection = "${CalendarContract.Events._ID} = ?"
        val selectionArgs = arrayOf(eventId.toString())
        return try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection, selection, selectionArgs, null
            )?.use { it.count > 0 } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "eventExists failed for eventId=$eventId", e)
            false
        }
    }

    private fun getEffectiveReminderTime(task: Task): Long? {
        val time = task.reminderTime ?: task.dueDate ?: return null
        val cal = java.util.Calendar.getInstance().apply { this.timeInMillis = time }
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        // If the time is midnight (00:00), default to 09:00 for a reasonable reminder
        if (hour == 0 && minute == 0) {
            cal.set(java.util.Calendar.HOUR_OF_DAY, 9)
            return cal.timeInMillis
        }
        return time
    }

    fun createCalendarEvent(task: Task, deepLinkUri: String): Long? {
        val reminderTime = getEffectiveReminderTime(task)
        if (reminderTime == null) {
            Log.w(TAG, "createCalendarEvent: no effective time for '${task.title}'")
            return null
        }

        val calendarId = getDefaultCalendarId()
        if (calendarId < 0) {
            Log.e(TAG, "createCalendarEvent: no writable calendar found")
            return null
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, task.title)
            put(CalendarContract.Events.DESCRIPTION, buildEventDescription(task, deepLinkUri))
            put(CalendarContract.Events.DTSTART, reminderTime)
            put(CalendarContract.Events.DTEND, reminderTime + 3600000)
            put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
            if (task.repeatRule != null) {
                put(CalendarContract.Events.RRULE, task.repeatRule)
            }
        }

        return try {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = uri?.lastPathSegment?.toLongOrNull()
            if (eventId == null) {
                Log.e(TAG, "createCalendarEvent: insert returned null URI for '${task.title}'")
                return null
            }
            Log.d(TAG, "createCalendarEvent: inserted eventId=$eventId, calendarId=$calendarId for '${task.title}'")

            // Insert reminder
            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, 10)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            val reminderUri = context.contentResolver.insert(
                CalendarContract.Reminders.CONTENT_URI, reminderValues
            )
            if (reminderUri == null) {
                Log.w(TAG, "createCalendarEvent: failed to insert reminder for eventId=$eventId")
            }

            eventId
        } catch (e: Exception) {
            Log.e(TAG, "createCalendarEvent failed for '${task.title}'", e)
            null
        }
    }

    fun updateCalendarEvent(eventId: Long, task: Task, deepLinkUri: String) {
        val reminderTime = getEffectiveReminderTime(task)
        if (reminderTime == null) {
            Log.w(TAG, "updateCalendarEvent: no effective time for '${task.title}', deleting event instead")
            deleteCalendarEvent(eventId)
            return
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, task.title)
            put(CalendarContract.Events.DESCRIPTION, buildEventDescription(task, deepLinkUri))
            put(CalendarContract.Events.DTSTART, reminderTime)
            put(CalendarContract.Events.DTEND, reminderTime + 3600000)
            if (task.repeatRule != null) {
                put(CalendarContract.Events.RRULE, task.repeatRule)
            }
        }

        try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rows = context.contentResolver.update(uri, values, null, null)
            Log.d(TAG, "updateCalendarEvent: updated $rows rows for eventId=$eventId '${task.title}'")
        } catch (e: Exception) {
            Log.e(TAG, "updateCalendarEvent failed for eventId=$eventId '${task.title}'", e)
        }
    }

    fun deleteCalendarEvent(eventId: Long) {
        try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rows = context.contentResolver.delete(uri, null, null)
            Log.d(TAG, "deleteCalendarEvent: deleted $rows rows for eventId=$eventId")
        } catch (e: Exception) {
            Log.e(TAG, "deleteCalendarEvent failed for eventId=$eventId", e)
        }
    }

    /**
     * Find the best calendar to write events to.
     * Prefers: primary calendar > first owner-level calendar > first writable calendar.
     * Returns -1 if no writable calendar is found.
     */
    fun getDefaultCalendarId(): Long {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )
        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection, null, null, null
            )?.use { cursor ->
                var bestId = -1L
                var bestScore = -1
                val allCalendars = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val isPrimary = cursor.getInt(1) == 1
                    val accessLevel = cursor.getInt(2)
                    val displayName = cursor.getString(3) ?: "unknown"
                    allCalendars.add("id=$id primary=$isPrimary access=$accessLevel name=$displayName")

                    // Only consider calendars we can write to (owner or editor)
                    if (accessLevel < CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) continue

                    // Scoring: primary gets +2, owner gets +1
                    val score = (if (isPrimary) 2 else 0) +
                        (if (accessLevel >= CalendarContract.Calendars.CAL_ACCESS_OWNER) 1 else 0)

                    if (score > bestScore) {
                        bestScore = score
                        bestId = id
                    }
                }
                Log.d(TAG, "getDefaultCalendarId: found ${allCalendars.size} calendars: $allCalendars, selected=$bestId")
                bestId
            } ?: -1L
        } catch (e: Exception) {
            Log.e(TAG, "getDefaultCalendarId failed", e)
            -1L
        }
    }

    private fun buildEventDescription(task: Task, deepLinkUri: String): String {
        val sb = StringBuilder()
        task.description?.let { sb.append(it).append("\n\n") }
        sb.append("Open in TodoApp: $deepLinkUri")
        return sb.toString()
    }
}
