package com.bodyrecord.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

data class Weight(val date: String, val kg: Double, val note: String = "")
data class Meal(
    val id: String = UUID.randomUUID().toString(), val date: String,
    val category: String, val photos: List<String>, val note: String = "", val isCheatMeal: Boolean = false
)
data class Training(
    val id: String = UUID.randomUUID().toString(), val date: String,
    val name: String, val minutes: Int, val sets: Int = 0, val reps: Int = 0,
    val load: Double = 0.0, val note: String = "",
    val exerciseId: String? = null, val muscleGroup: String = "", val difficulty: String = ""
)
data class Profile(val target: Double? = null, val height: Double? = null, val weeklyGoal: Int = 3, val sex: String = "")
data class Records(
    val weights: List<Weight> = emptyList(), val meals: List<Meal> = emptyList(),
    val trainings: List<Training> = emptyList(), val profile: Profile = Profile(),
    val exercises: List<Exercise> = emptyList(),
    val days: List<DayAnnotation> = emptyList(), val foods: List<Food> = emptyList()
) {
    fun dates() = (weights.map { it.date } + meals.map { it.date } + trainings.map { it.date } + days.map { it.date }).toSet()
}

object Rules {
    fun validDate(value: String, today: LocalDate = LocalDate.now()): Boolean =
        runCatching { val d = LocalDate.parse(value); d.toString() == value && !d.isAfter(today) && d.year >= 1900 }.getOrDefault(false)
    fun weight(kg: Double) = kg.isFinite() && kg in 20.0..500.0
    fun training(t: Training) = t.name.isNotBlank() && t.name.length <= 80 && t.minutes in 1..1440 &&
        t.sets in 0..999 && t.reps in 0..9999 && t.load.isFinite() && t.load in 0.0..2000.0 &&
        t.muscleGroup.length <= 24 && (t.difficulty.isEmpty() || t.difficulty in ExerciseCatalog.difficulties) &&
        (t.exerciseId == null || t.exerciseId.matches(Regex("[a-zA-Z0-9_-]{1,100}")))
    fun window(weights: List<Weight>, end: LocalDate, days: Int) = weights.filter {
        val date = LocalDate.parse(it.date)
        !date.isBefore(end.minusDays((days - 1).toLong())) && !date.isAfter(end)
    }.sortedBy { it.date }
    fun xFraction(date: String, end: LocalDate, days: Int): Float =
        ChronoUnit.DAYS.between(end.minusDays((days - 1).toLong()), LocalDate.parse(date)).toFloat() / (days - 1)
    fun streak(dates: Set<String>, today: LocalDate): Int {
        var cursor = if (today.toString() in dates) today else today.minusDays(1)
        var count = 0
        while (cursor.toString() in dates) { count++; cursor = cursor.minusDays(1) }
        return count
    }
}
