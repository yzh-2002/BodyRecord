package com.bodyrecord.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bodyrecord.app.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class AppState(val records: Records = Records(), val loading: Boolean = true, val busy: Boolean = false, val message: String? = null)
class RecordViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = RecordRepository(app)
    private val _state = MutableStateFlow(AppState())
    val state = _state.asStateFlow()
    val builtInExercises = ExerciseCatalog.load(app)
    val builtInFoods = FoodCatalog.load(app)
    fun foods(records: Records, includeHidden: Boolean = false) = FoodCatalog.merge(builtInFoods,records.foods,includeHidden)
    fun exercises(records: Records, includeHidden: Boolean = false) = ExerciseCatalog.merge(builtInExercises,records.exercises,includeHidden)
    fun builtin(id: String) = builtInExercises.any { it.id == id }
    fun media(e: Exercise): Any? = e.media?.let { File(repo.mediaDir,it) }
        ?: e.demoAsset.takeIf { it.isNotEmpty() }?.let { "file:///android_asset/exercise-media/$it" }
    init { viewModelScope.launch { runCatching { repo.load() }.onSuccess { _state.value = AppState(records = it,loading = false) }.onFailure { _state.value = AppState(loading = false,message = "读取失败：${it.localizedMessage}") } } }
    fun photo(name: String) = File(repo.photoDir,name)
    fun clearMessage() { _state.value = _state.value.copy(message = null) }
    private fun run(success: String, done: () -> Unit = {}, block: suspend () -> Unit) {
        if(_state.value.busy || _state.value.loading) return
        _state.value = _state.value.copy(busy = true)
        viewModelScope.launch {
            try { block(); val records = repo.load(); _state.value = _state.value.copy(records = records,busy = false,message = success); done() }
            catch(e: Exception) { _state.value = _state.value.copy(busy = false,message = e.localizedMessage ?: "操作失败，请重试") }
        }
    }
    fun save(w: Weight, done: () -> Unit) = run("体重已保存",done) { repo.save(w) }
    fun saveDay(d: DayAnnotation, done: () -> Unit) = run("日历标记已保存",done) { repo.saveDay(d) }
    fun saveFood(f: Food, done: () -> Unit) = run("食物已保存",done) { repo.saveFood(f) }
    fun removeFood(f: Food, done: () -> Unit) = run(if(f.custom) "食物已删除" else "食物已隐藏",done) {
        if(f.custom) repo.delete("foods",f.id.toString()) else repo.saveFood(f.copy(hidden = true))
    }
    fun showFood(f: Food) = run("食物已恢复显示") { repo.saveFood(f.copy(hidden = false)) }
    fun resetFood(f: Food, done: () -> Unit) = run("已恢复内置食物",done) { repo.delete("foods",f.id.toString()) }
    fun save(t: Training, done: () -> Unit) = run("训练已保存",done) { repo.save(t) }
    fun save(p: Profile, done: () -> Unit) = run("目标已更新",done) { repo.save(p) }
    fun save(m: Meal, photos: List<Uri>, done: () -> Unit) = run("饮食已保存",done) { repo.saveMeal(m,photos) }
    fun saveExercise(e: Exercise, uri: Uri?, removeMedia: Boolean, done: () -> Unit) = run("动作已保存",done) { repo.saveExercise(e,uri,removeMedia) }
    fun removeExercise(e: Exercise, done: () -> Unit) = run(if(builtin(e.id)) "动作已隐藏" else "动作已删除",done) { repo.removeExercise(e,builtin(e.id)) }
    fun resetExercise(e: Exercise, done: () -> Unit) = run("已恢复内置动作",done) { repo.delete("exercises",e.id) }
    fun delete(table: String, id: String) = run("记录已删除") { repo.delete(table,id) }
    fun export(uri: Uri) = run("备份已导出，请妥善保存") { repo.export(uri) }
    fun restore(uri: Uri) = run("备份已恢复") { repo.restore(uri) }
}
