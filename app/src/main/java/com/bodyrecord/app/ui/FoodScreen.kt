package com.bodyrecord.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bodyrecord.app.RecordViewModel
import com.bodyrecord.app.data.*

@Composable
fun FoodScreen(r: Records, vm: RecordViewModel, recordMeal: () -> Unit) {
    var category by rememberSaveable { mutableStateOf("碳水") }
    var search by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showHidden by rememberSaveable { mutableStateOf(false) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var removingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var resettingId by rememberSaveable { mutableStateOf<Int?>(null) }
    val state by vm.state.collectAsState()
    val foods = vm.foods(r,true)
    fun edited(food: Food) = !food.custom && vm.builtInFoods.find { it.id == food.id } != food.copy(hidden = false)
    val filtered = foods.filter { it.hidden == showHidden && it.category == category && "${it.name} ${it.state} ${it.sourceName}".contains(search.trim(),true) }
    LazyColumn(Modifier.fillMaxSize().testTag("food-list"),contentPadding = PaddingValues(20.dp),verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageTitle("FOOD GUIDE","吃得明白一点") {
            FilledIconButton(onClick = recordMeal) { Icon(AppIcons.AddPhotoAlternate,"记录饮食照片") }
        } }
        item { SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            FoodCatalog.categories.forEachIndexed { i,c -> SegmentedButton(selected = category == c,onClick = { category = c },shape = SegmentedButtonDefaults.itemShape(i,3),modifier = Modifier.testTag("food-category-$c")) { Text(c) } }
        } }
        item { Panel(color = SoftGreen) {
            Text("$category · 常见食物来源",style = MaterialTheme.typography.titleMedium)
            Text(FoodCatalog.introductions.getValue(category),color = Pine,style = MaterialTheme.typography.bodyMedium)
            Text("分类按常见主要来源整理，一种食物可以同时含有三类营养素。",color = Muted,style = MaterialTheme.typography.bodySmall)
        } }
        item { OutlinedTextField(value = search,onValueChange = { search = it.take(100) },singleLine = true,placeholder = { Text("搜索食物名称或生熟状态") },leadingIcon = { Icon(AppIcons.Search,null) },modifier = Modifier.fillMaxWidth().testTag("food-search"),shape = MaterialTheme.shapes.medium) }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { creating = true },modifier = Modifier.weight(1f).testTag("food-add")) { Icon(AppIcons.Add,null,Modifier.size(18.dp)); Text("新增食物") }
            OutlinedButton(onClick = { showHidden = !showHidden },modifier = Modifier.weight(1f).testTag("food-show-hidden"),contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(if(showHidden) "返回食物库" else "已隐藏 (${foods.count { it.hidden }})")
            }
        } }
        item { Text("${filtered.size} 种食物 · 数值均为每 100 g 可食部分",color = Muted,style = MaterialTheme.typography.bodySmall) }
        items(filtered,key = { it.id }) { food ->
            Surface(modifier = Modifier.fillMaxWidth().testTag("food-${food.id}"),onClick = { selectedId = food.id },shape = MaterialTheme.shapes.medium,color = Color.White) {
                Column(Modifier.padding(18.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(food.name,style = MaterialTheme.typography.titleMedium)
                            Text("${food.state}${if(food.hidden) " · 已隐藏" else if(food.custom) " · 自定义" else if(edited(food)) " · 已编辑" else ""}",color = Muted,style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(AppIcons.ChevronRight,null,Modifier.size(18.dp),tint = Muted)
                    }
                    MacroValues(food.nutrients)
                }
            }
        }
        if(filtered.isEmpty()) item { EmptyState(AppIcons.Restaurant,if(showHidden) "此分类没有已隐藏的食物" else "没有匹配的食物","可切换分类、修改关键词或新增常吃的食物。") }
        item { Text("内置数据：USDA FoodData Central · SR Legacy 2018。可在详情中编辑或隐藏；自定义条目按你填写的数值保存。品牌、品种、含水量和烹调方式会影响实际含量。",color = Muted,style = MaterialTheme.typography.bodySmall) }
    }
    foods.find { it.id == selectedId }?.let { food -> FoodDetail(food,edited(food),state.busy,
        { selectedId = null },{ selectedId = null; editingId = food.id },{ selectedId = null; removingId = food.id },
        { vm.showFood(food); selectedId = null },{ selectedId = null; resettingId = food.id }) }
    if(creating || editingId != null) FoodEditor(foods.find { it.id == editingId },FoodCatalog.nextCustomId(r.foods),category,vm) { creating = false; editingId = null }
    foods.find { it.id == removingId }?.let { food ->
        AlertDialog(onDismissRequest = { removingId = null },title = { Text(if(food.custom) "删除这项食物？" else "隐藏这项食物？") },
            text = { Text(if(food.custom) "将删除“${food.name}”的自定义条目。" else "“${food.name}”将从食物库中隐藏，可在“已隐藏”中恢复。") },
            confirmButton = { TextButton(enabled = !state.busy,onClick = { vm.removeFood(food) { removingId = null } }) { Text(if(food.custom) "删除" else "隐藏") } },
            dismissButton = { TextButton(onClick = { removingId = null }) { Text("取消") } })
    }
    foods.find { it.id == resettingId }?.let { food ->
        AlertDialog(onDismissRequest = { resettingId = null },title = { Text("恢复内置数据？") },text = { Text("将恢复这项食物的原始名称、分类和营养数据，并清除你的修改。") },
            confirmButton = { TextButton(enabled = !state.busy,onClick = { vm.resetFood(food) { resettingId = null } }) { Text("恢复") } },
            dismissButton = { TextButton(onClick = { resettingId = null }) { Text("取消") } })
    }
}

@Composable
internal fun MacroValues(amount: MacroAmount) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf("碳水" to amount.carbs,"蛋白质" to amount.protein,"脂肪" to amount.fat).forEach { (label,value) ->
            Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label,color = Muted,style = MaterialTheme.typography.bodySmall)
                Text("${value.pretty()} g",fontFamily = NumberFont,fontSize = 21.sp,color = Pine)
            }
        }
    }
}

@Composable
private fun FoodDetail(food: Food, modified: Boolean, busy: Boolean, close: () -> Unit, edit: () -> Unit, remove: () -> Unit, show: () -> Unit, reset: () -> Unit) {
    var grams by rememberSaveable(food.id) { mutableStateOf("100") }
    val amount = decimal(grams)
    val valid = amount != null && amount.isFinite() && amount in 1.0..5000.0
    val uriHandler = LocalUriHandler.current
    Dialog(onDismissRequest = close,properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(12.dp).fillMaxHeight(.90f),shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(20.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween,verticalAlignment = Alignment.CenterVertically) {
                    Text("食物详情",style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = close) { Icon(AppIcons.Close,"关闭食物详情") }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(food.name,style = MaterialTheme.typography.headlineMedium)
                    Text("${food.category}来源 · ${food.state}",color = Pine)
                    Text(food.description,style = MaterialTheme.typography.bodyMedium)
                    Panel(color = SoftGreen) {
                        Text("每 100 g 可食部分",style = MaterialTheme.typography.titleSmall)
                        MacroValues(food.nutrients)
                    }
                    Text("份量换算",style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = grams,onValueChange = { grams = it.take(10) },label = { Text("食物重量") },suffix = { Text("g") },singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),isError = !valid,modifier = Modifier.fillMaxWidth().testTag("food-portion"),shape = MaterialTheme.shapes.small)
                    if(valid) MacroValues(food.portion(amount!!)) else Text("请输入 1–5000 g 的有效份量",color = MaterialTheme.colorScheme.error,style = MaterialTheme.typography.bodySmall)
                    Text("计量提示",style = MaterialTheme.typography.titleMedium)
                    Text(food.note,color = Muted,style = MaterialTheme.typography.bodyMedium)
                    Text(if(food.custom || modified) "当前数据由你填写或修改，请与所用包装标签、食材状态保持一致。份量换算不保存为已摄入量。" else "总碳水采用 USDA 差减法口径，包含膳食纤维；不是“净碳水”。份量换算仅展示食物成分，不保存为已摄入量。",color = Muted,style = MaterialTheme.typography.bodySmall)
                    Text("数据来源",style = MaterialTheme.typography.titleMedium)
                    Text(if(food.custom) "自定义食物 · 用户录入" else "${if(modified) "已在内置条目基础上编辑\n" else ""}原始来源：USDA FoodData Central · SR Legacy 2018\nFDC ID ${food.id}\n${food.sourceName}",color = Muted,style = MaterialTheme.typography.bodySmall)
                    if(!food.custom) TextButton(onClick = { uriHandler.openUri("https://fdc.nal.usda.gov/food-details/${food.id}/nutrients") }) { Text("查看 USDA 原始条目") }
                    if(!food.custom && modified) TextButton(onClick = reset,enabled = !busy) { Text("恢复内置数据") }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = edit,enabled = !busy,modifier = Modifier.weight(1f).testTag("food-edit")) { Text("编辑") }
                    OutlinedButton(onClick = if(food.hidden) show else remove,enabled = !busy,modifier = Modifier.weight(1f).testTag("food-remove")) { Text(if(food.hidden) "恢复显示" else if(food.custom) "删除" else "隐藏") }
                }
                Button(onClick = close,modifier = Modifier.fillMaxWidth()) { Text("完成") }
            }
        }
    }
}

fun IntakeRange.display(): String = if(low == high) low.pretty() else "${low.pretty()}–${high.pretty()}"

@Composable
fun NutritionReferencePanel(r: Records, configure: () -> Unit) {
    var scenario by rememberSaveable { mutableStateOf("力量训练日") }
    var band by rememberSaveable { mutableStateOf("常规配额") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val weight = r.weights.lastOrNull()
    val sex = r.profile.sex
    Panel {
        SectionTitle("图示摄入参考",action = "身体信息",onClick = configure)
        Text("图示配额 × 最新体重，展示对应摄入范围。克数指营养素质量，不是食物重量。",color = Muted,style = MaterialTheme.typography.bodySmall)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ImageIntakeReference.scenarios.forEach { value -> FilterChip(selected = scenario == value,onClick = { scenario = value },label = { Text(value) },modifier = Modifier.testTag("intake-scenario-$value")) }
        }
        if(sex !in listOf("男","女")) {
            Text("在身体信息中选择性别后，可用最新体重换算图示范围。",color = Pine,style = MaterialTheme.typography.bodyMedium)
            ReferenceTable()
        } else {
            val coeffs = ImageIntakeReference.coefficients(sex,scenario,band)
            val values = if(weight == null) coeffs else ImageIntakeReference.grams(weight.kg,coeffs)
            Text(if(weight == null) "$sex · 尚未记录体重，显示 g/kg/天" else "$sex · ${weight.kg.pretty()} kg · 记录于 ${weight.date}",color = Pine,style = MaterialTheme.typography.labelLarge)
            val rows = listOf(Triple("碳水",values.carbs,coeffs.carbs),Triple("蛋白质",values.protein,coeffs.protein),Triple("脂肪",values.fat,coeffs.fat))
            rows.forEach { (label,value,coefficient) ->
                Row(Modifier.fillMaxWidth().testTag("intake-value-$label"),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(label,fontWeight = FontWeight.SemiBold)
                        if(weight != null) Text("${coefficient.display()} g/kg/天",color = Muted,style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${value.display()} ${if(weight == null) "g/kg/天" else "g/天"}",fontFamily = NumberFont,fontSize = 22.sp,color = Pine)
                }
            }
            if(scenario != "不健身" && sex == "男" && band == "常规配额") Text("图中男性蛋白质：新手 1.5 g/kg，有基础者 2.0 g/kg；这里合并展示为范围。",color = Muted,style = MaterialTheme.typography.bodySmall)
            if(scenario == "休息 / 有氧日") Text("休息 / 有氧日仅按原图改变碳水，蛋白质和脂肪沿用健身者档位；不健身者请查看对应场景。",color = Muted,style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { expanded = !expanded }) { Text(if(expanded) "收起图示特殊分档" else "查看图示特殊分档") }
            if(expanded) {
                Text("特殊分档仅展示原图信息，由你选择；不会根据 BMI 自动套用。",color = Muted,style = MaterialTheme.typography.bodySmall)
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ImageIntakeReference.bands.forEach { value -> FilterChip(selected = band == value,onClick = { band = value },label = { Text(value) },modifier = Modifier.testTag("intake-band-$value")) }
                }
            }
            if(band != "常规配额") Text("当前：$band（手动选择）",color = Orange,style = MaterialTheme.typography.labelSmall)
        }
        Text("来源：你提供的经验配额图。此处是原图数值的展示与乘法换算，不是个体化膳食建议；未成年人或有特殊营养需求者不宜直接套用。",color = Muted,style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReferenceTable() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("原图基础配额 · g/kg/天",style = MaterialTheme.typography.titleSmall)
        Text("男（力量日）：碳水 3–3.5 · 蛋白质 1.5–2 · 脂肪 0.8",style = MaterialTheme.typography.bodySmall)
        Text("女（力量日）：碳水 2.5–3 · 蛋白质 1.5 · 脂肪 0.8",style = MaterialTheme.typography.bodySmall)
        Text("不健身：碳水 2–2.5 · 蛋白质 1.2 · 脂肪 0.8",style = MaterialTheme.typography.bodySmall)
        Text("休息 / 有氧日：碳水 1.5–2，其他按适用档位",style = MaterialTheme.typography.bodySmall)
    }
}
