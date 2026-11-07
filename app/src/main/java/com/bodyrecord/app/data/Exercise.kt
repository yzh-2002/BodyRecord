package com.bodyrecord.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Exercise(
    val id: String, val name: String, val group: String, val pattern: String,
    val difficulty: String, val equipment: String, val description: String,
    val steps: List<String>, val tips: String,
    val demoAsset: String = "", val demoSource: String = "", val media: String? = null,
    val hidden: Boolean = false
)

object ExerciseCodec {
    fun encode(e: Exercise): JSONObject = JSONObject().apply {
        put("id",e.id); put("name",e.name); put("group",e.group); put("pattern",e.pattern)
        put("difficulty",e.difficulty); put("equipment",e.equipment); put("description",e.description)
        put("steps",JSONArray(e.steps)); put("tips",e.tips); put("demoAsset",e.demoAsset); put("demoSource",e.demoSource)
        put("media",e.media ?: JSONObject.NULL); put("hidden",e.hidden)
    }
    fun decode(j: JSONObject): Exercise {
        val steps = j.getJSONArray("steps")
        require(steps.length() <= 20) { "动作步骤过多" }
        val e = Exercise(j.getString("id"),j.getString("name"),j.getString("group"),j.optString("pattern","自定义"),
            j.getString("difficulty"),j.getString("equipment"),j.getString("description"),List(steps.length()) { steps.getString(it) },
            j.getString("tips"),j.optString("demoAsset"),j.optString("demoSource"),if(j.isNull("media")) null else j.getString("media"),j.optBoolean("hidden"))
        require(valid(e)) { "动作信息无效" }
        return e
    }
    fun valid(e: Exercise) = e.id.matches(Regex("[a-zA-Z0-9_-]{1,100}")) && e.name.isNotBlank() && e.name.length <= 80 &&
        e.group.isNotBlank() && e.group.length <= 24 && e.pattern.length <= 40 && e.difficulty in ExerciseCatalog.difficulties &&
        e.equipment.length <= 80 && e.description.length <= 2000 && e.steps.size <= 20 && e.steps.all { it.length <= 1000 } &&
        e.tips.length <= 2000 && (e.demoAsset.isEmpty() || e.demoAsset.matches(Regex("[a-zA-Z0-9_-]{1,100}\\.gif"))) &&
        e.demoSource.length <= 300 && (e.media == null || safeMedia(e.media))
    fun safeMedia(name: String) = name.matches(Regex("[a-zA-Z0-9-]{1,80}\\.(gif|webp|jpg|png)"))
}

object ExerciseCatalog {
    val groups = listOf("背","胸","肩前中束","肩后束","肱二头","肱三头","腿","腹","臀")
    val difficulties = listOf("容易","一般","困难")
    fun load(context: Context): List<Exercise> {
        val array = JSONArray(context.assets.open("exercises.json").bufferedReader().use { it.readText() })
        val result = List(array.length()) { ExerciseCodec.decode(array.getJSONObject(it)) }
        require(result.map { it.id }.distinct().size == result.size)
        return result
    }
    fun merge(builtIns: List<Exercise>, changes: List<Exercise>, includeHidden: Boolean = false): List<Exercise> {
        val merged = LinkedHashMap<String,Exercise>()
        builtIns.forEach { merged[it.id] = it }; changes.forEach { merged[it.id] = it }
        return merged.values.filter { includeHidden || !it.hidden }
    }
}

data class PlanBlock(val group: String, val prescription: String, val note: String = "")
data class PlanDay(val name: String, val blocks: List<PlanBlock>)
data class SplitPlan(val id: String, val title: String, val days: List<PlanDay>)

object TrainingPlans {
    val plans = listOf(
        SplitPlan("three","三分化",listOf(
            PlanDay("Day 1 · 背 + 肩后束 + 肱二头",listOf(PlanBlock("背","选 3 个动作 × 3 组","2 个下拉 + 1 个划船，或 2 个划船 + 1 个下拉"),PlanBlock("肩后束","选 2 个动作 × 3 组"),PlanBlock("肱二头","选 2 个动作 × 3 组"))),
            PlanDay("Day 2 · 胸 + 肩前中束 + 肱三头",listOf(PlanBlock("胸","选 3 个动作 × 3 组","2 个平板推 + 1 个器械推 / 上斜推 / 臂屈伸；夹胸可作为热身"),PlanBlock("肩前中束","选 2 个动作 × 3 组","2 个推举，或 1 个推举 + 1 个其他动作"),PlanBlock("肱三头","选 2 个动作 × 3 组"))),
            PlanDay("Day 3 · 腿 + 腹 / 臀",listOf(PlanBlock("腿","选 3 个动作 × 4 组","深蹲 + 2 个其他动作；也可交替安排以深蹲或硬拉为主的训练日"),PlanBlock("腹","任选动作，共 5–8 组"),PlanBlock("臀","选 2 个动作 × 4 组","按目标选择腹或臀，也可以自行组合")))
        )),
        SplitPlan("four-arms","四分化 · 手臂单练",listOf(
            PlanDay("Day 1 · 背 + 肩后束",listOf(PlanBlock("背","选择动作"),PlanBlock("肩后束","选择动作"))),
            PlanDay("Day 2 · 胸 + 肩前中束",listOf(PlanBlock("胸","选择动作"),PlanBlock("肩前中束","选择动作"))),
            PlanDay("Day 3 · 肱二头 + 肱三头",listOf(PlanBlock("肱二头","选择动作"),PlanBlock("肱三头","选择动作"))),
            PlanDay("Day 4 · 腿 + 腹 / 臀",listOf(PlanBlock("腿","选择动作"),PlanBlock("腹","按需选择"),PlanBlock("臀","按需选择")))
        )),
        SplitPlan("four-shoulders","四分化 · 肩单练",listOf(
            PlanDay("Day 1 · 背 + 肱二头",listOf(PlanBlock("背","选择动作"),PlanBlock("肱二头","选择动作"))),
            PlanDay("Day 2 · 胸 + 肱三头",listOf(PlanBlock("胸","选择动作"),PlanBlock("肱三头","选择动作"))),
            PlanDay("Day 3 · 肩",listOf(PlanBlock("肩前中束","选择动作"),PlanBlock("肩后束","选择动作"))),
            PlanDay("Day 4 · 腿 + 腹 / 臀",listOf(PlanBlock("腿","选择动作"),PlanBlock("腹","按需选择"),PlanBlock("臀","按需选择")))
        ))
    )
}
