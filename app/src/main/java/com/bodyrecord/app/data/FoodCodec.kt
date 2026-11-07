package com.bodyrecord.app.data

import org.json.JSONObject

object FoodCodec {
    fun valid(f: Food) = f.id != 0 && f.name.isNotBlank() && f.name.length <= 80 &&
        f.category in FoodCatalog.categories && f.state.isNotBlank() && f.state.length <= 80 &&
        f.description.length <= 1000 && f.note.length <= 1000 && f.sourceName.length <= 500 &&
        listOf(f.nutrients.carbs, f.nutrients.protein, f.nutrients.fat).all { it.isFinite() && it in 0.0..100.0 }
    fun encode(f: Food) = JSONObject().apply {
        put("id", f.id); put("name", f.name); put("category", f.category); put("state", f.state)
        put("description", f.description); put("note", f.note); put("sourceName", f.sourceName)
        put("carbs", f.nutrients.carbs); put("protein", f.nutrients.protein); put("fat", f.nutrients.fat); put("hidden", f.hidden)
    }
    fun decode(j: JSONObject): Food = Food(j.getInt("id"), j.getString("name"), j.getString("category"), j.getString("state"),
        j.getString("description"), j.getString("note"), j.getString("sourceName"),
        MacroAmount(j.getDouble("carbs"), j.getDouble("protein"), j.getDouble("fat")), j.optBoolean("hidden")).also {
        require(valid(it)) { "食物信息无效" }
    }
}
