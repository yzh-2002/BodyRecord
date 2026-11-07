package com.bodyrecord.app.ui

import android.net.Uri
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import com.bodyrecord.app.ui.AppIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.bodyrecord.app.RecordViewModel
import com.bodyrecord.app.data.*

@Composable
internal fun Editor(title: String, date: String?, busy: Boolean, onDismiss: () -> Unit, onSave: () -> Unit, saveLabel: String = "保存记录", content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = { if(!busy) onDismiss() },properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(16.dp).widthIn(max = 560.dp).imePadding(),shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(24.dp).heightIn(max = 650.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text(title,style = MaterialTheme.typography.titleLarge); if(date != null) Text(date,color = Muted,style = MaterialTheme.typography.bodySmall) }
                    IconButton(onClick = onDismiss,enabled = !busy) { Icon(AppIcons.Close,"关闭") }
                }
                Column(Modifier.weight(1f,fill = false).verticalScroll(rememberScrollState()),verticalArrangement = Arrangement.spacedBy(14.dp),content = content)
                Button(onClick = onSave,enabled = !busy,modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    if(busy) CircularProgressIndicator(Modifier.size(20.dp),strokeWidth = 2.dp,color = MaterialTheme.colorScheme.onPrimary) else Text(saveLabel)
                }
            }
        }
    }
}

@Composable
internal fun EntryField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, numeric: Boolean = false, decimal: Boolean = true, suffix: String? = null) {
    OutlinedTextField(value = value,onValueChange = { onChange(it.take(if(numeric) 12 else 1000)) },label = { Text(label) },
        modifier = modifier.fillMaxWidth(),singleLine = numeric,shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(keyboardType = if(numeric) { if(decimal) KeyboardType.Decimal else KeyboardType.Number } else KeyboardType.Text),
        suffix = if(suffix != null) ({ Text(suffix) }) else null)
}

@Composable
fun WeightEditor(date: String, original: Weight?, busy: Boolean, vm: RecordViewModel, close: () -> Unit) {
    var kg by rememberSaveable { mutableStateOf(original?.kg?.pretty() ?: "") }
    var note by rememberSaveable { mutableStateOf(original?.note ?: "") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Editor(if(original == null) "记录体重" else "编辑体重",date,busy,close,{
        val value = decimal(kg)
        if(value == null || !Rules.weight(value)) error = "请输入 20–500 kg 之间的有效体重"
        else vm.save(Weight(date,value,note),close)
    }) {
        EntryField(kg,{ kg = it; error = null },"体重",numeric = true,suffix = "kg")
        Text("每天保留一条体重，再次保存会更新当天记录。",color = Muted,style = MaterialTheme.typography.bodySmall)
        EntryField(note,{ note = it },"备注（可选）")
        if(error != null) Text(error!!,color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun MealEditor(date: String, original: Meal?, busy: Boolean, vm: RecordViewModel, close: () -> Unit) {
    var category by rememberSaveable { mutableStateOf(original?.category ?: "午餐") }
    var note by rememberSaveable { mutableStateOf(original?.note ?: "") }
    var cheat by rememberSaveable { mutableStateOf(original?.isCheatMeal ?: false) }
    var photos by rememberSaveable { mutableStateOf<List<String>>(original?.photos?.map { Uri.fromFile(vm.photo(it)).toString() } ?: emptyList()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { values ->
        if(values.isNotEmpty()) { photos = (photos + values.map { it.toString() }).distinct().take(6); error = null }
    }
    var expandedPhoto by remember { mutableStateOf<String?>(null) }
    Editor(if(original == null) "记录饮食" else "编辑饮食",date,busy,close,{
        if(photos.isEmpty()) error = "请从相册选择至少一张图片"
        else vm.save(original?.copy(date = date,category = category,note = note,isCheatMeal = cheat) ?: Meal(date = date,category = category,photos = emptyList(),note = note,isCheatMeal = cheat),photos.map { it.toUri() },close)
    }) {
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("早餐","午餐","晚餐","加餐").forEach { value -> FilterChip(selected = category == value,onClick = { category = value },label = { Text(value) }) }
        }
        if(photos.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            photos.forEach { uri ->
                Box { AsyncImage(model = uri.toUri(),contentDescription = "查看饮食照片",contentScale = ContentScale.Crop,modifier = Modifier.size(112.dp).clickable { expandedPhoto = uri })
                    FilledIconButton(onClick = { photos = photos.filter { it != uri } },modifier = Modifier.align(Alignment.TopEnd).size(32.dp)) { Icon(AppIcons.Close,"移除图片",Modifier.size(18.dp)) }
                }
            }
        }
        OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },enabled = photos.size < 6 && !busy,modifier = Modifier.fillMaxWidth()) {
            Icon(AppIcons.AddPhotoAlternate,null); Spacer(Modifier.width(8.dp)); Text("从相册选择 · ${photos.size}/6")
        }
        Text("照片保存在应用内，不会发送到服务器。",color = Muted,style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().testTag("meal-cheat").toggleable(value = cheat,role = Role.Checkbox,onValueChange = { cheat = it }),verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = cheat,onCheckedChange = null)
            Text("这是一餐放纵餐")
        }
        EntryField(note,{ note = it },"吃了什么？（可选）")
        if(error != null) Text(error!!,color = MaterialTheme.colorScheme.error)
    }
    expandedPhoto?.let { PhotoViewer(it.toUri()) { expandedPhoto = null } }
}

@Composable
fun TrainingEditor(date: String, original: Training?, busy: Boolean, vm: RecordViewModel, close: () -> Unit, initialExercise: Exercise? = null, initialSets: Int? = null) {
    var name by rememberSaveable { mutableStateOf(original?.name ?: initialExercise?.name ?: "") }
    var minutes by rememberSaveable { mutableStateOf(original?.minutes?.toString() ?: "") }
    var sets by rememberSaveable { mutableStateOf(original?.sets?.takeIf { it > 0 }?.toString() ?: initialSets?.toString() ?: "") }
    var reps by rememberSaveable { mutableStateOf(original?.reps?.takeIf { it > 0 }?.toString() ?: "") }
    var load by rememberSaveable { mutableStateOf(original?.load?.takeIf { it > 0 }?.pretty() ?: "") }
    var note by rememberSaveable { mutableStateOf(original?.note ?: "") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf(original?.exerciseId ?: initialExercise?.id) }
    var muscleGroup by rememberSaveable { mutableStateOf(original?.muscleGroup ?: initialExercise?.group ?: "") }
    var difficulty by rememberSaveable { mutableStateOf(original?.difficulty ?: initialExercise?.difficulty ?: "") }
    var picker by rememberSaveable { mutableStateOf(false) }
    Editor(if(original == null) "记录训练" else "编辑训练",date,busy,close,{
        val duration = minutes.toIntOrNull(); val s = if(sets.isBlank()) 0 else sets.toIntOrNull()
        val r = if(reps.isBlank()) 0 else reps.toIntOrNull(); val l = if(load.isBlank()) 0.0 else decimal(load)
        if(name.isBlank() || name.length > 80) error = "请输入训练名称（最多 80 字）"
        else if(duration == null || duration !in 1..1440) error = "时长须为 1–1440 分钟"
        else if(s == null || s !in 0..999 || r == null || r !in 0..9999 || l == null || !l.isFinite() || l !in 0.0..2000.0) error = "请检查组数、次数和负重是否有效"
        else vm.save(Training(original?.id ?: java.util.UUID.randomUUID().toString(),date,name.trim(),duration,s,r,l,note,selectedId,muscleGroup,difficulty),close)
    }) {
        OutlinedButton(onClick = { picker = true },modifier = Modifier.fillMaxWidth()) { Icon(AppIcons.FitnessCenter,null,Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("从动作库 / 训练计划选择") }
        if(muscleGroup.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(muscleGroup,color = Pine); if(difficulty.isNotEmpty()) DifficultyBadge(difficulty)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("力量训练","跑步","骑行","游泳","瑜伽").forEach { value -> SuggestionChip(onClick = { name = value },label = { Text(value) }) }
        }
        EntryField(name,{ name = it; selectedId = null; muscleGroup = ""; difficulty = "" },"训练 / 动作名称",modifier = Modifier.testTag("training-name"))
        EntryField(minutes,{ minutes = it },"训练时长",modifier = Modifier.testTag("training-minutes"),numeric = true,decimal = false,suffix = "分钟")
        Text("力量训练详情（可选）",style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EntryField(sets,{ sets = it },"组数",numeric = true,decimal = false,modifier = Modifier.weight(1f))
            EntryField(reps,{ reps = it },"每组次数",numeric = true,decimal = false,modifier = Modifier.weight(1f))
        }
        EntryField(load,{ load = it },"负重（可选）",numeric = true,suffix = "kg")
        EntryField(note,{ note = it },"训练感受 / 详情（可选）")
        if(error != null) Text(error!!,color = MaterialTheme.colorScheme.error)
    }
    if(picker) ExercisePicker(vm,{ e,suggested ->
        name = e.name; selectedId = e.id; muscleGroup = e.group; difficulty = e.difficulty
        if(suggested != null) sets = suggested.toString()
        picker = false
    },{ picker = false })
}

@Composable
fun ProfileEditor(original: Profile, busy: Boolean, vm: RecordViewModel, close: () -> Unit) {
    var target by rememberSaveable { mutableStateOf(original.target?.pretty() ?: "") }
    var height by rememberSaveable { mutableStateOf(original.height?.pretty() ?: "") }
    var goal by rememberSaveable { mutableIntStateOf(original.weeklyGoal) }
    var sex by rememberSaveable { mutableStateOf(original.sex) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Editor("身体信息与目标",null,busy,close,{
        val t = if(target.isBlank()) null else decimal(target); val h = if(height.isBlank()) null else decimal(height)
        if(target.isNotBlank() && (t == null || !Rules.weight(t))) error = "目标体重须为 20–500 kg"
        else if(height.isNotBlank() && (h == null || !h.isFinite() || h !in 80.0..250.0)) error = "身高须为 80–250 cm"
        else vm.save(Profile(t,h,goal,sex),close)
    },saveLabel = "保存设置") {
        Text("性别（用于图示摄入参考，可不填）",style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("" to "未设置","男" to "男","女" to "女").forEach { (value,label) -> FilterChip(selected = sex == value,onClick = { sex = value },label = { Text(label) },modifier = Modifier.testTag("profile-sex-$label")) }
        }
        EntryField(target,{ target = it },"目标体重（可选，留空可清除）",numeric = true,suffix = "kg")
        EntryField(height,{ height = it },"身高（可选，用于计算 BMI）",numeric = true,suffix = "cm")
        Text("每周训练 $goal 天",style = MaterialTheme.typography.titleMedium)
        Slider(value = goal.toFloat(),onValueChange = { goal = it.toInt() },valueRange = 1f..7f,steps = 5)
        Text("按自己的节奏记录，关注长期变化。",color = Muted,style = MaterialTheme.typography.bodySmall)
        if(error != null) Text(error!!,color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun PhotoViewer(model: Any?, close: () -> Unit) {
    Dialog(onDismissRequest = close,properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = androidx.compose.ui.graphics.Color.Black,modifier = Modifier.fillMaxWidth().fillMaxHeight(.85f)) {
            Box {
                AsyncImage(model = model,contentDescription = "饮食照片大图",contentScale = ContentScale.Fit,modifier = Modifier.fillMaxSize())
                IconButton(onClick = close,modifier = Modifier.align(Alignment.TopEnd)) { Icon(AppIcons.Close,"关闭大图",tint = androidx.compose.ui.graphics.Color.White) }
            }
        }
    }
}
