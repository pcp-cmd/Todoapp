package com.todoapp.domain

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class RepeatRuleEngineTest {

    @Test
    fun `calculate next date for daily repeat`() {
        val rule = "FREQ=DAILY"
        val currentDate = createTimestamp(2026, Calendar.JANUARY, 15)
        val nextDate = RepeatRuleEngine.calculateNextDate(rule, currentDate)

        val calendar = Calendar.getInstance().apply { timeInMillis = nextDate }
        assertEquals(2026, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, calendar.get(Calendar.MONTH))
        assertEquals(16, calendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `calculate next date for weekly repeat with specific days`() {
        val rule = "FREQ=WEEKLY;BYDAY=MO,WE,FR"
        val monday = createTimestamp(2026, Calendar.JANUARY, 12)
        val nextDate = RepeatRuleEngine.calculateNextDate(rule, monday)

        val calendar = Calendar.getInstance().apply { timeInMillis = nextDate }
        assertEquals(Calendar.WEDNESDAY, calendar.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun `calculate next date for monthly repeat`() {
        val rule = "FREQ=MONTHLY;BYMONTHDAY=15"
        val currentDate = createTimestamp(2026, Calendar.JANUARY, 15)
        val nextDate = RepeatRuleEngine.calculateNextDate(rule, currentDate)

        val calendar = Calendar.getInstance().apply { timeInMillis = nextDate }
        assertEquals(Calendar.FEBRUARY, calendar.get(Calendar.MONTH))
        assertEquals(15, calendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `calculate next date for custom interval`() {
        val rule = "FREQ=DAILY;INTERVAL=3"
        val currentDate = createTimestamp(2026, Calendar.JANUARY, 15)
        val nextDate = RepeatRuleEngine.calculateNextDate(rule, currentDate)

        val calendar = Calendar.getInstance().apply { timeInMillis = nextDate }
        assertEquals(18, calendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `parse repeat rule string`() {
        val rule = "FREQ=WEEKLY;BYDAY=MO,WE,FR"
        val parsed = RepeatRuleEngine.parseRule(rule)

        assertEquals("WEEKLY", parsed["FREQ"])
        assertEquals("MO,WE,FR", parsed["BYDAY"])
    }

    private fun createTimestamp(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month, day, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
