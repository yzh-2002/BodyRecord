package com.bodyrecord.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bodyrecord.app.RecordViewModel
import com.bodyrecord.app.data.*
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun JournalScreen(r: Records, date: LocalDate, onDate: (LocalDate) -> Unit, vm: RecordViewModel,
    editWeight: (Weight) -> Unit, editMeal: (Meal) -> Unit, editTraining: (Training) -> Unit,
    delete: (String,String) -> Unit, addMeal: () -> Unit, addTraining: () -> Unit, addWeight: () -> Unit) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(date).toString()) }
    var detail by rememberSaveable { mutableStateOf(false) }
    var history by rememberSaveable { mutableStateOf(false) }
    var marking by rememberSaveable { mutableStateOf(false) }
    val month = YearMonth.parse(monthText)
    val today = LocalDate.now()
    val windowSize = LocalWindowInfo.current.containerSize
    val compact = with(LocalDensity.current) { windowSize.height.toDp() < 740.dp }
    LaunchedEffect(date) { monthText = YearMonth.from(date).toString() }
    BackHandler(detail || history) { detail = false; history = false }
    Column(Modifier.fillMaxSize()) {
        if(detail || history) TextButton(onClick = { detail = false; history = false }) {
            Icon(AppIcons.ChevronLeft,null); Text("返回月历")
        }
        if(history) {
            RecordListScreen(r,date,onDate,vm,editWeight,editMeal,editTraining,delete,addMeal,addTraining)
        } else if(detail) {
            DayDetails(r,date,onDate,vm,editWeight,editMeal,editTraining,delete,addMeal,addTraining,addWeight,{ marking = true })
        } else {
            LazyColumn(Modifier.fillMaxSize().testTag("journal-calendar"),contentPadding = PaddingValues(if(compact) 8.dp else 16.dp),verticalArrangement = Arrangement.spacedBy(if(compact) 4.dp else 14.dp)) {
                item {
                    if(compact) Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
                        Text("日历里的日常",style = MaterialTheme.typography.titleLarge,modifier = Modifier.weight(1f))
                        IconButton(onClick = { history = true },modifier = Modifier.size(40.dp)) { Icon(AppIcons.Search,"搜索全部记录") }
                    } else PageTitle("YOUR JOURNAL","日历里的日常") {
                        IconButton(onClick = { history = true }) { Icon(AppIcons.Search,"搜索全部记录") }
                    }
                }
                item { Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { monthText = month.minusMonths(1).toString() },enabled = month > YearMonth.of(1900,1),modifier = Modifier.size(if(compact) 40.dp else 48.dp)) { Icon(AppIcons.ChevronLeft,"上个月") }
                    Text("${month.year} 年 ${month.monthValue} 月",style = MaterialTheme.typography.titleLarge,modifier = Modifier.weight(1f).testTag("calendar-month"))
                    TextButton(onClick = { onDate(today); monthText = YearMonth.from(today).toString() }) { Text("本月") }
                    IconButton(onClick = { monthText = month.plusMonths(1).toString() },enabled = month < YearMonth.from(today),modifier = Modifier.size(if(compact) 40.dp else 48.dp)) { Icon(AppIcons.ChevronRight,"下个月") }
                } }
                item { Surface(color = Color.White,shape = MaterialTheme.shapes.large) {
                  Column(Modifier.fillMaxWidth().padding(if(compact) 8.dp else 20.dp),verticalArrangement = Arrangement.spacedBy(if(compact) 6.dp else 12.dp)) {
                    Row(Modifier.fillMaxWidth()) { listOf("一","二","三","四","五","六","日").forEach {
                        Box(Modifier.weight(1f),contentAlignment = Alignment.Center) { Text(it,color = Muted,style = MaterialTheme.typography.labelMedium) }
                    } }
                    Column(verticalArrangement = Arrangement.spacedBy(if(compact) 0.dp else 4.dp)) {
                        Journal.monthCells(month).chunked(7).forEach { week ->
                            Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                week.forEach { day ->
                                    Box(Modifier.weight(1f)) {
                                        if(day != null) CalendarCell(day,day == today,day == date,Journal.summary(r,day.toString()),!day.isAfter(today),compact) {
                                            onDate(day); detail = true
                                        } else Spacer(Modifier.height(if(compact) 48.dp else 78.dp))
                                    }
                                }
                            }
                        }
                    }
                  }
                } }
                item { Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceEvenly) {
                    CalendarLegend(Pine,"训练日"); CalendarLegend(Muted,"休息日"); CalendarLegend(Orange,"放纵餐")
                } }
                item {
                    val summaries = (1..month.lengthOfMonth()).map { Journal.summary(r,month.atDay(it).toString()) }
                    Panel(color = SoftGreen) {
                        Text("这个月的节奏",style = MaterialTheme.typography.titleMedium)
                        Text("训练 ${summaries.count { it.training }} 天  ·  休息 ${summaries.count { it.rest }} 天  ·  放纵餐 ${summaries.count { it.cheat }} 天",color = Pine,style = MaterialTheme.typography.bodyMedium)
                        Text("点击日期查看体重、饮食照片和训练详情，或补充当天标记。空白日期表示尚未记录，休息日需手动标记。",color = Muted,style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    if(marking) DayMarkerEditor(r,date.toString(),vm) { marking = false }
}

@Composable
private fun CalendarLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(6.dp).background(color,CircleShape)); Text(label,color = Muted,style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CalendarCell(day: LocalDate, today: Boolean, selected: Boolean, summary: DaySummary, enabled: Boolean, compact: Boolean, click: () -> Unit) {
    val label = when { summary.training -> "训练日"; summary.rest -> "休息日"; summary.recorded -> "有记录"; else -> "未记录" }
    val groups = summary.groups.map { when(it) { "肩前中束" -> "肩前中"; "肱二头" -> "二头"; "肱三头" -> "三头"; "其他训练" -> "训练"; else -> it } }
    Surface(onClick = click,enabled = enabled,modifier = Modifier.fillMaxWidth().height(if(compact) 48.dp else 78.dp).testTag("calendar-day-$day")
        .semantics { contentDescription = "$day，$label，${summary.groups.joinToString("、")}${if(summary.cheat) "，含放纵餐" else ""}" },
        shape = MaterialTheme.shapes.small,color = if(selected) Lime else if(today) SoftGreen else Color.Transparent) {
        Column(Modifier.padding(vertical = if(compact) 2.dp else 5.dp),horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.spacedBy(if(compact) 1.dp else 2.dp)) {
            Text(day.dayOfMonth.toString(),fontSize = if(compact) 14.sp else 16.sp,lineHeight = if(compact) 18.sp else 25.sp,color = if(enabled) Ink else Muted.copy(alpha = .45f))
            if(summary.training || summary.rest) {
                val groupText = if(summary.rest) "休息" else if(groups.isEmpty()) "训练" else groups.take(2).joinToString("/") + if(groups.size > 2) "+" else ""
                if(compact) Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    Box(Modifier.size(3.dp).background(if(summary.training) Pine else Muted,CircleShape))
                    Text(groupText,fontSize = 8.sp,lineHeight = 10.sp,maxLines = 1,overflow = TextOverflow.Ellipsis,color = Pine)
                } else {
                    Box(Modifier.size(5.dp).background(if(summary.training) Pine else Muted,CircleShape))
                    Text(groupText,fontSize = 9.sp,lineHeight = 11.sp,maxLines = 1,overflow = TextOverflow.Ellipsis,color = Pine)
                }
            } else if(summary.recorded) Text("已记录",fontSize = if(compact) 8.sp else 9.sp,lineHeight = if(compact) 10.sp else 16.sp,color = Muted)
            if(summary.cheat) Text("● 放纵",fontSize = if(compact) 8.sp else 9.sp,lineHeight = if(compact) 10.sp else 11.sp,color = Orange)
        }
    }
}

@Composable
private fun DayDetails(r: Records, date: LocalDate, onDate: (LocalDate) -> Unit, vm: RecordViewModel,
    editWeight: (Weight) -> Unit, editMeal: (Meal) -> Unit, editTraining: (Training) -> Unit,
    delete: (String,String) -> Unit, addMeal: () -> Unit, addTraining: () -> Unit, addWeight: () -> Unit, mark: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf("全部") }
    val key = date.toString()
    val summary = Journal.summary(r,key)
    val annotation = r.days.find { it.date == key }
    val weights = r.weights.filter { it.date == key }
    val meals = r.meals.filter { it.date == key }
    val trainings = r.trainings.filter { it.date == key }
    LazyColumn(Modifier.fillMaxSize().testTag("day-details"),contentPadding = PaddingValues(horizontal = 20.dp,vertical = 8.dp),verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { DateNavigation(date,onDate) }
        item { Panel(color = SoftGreen) {
            SectionTitle("当天概况",action = "编辑标记",onClick = mark)
            Text(when { summary.training -> "● 训练日"; summary.rest -> "● 休息日"; else -> "尚未标记训练 / 休息" },color = Pine,modifier = Modifier.testTag("day-status"))
            if(summary.groups.isNotEmpty()) Text(summary.groups.joinToString(" · "),color = Pine)
            if(summary.cheat) Text("● 当天含放纵餐",color = Orange)
            if(!annotation?.note.isNullOrBlank()) Text(annotation!!.note,style = MaterialTheme.typography.bodyMedium)
            Text("体重 ${weights.size} 条 · 饮食 ${meals.size} 餐 · 训练 ${trainings.size} 项",color = Muted,style = MaterialTheme.typography.bodySmall)
        } }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("体重" to addWeight,"饮食" to addMeal,"训练" to addTraining).forEach { (text,action) ->
                OutlinedButton(onClick = action,modifier = Modifier.weight(1f),contentPadding = PaddingValues(horizontal = 6.dp,vertical = 10.dp)) { Text("＋ $text") }
            }
        } }
        item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("全部","体重","饮食","训练").forEach { value -> FilterChip(selected = filter == value,onClick = { filter = value },label = { Text(value) },modifier = Modifier.testTag("journal-filter-$value")) }
        } }
        if(filter in listOf("全部","体重")) items(weights,key = { "w${it.date}" }) { w -> WeightCard(w,{ editWeight(w) },{ delete("weights",w.date) }) }
        if(filter in listOf("全部","饮食")) items(meals,key = { "m${it.id}" }) { m -> MealCard(m,vm,{ editMeal(m) },{ delete("meals",m.id) }) }
        if(filter in listOf("全部","训练")) items(trainings,key = { "t${it.id}" }) { t -> TrainingCard(t,{ editTraining(t) },{ delete("trainings",t.id) }) }
        if((if(filter in listOf("全部","体重")) weights.size else 0) + (if(filter in listOf("全部","饮食")) meals.size else 0) + (if(filter in listOf("全部","训练")) trainings.size else 0) == 0) {
            item { EmptyState(AppIcons.EventNote,"这一天还没有${if(filter == "全部") "" else filter}记录","可添加记录，也可单独标记休息日或放纵餐。") }
        }
    }
}

@Composable
private fun DayMarkerEditor(r: Records, date: String, vm: RecordViewModel, close: () -> Unit) {
    val original = r.days.find { it.date == date }
    val hasTraining = r.trainings.any { it.date == date }
    var type by rememberSaveable(date) { mutableStateOf(if(hasTraining) "训练日" else original?.type ?: "未标记") }
    var cheat by rememberSaveable(date) { mutableStateOf(original?.cheat ?: false) }
    var note by rememberSaveable(date) { mutableStateOf(original?.note ?: "") }
    val state by vm.state.collectAsState()
    Editor("当天标记",date,state.busy,close,{ vm.saveDay(DayAnnotation(date,type,cheat,note),close) },saveLabel = "保存标记") {
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Journal.types.forEach { value -> FilterChip(selected = type == value,onClick = { type = value },enabled = !hasTraining || value == "训练日",label = { Text(value) },modifier = Modifier.testTag("day-type-$value")) }
        }
        if(hasTraining) Text("当天已有训练记录，日历会自动显示训练日和训练部位。",color = Muted,style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().testTag("day-cheat").toggleable(value = cheat,role = Role.Checkbox,onValueChange = { cheat = it }),verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = cheat,onCheckedChange = null); Text("标记当天含放纵餐")
        }
        Text("饮食记录中标为放纵餐的餐次也会自动显示在日历上；需到对应餐次取消该标记。",color = Muted,style = MaterialTheme.typography.bodySmall)
        EntryField(note,{ note = it },"当天备注（可选）",Modifier.testTag("day-note"))
    }
}
