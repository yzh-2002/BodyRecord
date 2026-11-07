package com.bodyrecord.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bodyrecord.app.RecordViewModel
import com.bodyrecord.app.data.*

@Composable
internal fun FoodEditor(original: Food?, newId: Int, initialCategory: String, vm: RecordViewModel, close: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(original?.name ?: "") }
    var category by rememberSaveable { mutableStateOf(original?.category ?: initialCategory) }
    var foodState by rememberSaveable { mutableStateOf(original?.state ?: "") }
    var carbs by rememberSaveable { mutableStateOf(original?.nutrients?.carbs?.toString() ?: "") }
    var protein by rememberSaveable { mutableStateOf(original?.nutrients?.protein?.toString() ?: "") }
    var fat by rememberSaveable { mutableStateOf(original?.nutrients?.fat?.toString() ?: "") }
    var description by rememberSaveable { mutableStateOf(original?.description ?: "") }
    var note by rememberSaveable { mutableStateOf(original?.note ?: "") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val state by vm.state.collectAsState()
    Editor(if(original == null) "新增食物" else "编辑食物",null,state.busy,close,{
        val values = listOf(decimal(carbs),decimal(protein),decimal(fat))
        if(name.isBlank() || name.length > 80) error = "请输入食物名称（最多 80 字）"
        else if(foodState.isBlank() || foodState.length > 80) error = "请注明生熟、干重或包装状态（最多 80 字）"
        else if(values.any { it == null || !it.isFinite() || it !in 0.0..100.0 }) error = "三类营养素均须填写每 100 g 的含量（0–100 g，没有则填 0）"
        else vm.saveFood(Food(original?.id ?: newId,name.trim(),category,foodState.trim(),description,note,
            original?.sourceName ?: "用户录入",MacroAmount(values[0]!!,values[1]!!,values[2]!!),original?.hidden ?: false),close)
    },saveLabel = "保存食物") {
        EntryField(name,{ name = it },"食物名称",Modifier.testTag("food-editor-name"))
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FoodCatalog.categories.forEach { value -> FilterChip(selected = category == value,onClick = { category = value },label = { Text(value) },modifier = Modifier.testTag("food-editor-category-$value")) }
        }
        EntryField(foodState,{ foodState = it },"计量状态（如：熟重 / 生重 / 包装成品）",Modifier.testTag("food-editor-state"))
        Text("以下填写每 100 g 可食部分的营养素含量，可参考食品包装标签。",color = Pine,style = MaterialTheme.typography.bodySmall)
        EntryField(carbs,{ carbs = it },"碳水",Modifier.testTag("food-editor-carbs"),numeric = true,suffix = "g / 100 g")
        EntryField(protein,{ protein = it },"蛋白质",Modifier.testTag("food-editor-protein"),numeric = true,suffix = "g / 100 g")
        EntryField(fat,{ fat = it },"脂肪",Modifier.testTag("food-editor-fat"),numeric = true,suffix = "g / 100 g")
        EntryField(description,{ description = it },"食物介绍（可选）")
        EntryField(note,{ note = it },"计量提示 / 备注（可选）")
        if(error != null) Text(error!!,color = MaterialTheme.colorScheme.error,modifier = Modifier.testTag("food-editor-error"))
    }
}
