package com.bodyrecord.app.data

import android.content.Context
import org.json.JSONArray

data class MacroAmount(val carbs: Double, val protein: Double, val fat: Double) {
    fun scaled(factor: Double) = MacroAmount(carbs*factor,protein*factor,fat*factor)
}
data class Food(
    val id: Int, val name: String, val category: String, val state: String,
    val description: String, val note: String, val sourceName: String, val nutrients: MacroAmount,
    val hidden: Boolean = false
) {
    val custom: Boolean get() = id < 0
    fun portion(grams: Double): MacroAmount {
        require(grams.isFinite() && grams in 1.0..5000.0)
        return nutrients.scaled(grams/100)
    }
}
object FoodCatalog {
    fun merge(builtins: List<Food>, overrides: List<Food>, includeHidden: Boolean = false): List<Food> {
        val merged = builtins.associateBy { it.id }.toMutableMap()
        overrides.forEach { merged[it.id] = it }
        return merged.values.filter { includeHidden || !it.hidden }
    }
    fun nextCustomId(overrides: List<Food>): Int {
        val minimum = overrides.minOfOrNull { it.id }?.coerceAtMost(0) ?: 0
        require(minimum > Int.MIN_VALUE) { "自定义食物数量已达上限" }
        return minimum - 1
    }
    val categories = listOf("碳水","蛋白质","脂肪")
    val introductions = mapOf(
        "碳水" to "常见来源包括米饭、燕麦、薯类、面包、玉米和水果。注意干重与熟重的区别。",
        "蛋白质" to "常见来源包括鱼、禽肉、蛋、奶、豆腐和豆类。不同食物同时也会含有脂肪或碳水。",
        "脂肪" to "常见来源包括植物油、坚果、种子和牛油果。油脂与坚果的脂肪含量和食用份量差别较大。"
    )
    fun load(context: Context): List<Food> {
        val array = JSONArray(context.assets.open("foods.json").bufferedReader().use { it.readText() })
        val result = List(array.length()) { index ->
            val j = array.getJSONObject(index)
            Food(j.getInt("id"),j.getString("name"),j.getString("category"),j.getString("state"),j.getString("description"),j.getString("note"),j.getString("sourceName"),
                MacroAmount(j.getDouble("carbs"),j.getDouble("protein"),j.getDouble("fat")))
        }
        require(result.map { it.id }.distinct().size == result.size)
        require(result.all { it.category in categories && it.state.isNotBlank() && listOf(it.nutrients.carbs,it.nutrients.protein,it.nutrients.fat).all { n -> n.isFinite() && n in 0.0..100.0 } })
        return result
    }
}

data class IntakeRange(val low: Double, val high: Double = low) {
    fun scaled(weight: Double): IntakeRange {
        require(Rules.weight(weight))
        return IntakeRange(low*weight,high*weight)
    }
}
data class IntakeReference(val carbs: IntakeRange, val protein: IntakeRange, val fat: IntakeRange)

/** A literal transcription of the user's image, not a dietary-needs estimator. */
object ImageIntakeReference {
    val scenarios = listOf("力量训练日","休息 / 有氧日","不健身")
    val bands = listOf("常规配额","图示 BMI > 28","图示 BMI > 32")
    fun coefficients(sex: String, scenario: String, band: String = bands[0]): IntakeReference {
        require(sex in listOf("男","女") && scenario in scenarios && band in bands)
        val nonGym = scenario == "不健身"
        val rest = scenario == "休息 / 有氧日"
        val carbs = when {
            rest -> IntakeRange(1.5,2.0)
            band == bands[2] -> IntakeRange(if(sex == "男") 2.0 else 1.7)
            band == bands[1] -> IntakeRange(if(sex == "男") 2.5 else 2.1)
            nonGym -> IntakeRange(2.0,2.5)
            sex == "男" -> IntakeRange(3.0,3.5)
            else -> IntakeRange(2.5,3.0)
        }
        val protein = when {
            band == bands[2] -> IntakeRange(if(nonGym) .8 else 1.0)
            band == bands[1] -> IntakeRange(if(nonGym) 1.0 else 1.2)
            nonGym -> IntakeRange(1.2)
            sex == "男" -> IntakeRange(1.5,2.0)
            else -> IntakeRange(1.5)
        }
        val fat = IntakeRange(when(band) { bands[2] -> .5; bands[1] -> .6; else -> .8 })
        return IntakeReference(carbs,protein,fat)
    }
    fun grams(weight: Double, coefficients: IntakeReference) = IntakeReference(coefficients.carbs.scaled(weight),coefficients.protein.scaled(weight),coefficients.fat.scaled(weight))
}
