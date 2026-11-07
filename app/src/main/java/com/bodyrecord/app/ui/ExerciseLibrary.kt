package com.bodyrecord.app.ui

import android.graphics.drawable.Animatable
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.bodyrecord.app.RecordViewModel
import com.bodyrecord.app.data.*

@Composable
fun DifficultyBadge(difficulty: String) {
    val color = when(difficulty) { "容易" -> Pine; "困难" -> Color(0xFFB14D40); else -> Orange }
    Surface(color = color.copy(alpha = .10f),shape = RoundedCornerShape(8.dp),contentColor = color) {
        Text(difficulty,modifier = Modifier.padding(horizontal = 9.dp,vertical = 4.dp),style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun ExercisePicker(vm: RecordViewModel, choose: (Exercise,Int?) -> Unit, close: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    Dialog(onDismissRequest = close,properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.97f).fillMaxHeight(.94f),shape = MaterialTheme.shapes.large,color = Paper) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp,end = 8.dp,top = 6.dp),horizontalArrangement = Arrangement.SpaceBetween,verticalAlignment = Alignment.CenterVertically) {
                    Text("选择训练动作",style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = close) { Icon(AppIcons.Close,"关闭动作选择") }
                }
                ExerciseLibraryScreen(state.records,vm,choose,compact = true)
            }
        }
    }
}

@Composable
fun ExerciseLibraryScreen(r: Records, vm: RecordViewModel, record: (Exercise,Int?) -> Unit, compact: Boolean = false) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var group by rememberSaveable { mutableStateOf("全部") }
    var difficulty by rememberSaveable { mutableStateOf("全部难度") }
    var search by rememberSaveable { mutableStateOf("") }
    var showHidden by rememberSaveable { mutableStateOf(false) }
    var onlyCustom by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var planId by rememberSaveable { mutableStateOf("three") }
    var dayIndex by rememberSaveable { mutableIntStateOf(-1) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val state by vm.state.collectAsStateWithLifecycle()
    val all = vm.exercises(r,true)
    val groups = (ExerciseCatalog.groups + all.map { it.group }).distinct()
    val plan = TrainingPlans.plans.first { it.id == planId }
    val selectedDay = plan.days.getOrNull(dayIndex)
    val dayGroups = selectedDay?.blocks?.map { it.group }
    val filtered = all.filter { e ->
        (showHidden || !e.hidden) && (!onlyCustom || !vm.builtin(e.id)) &&
        (group == "全部" || e.group == group) && (dayGroups == null || e.group in dayGroups) &&
        (difficulty == "全部难度" || e.difficulty == difficulty) &&
        "${e.name} ${e.group} ${e.pattern} ${e.equipment}".contains(search.trim(),true)
    }
    val selected = all.find { it.id == selectedId }
    fun sets(e: Exercise): Int? = selectedDay?.blocks?.find { it.group == e.group }?.prescription
        ?.let { Regex("× (\\d+) 组").find(it)?.groupValues?.get(1)?.toIntOrNull() }
    LazyColumn(Modifier.fillMaxSize().testTag("exercise-list"),contentPadding = PaddingValues(if(compact) 16.dp else 20.dp),verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            if(!compact) PageTitle("TRAINING LIBRARY","训练动作库") {
                FilledIconButton(onClick = { creating = true; editId = null },enabled = !state.busy) { Icon(AppIcons.Add,"新建动作") }
            } else Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween,verticalAlignment = Alignment.CenterVertically) {
                Text("${vm.exercises(r).size} 个动作 · 支持自定义",color = Muted,style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { creating = true; editId = null }) { Text("＋ 新建动作") }
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("动作库","训练计划").forEachIndexed { index,title -> SegmentedButton(selected = tab == index,onClick = { tab = index },shape = SegmentedButtonDefaults.itemShape(index,2),modifier = Modifier.testTag("exercise-tab-$index")) { Text(title) } }
            }
        }
        if(tab == 1) {
            item { Text("选择三分化或四分化，再按训练日挑选动作。",color = Muted,style = MaterialTheme.typography.bodyMedium) }
            item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrainingPlans.plans.forEach { p -> FilterChip(selected = planId == p.id,onClick = { planId = p.id; dayIndex = -1; group = "全部" },label = { Text(p.title) }) }
            } }
            items(plan.days.mapIndexed { i,d -> i to d },key = { it.first }) { (index,day) ->
                Panel {
                    Text(day.name,style = MaterialTheme.typography.titleMedium)
                    day.blocks.forEach { block ->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween,verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(block.group,fontWeight = FontWeight.SemiBold); Text(block.prescription,color = Muted,style = MaterialTheme.typography.bodySmall) }
                            TextButton(onClick = { tab = 0; dayIndex = index; group = block.group; search = ""; difficulty = "全部难度"; onlyCustom = false }) { Text("选动作") }
                        }
                        if(block.note.isNotBlank()) Text(block.note,color = Muted,style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = { tab = 0; dayIndex = index; group = "全部"; search = ""; difficulty = "全部难度"; onlyCustom = false },modifier = Modifier.fillMaxWidth()) { Text("查看本日全部动作") }
                }
            }
            item { Panel(color = SoftGreen) {
                Text("计划参考",style = MaterialTheme.typography.titleMedium)
                Text("按你提供的图示整理。三分化中的组数会带入记录表单，仍可修改；四分化未指定的组数由你填写。",color = Muted,style = MaterialTheme.typography.bodySmall)
                Text("图示组间休息参考：大肌群约 3 分钟，深蹲可更久；小肌群约 2 分钟。腹与臀均保留，按自己的目标选择。",color = Muted,style = MaterialTheme.typography.bodySmall)
            } }
        } else {
            if(selectedDay != null) item { Panel(color = SoftGreen) {
                SectionTitle(plan.title,selectedDay.name,"清除计划筛选",{ dayIndex = -1; group = "全部" })
                Text("按肌群选择动作，加入记录时会带入模板组数。",color = Muted,style = MaterialTheme.typography.bodySmall)
            } }
            item { OutlinedTextField(value = search,onValueChange = { search = it.take(100) },singleLine = true,placeholder = { Text("搜索动作、器械或肌群") },leadingIcon = { Icon(AppIcons.Search,null) },modifier = Modifier.fillMaxWidth().testTag("exercise-search"),shape = MaterialTheme.shapes.medium) }
            item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf("全部") + groups.filter { dayGroups == null || it in dayGroups }).forEach { g -> FilterChip(selected = group == g,onClick = { group = g },label = { Text(g) },modifier = Modifier.testTag("exercise-group-$g")) }
            } }
            item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf("全部难度") + ExerciseCatalog.difficulties).forEach { d -> FilterChip(selected = difficulty == d,onClick = { difficulty = d },label = { Text(d) },modifier = Modifier.testTag("exercise-difficulty-$d")) }
            } }
            item { Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween,verticalAlignment = Alignment.CenterVertically) {
                Text("${filtered.size} 个动作${if(onlyCustom) " · 仅自定义" else ""}",color = Muted,style = MaterialTheme.typography.bodySmall)
                Box {
                    TextButton(onClick = { menu = true }) { Text("筛选与管理") }
                    DropdownMenu(expanded = menu,onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(if(onlyCustom) "显示全部来源" else "仅显示自定义") },onClick = { onlyCustom = !onlyCustom; menu = false })
                        DropdownMenuItem(text = { Text(if(showHidden) "不显示隐藏动作" else "包含已隐藏动作") },onClick = { showHidden = !showHidden; menu = false })
                    }
                }
            } }
            if(filtered.isEmpty()) item { Panel { EmptyState(AppIcons.FitnessCenter,"没有匹配的动作","调整筛选，或添加自己的训练动作","新建动作",{ creating = true; editId = null }) } }
            items(filtered,key = { it.id }) { exercise ->
                Surface(modifier = Modifier.fillMaxWidth().testTag("exercise-${exercise.id}"),onClick = { selectedId = exercise.id },shape = MaterialTheme.shapes.medium,color = Color.White) {
                    Row(Modifier.padding(18.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(exercise.name,style = MaterialTheme.typography.titleMedium)
                            Text("${exercise.group} · ${exercise.pattern} · ${exercise.equipment}",color = Muted,style = MaterialTheme.typography.bodySmall)
                            Text(if(exercise.hidden) "已隐藏" else if(!vm.builtin(exercise.id)) "自定义动作" else if(vm.media(exercise) != null) "含动作介绍和演示" else "含动作介绍 · 可添加演示",color = Pine,style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.End,verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DifficultyBadge(exercise.difficulty)
                            Icon(AppIcons.ChevronRight,null,Modifier.size(18.dp),tint = Muted)
                        }
                    }
                }
            }
            item { Text("难度参考你提供的分级，可在动作详情中编辑调整。腹、臀及扩展版本采用补充参考等级。",color = Muted,style = MaterialTheme.typography.bodySmall) }
        }
    }
    if(selected != null) ExerciseDetail(selected,vm,state.busy,{ selectedId = null },{
        selectedId = null; record(selected,sets(selected))
    },{ editId = selected.id },{ deleting = true },{
        vm.resetExercise(selected) { selectedId = null }
    },{ vm.saveExercise(selected.copy(hidden = false),null,false) { selectedId = null } })
    if(creating || editId != null) {
        val original = all.find { it.id == editId }
        ExerciseEditor(original,if(group == "全部") "背" else group,state.busy,vm) { creating = false; editId = null }
    }
    if(deleting && selected != null) AlertDialog(onDismissRequest = { deleting = false },title = { Text(if(vm.builtin(selected.id)) "隐藏这个动作？" else "删除这个动作？") },
        text = { Text(if(vm.builtin(selected.id)) "可通过“筛选与管理 → 包含已隐藏动作”恢复。已有训练记录会保留。" else "已有训练记录和当时的动作名称会保留，动作库中的自定义内容将被删除。") },
        confirmButton = { TextButton(onClick = { vm.removeExercise(selected) { selectedId = null; deleting = false } },enabled = !state.busy) { Text("确认") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("取消") } })
}

@Composable
private fun ExerciseDetail(e: Exercise, vm: RecordViewModel, busy: Boolean, close: () -> Unit, record: () -> Unit, edit: () -> Unit, delete: () -> Unit, reset: () -> Unit, restore: () -> Unit) {
    Dialog(onDismissRequest = { if(!busy) close() },properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(12.dp).fillMaxHeight(.92f),shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(20.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween,verticalAlignment = Alignment.CenterVertically) {
                    Text("动作详情",style = MaterialTheme.typography.labelLarge,color = Muted)
                    IconButton(onClick = close,enabled = !busy) { Icon(AppIcons.Close,"关闭动作详情") }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(e.name,style = MaterialTheme.typography.headlineMedium)
                    Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(10.dp)) { Text(e.group,color = Pine); DifficultyBadge(e.difficulty) }
                    Text("器械：${e.equipment}",color = Muted)
                    ExerciseDemo(e,vm)
                    if(e.demoAsset.isNotEmpty() && e.media == null) Text("演示版本：${e.demoSource}\nExerciseDB V1 · 个人测试使用",color = Muted,style = MaterialTheme.typography.bodySmall)
                    Text("动作介绍",style = MaterialTheme.typography.titleMedium)
                    Text(e.description,style = MaterialTheme.typography.bodyMedium)
                    Text("动作步骤",style = MaterialTheme.typography.titleMedium)
                    e.steps.forEachIndexed { i,step -> Text("${i+1}. $step",style = MaterialTheme.typography.bodyMedium) }
                    Text("动作要点",style = MaterialTheme.typography.titleMedium)
                    Text(e.tips.ifBlank { "按可控的幅度和节奏练习。" },style = MaterialTheme.typography.bodyMedium,color = Muted)
                    OutlinedButton(onClick = edit,enabled = !busy,modifier = Modifier.fillMaxWidth()) { Text("编辑动作 / 自定义难度和演示") }
                    if(e.hidden) TextButton(onClick = restore,enabled = !busy) { Text("恢复显示") }
                    if(vm.builtin(e.id)) TextButton(onClick = reset,enabled = !busy) { Text("恢复内置内容") }
                    TextButton(onClick = delete,enabled = !busy) { Text(if(vm.builtin(e.id)) "隐藏动作" else "删除自定义动作",color = MaterialTheme.colorScheme.error) }
                }
                Button(onClick = record,enabled = !busy && !e.hidden,modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("选择此动作并记录") }
            }
        }
    }
}

@Composable
internal fun ExerciseDemo(e: Exercise, vm: RecordViewModel) {
    val model = vm.media(e)
    if(model == null) {
        Panel(color = SoftGreen) { Text("暂未收录匹配的动图",style = MaterialTheme.typography.titleSmall); Text("可以通过编辑动作导入自己的 GIF 或 WebP 演示。",color = Muted,style = MaterialTheme.typography.bodySmall) }
        return
    }
    val context = LocalContext.current
    val loader = remember(context) { ImageLoader.Builder(context).components {
        if(Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
    }.build() }
    DisposableEffect(loader) { onDispose { loader.shutdown() } }
    var playing by rememberSaveable(model.toString()) { mutableStateOf(true) }
    var drawable by remember(model) { mutableStateOf<Animatable?>(null) }
    var failed by remember(model) { mutableStateOf(false) }
    LaunchedEffect(playing,drawable) { if(playing) drawable?.start() else drawable?.stop() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.medium).background(SoftGreen),contentAlignment = Alignment.Center) {
            AsyncImage(model = model,imageLoader = loader,contentDescription = "${e.name}动作演示",contentScale = ContentScale.Fit,modifier = Modifier.fillMaxSize(),
                onSuccess = { drawable = it.result.drawable as? Animatable; if(!playing) drawable?.stop() },onError = { failed = true })
            if(failed) Text("无法显示演示，请重新导入图片。",Modifier.padding(16.dp),color = Muted)
        }
        if(drawable != null) TextButton(onClick = { playing = !playing }) { Text(if(playing) "暂停演示" else "播放演示") }
    }
}
