package com.bodyrecord.app

import com.bodyrecord.app.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RulesTest {
    private val today = LocalDate.of(2026,11,7)
    @Test fun windowsIncludeStartAndEndButNotOutside() {
        val weights = listOf(Weight("2026-10-31",75.0),Weight("2026-11-01",74.0),Weight("2026-11-07",73.0),Weight("2026-11-08",72.0))
        assertEquals(listOf("2026-11-01","2026-11-07"),Rules.window(weights,today,7).map { it.date })
        assertEquals(0f,Rules.xFraction("2026-11-01",today,7),.0001f)
        assertEquals(1f,Rules.xFraction("2026-11-07",today,7),.0001f)
    }
    @Test fun monthWindowCrossesYear() {
        val end = LocalDate.of(2026,1,5)
        assertEquals(1,Rules.window(listOf(Weight("2025-12-07",75.0),Weight("2025-12-06",76.0)),end,30).size)
    }
    @Test fun streakAllowsTodayNotYetRecordedAndStopsAtGap() {
        assertEquals(2,Rules.streak(setOf("2026-11-05","2026-11-06"),today))
        assertEquals(3,Rules.streak(setOf("2026-11-05","2026-11-06","2026-11-07"),today))
        assertEquals(0,Rules.streak(setOf("2026-11-05"),today))
    }
    @Test fun rejectInvalidAndFutureDates() {
        assertFalse(Rules.validDate("2026-11-08",today))
        assertFalse(Rules.validDate("2026-02-30",today))
        assertFalse(Rules.validDate("1899-01-01",today))
        assertTrue(Rules.validDate("2024-02-29",today))
    }
    @Test fun rejectNonFiniteWeightAndInvalidTraining() {
        assertFalse(Rules.weight(Double.NaN)); assertFalse(Rules.weight(Double.POSITIVE_INFINITY)); assertFalse(Rules.weight(0.0))
        assertFalse(Rules.training(Training(date = "2026-11-07",name = "跑步",minutes = 0)))
        assertFalse(Rules.training(Training(date = "2026-11-07",name = "跑步",minutes = 20,load = Double.NaN)))
        assertTrue(Rules.training(Training(date = "2026-11-07",name = "深蹲",minutes = 40,sets = 4,reps = 12,load = 40.0)))
    }
}
