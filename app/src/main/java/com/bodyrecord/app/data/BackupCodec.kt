package com.bodyrecord.app.data

import org.json.JSONArray
import org.json.JSONObject

/** Versioned, explicit portable format, independent of the SQLite schema. */
object BackupCodec {
    private fun json(block: JSONObject.() -> Unit) = JSONObject().apply(block)
    fun encode(r: Records): String = json {
        put("format", "bodyrecord"); put("version",4)
        put("weights",JSONArray(r.weights.map { w -> json { put("date",w.date); put("kg",w.kg); put("note",w.note) } }))
        put("meals",JSONArray(r.meals.map { m -> json { put("id",m.id); put("date",m.date); put("category",m.category); put("photos",JSONArray(m.photos)); put("note",m.note); put("isCheatMeal",m.isCheatMeal) } }))
        put("trainings",JSONArray(r.trainings.map { t -> json { put("id",t.id); put("date",t.date); put("name",t.name); put("minutes",t.minutes); put("sets",t.sets); put("reps",t.reps); put("load",t.load); put("note",t.note); put("exerciseId",t.exerciseId ?: JSONObject.NULL); put("muscleGroup",t.muscleGroup); put("difficulty",t.difficulty) } }))
        put("exercises",JSONArray(r.exercises.map(ExerciseCodec::encode)))
        put("foods",JSONArray(r.foods.map(FoodCodec::encode)))
        put("days",JSONArray(r.days.map { d -> json { put("date",d.date); put("type",d.type); put("cheat",d.cheat); put("note",d.note) } }))
        put("profile",json { put("target",r.profile.target ?: JSONObject.NULL); put("height",r.profile.height ?: JSONObject.NULL); put("weeklyGoal",r.profile.weeklyGoal); put("sex",r.profile.sex) })
    }.toString(2)

    fun decode(text: String): Records {
        val j = JSONObject(text)
        require(j.getString("format") == "bodyrecord" && j.getInt("version") in 1..4) { "不支持的备份格式或版本" }
        fun <T> read(key: String, block: (JSONObject) -> T): List<T> {
            val array = j.getJSONArray(key)
            require(array.length() <= 50000) { "备份记录过多" }
            return List(array.length()) { block(array.getJSONObject(it)) }
        }
        val w = read("weights") { Weight(it.getString("date"),it.getDouble("kg"),it.getString("note")) }
        val m = read("meals") { a ->
            val photos = a.getJSONArray("photos")
            require(photos.length() in 1..6)
            Meal(a.getString("id"),a.getString("date"),a.getString("category"),List(photos.length()) { photos.getString(it) },a.getString("note"),a.optBoolean("isCheatMeal"))
        }
        val t = read("trainings") { Training(it.getString("id"),it.getString("date"),it.getString("name"),it.getInt("minutes"),it.getInt("sets"),it.getInt("reps"),it.getDouble("load"),it.getString("note"),if(it.isNull("exerciseId")) null else it.getString("exerciseId"),it.optString("muscleGroup"),it.optString("difficulty")) }
        val exercises = if(j.has("exercises")) read("exercises",ExerciseCodec::decode) else emptyList()
        val foods = if(j.has("foods")) read("foods",FoodCodec::decode) else emptyList()
        val days = if(j.has("days")) read("days") { DayAnnotation(it.getString("date"),it.getString("type"),it.getBoolean("cheat"),it.getString("note")) } else emptyList()
        val p = j.getJSONObject("profile")
        val profile = Profile(if(p.isNull("target")) null else p.getDouble("target"),if(p.isNull("height")) null else p.getDouble("height"),p.getInt("weeklyGoal"),p.optString("sex"))
        require(w.all { Rules.validDate(it.date) && Rules.weight(it.kg) && it.note.length <= 1000 }) { "体重记录无效" }
        require(m.all { Rules.validDate(it.date) && it.id.isNotBlank() && it.id.length <= 100 && it.category in listOf("早餐","午餐","晚餐","加餐") && it.note.length <= 1000 && it.photos.all(::safePhoto) }) { "饮食记录无效" }
        require(t.all { Rules.validDate(it.date) && it.id.isNotBlank() && it.id.length <= 100 && Rules.training(it) && it.note.length <= 1000 && it.muscleGroup.length <= 24 && (it.difficulty.isEmpty() || it.difficulty in ExerciseCatalog.difficulties) && (it.exerciseId == null || it.exerciseId.matches(Regex("[a-zA-Z0-9_-]{1,100}"))) }) { "训练记录无效" }
        require(w.map { it.date }.distinct().size == w.size && m.map { it.id }.distinct().size == m.size && t.map { it.id }.distinct().size == t.size) { "备份含有重复记录" }
        require(exercises.map { it.id }.distinct().size == exercises.size) { "备份含有重复动作" }
        require(days.all(Journal::valid) && days.map { it.date }.distinct().size == days.size) { "日历标记无效或重复" }
        require(foods.map { it.id }.distinct().size == foods.size) { "备份含有重复食物" }
        require((profile.target == null || Rules.weight(profile.target)) && (profile.height == null || (profile.height.isFinite() && profile.height in 80.0..250.0)) && profile.weeklyGoal in 1..7 && profile.sex in listOf("","男","女")) { "身体信息无效" }
        return Records(w,m,t,profile,exercises,days,foods)
    }
    fun safePhoto(path: String) = Regex("[a-zA-Z0-9-]{1,80}\\.jpg").matches(path)
}
