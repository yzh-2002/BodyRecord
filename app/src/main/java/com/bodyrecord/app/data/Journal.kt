package com.bodyrecord.app.data

import java.time.LocalDate
import java.time.YearMonth

data class DayAnnotation(val date: String, val type: String = "未标记", val cheat: Boolean = false, val note: String = "")

data class DaySummary(val training: Boolean, val rest: Boolean, val cheat: Boolean, val groups: List<String>, val recorded: Boolean)

object Journal {
    val types = listOf("未标记", "训练日", "休息日")
    fun valid(day: DayAnnotation) = Rules.validDate(day.date) && day.type in types && day.note.length <= 1000
    fun summary(r: Records, date: String): DaySummary {
        val day = r.days.find { it.date == date }
        val trainings = r.trainings.filter { it.date == date }
        val training = trainings.isNotEmpty() || day?.type == "训练日"
        return DaySummary(training, !training && day?.type == "休息日",
            day?.cheat == true || r.meals.any { it.date == date && it.isCheatMeal },
            trainings.map { it.muscleGroup.ifBlank { "其他训练" } }.distinct(), date in r.dates())
    }
    /** Monday-first, including blank cells before/after this month. */
    fun monthCells(month: YearMonth): List<LocalDate?> {
        val offset = month.atDay(1).dayOfWeek.value - 1
        val size = ((offset + month.lengthOfMonth() + 6) / 7) * 7
        return List(size) { i -> (i - offset + 1).takeIf { it in 1..month.lengthOfMonth() }?.let(month::atDay) }
    }
}
