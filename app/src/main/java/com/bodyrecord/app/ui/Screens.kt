package com.bodyrecord.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import com.bodyrecord.app.ui.AppIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bodyrecord.app.RecordViewModel
import com.bodyrecord.app.data.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun HomeScreen(r: Records, date: LocalDate, onDate: (LocalDate) -> Unit, addWeight: () -> Unit, addMeal: () -> Unit, addTraining: () -> Unit, trends: () -> Unit, journal: () -> Unit, vm: RecordViewModel, editMeal: (Meal) -> Unit, editTraining: (Training) -> Unit) {
    val weight = r.weights.find { it.date == date.toString() }
    val latest = r.weights.lastOrNull { it.date <= date.toString() }
    val meals = r.meals.filter { it.date == date.toString() }
    val trainings = r.trainings.filter { it.date == date.toString() }
    val completed = listOf(weight != null,meals.isNotEmpty(),trainings.isNotEmpty()).count { it }
    val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDays = r.trainings.filter { it.date >= monday.toString() && it.date <= monday.plusDays(6).toString() }.map { it.date }.distinct().size
    LazyColumn(Modifier.fillMaxSize(),contentPadding = PaddingValues(20.dp),verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageTitle("BODY RECORD  /  轻记",if(date == LocalDate.now()) "每一小步，都算数。" else "回看这一天。") }
        item { DateNavigation(date,onDate) }
        item {
            Panel(color = Pine) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween,verticalAlignment = Alignment.CenterVertically) {
                    Text(if(weight != null) "今日体重" else "最近一次体重",color = Color.White.copy(alpha = .7f),style = MaterialTheme.typography.labelLarge)
                    Surface(color = Color.White.copy(alpha = .12f),shape = CircleShape) { Text("${completed}/3 已记录",Modifier.padding(horizontal = 12.dp,vertical = 6.dp),color = Lime,style = MaterialTheme.typography.labelSmall) }
                }
                Row(verticalAlignment = Alignment.Bottom,horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(latest?.kg?.pretty() ?: "—",fontFamily = NumberFont,fontSize = 58.sp,color = Color.White)
                    Text("kg",color = Color.White.copy(alpha = .7f),modifier = Modifier.padding(bottom = 12.dp))
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween,verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(if(latest == null) "从今天开始，了解自己的节奏" else if(weight == null) "记录于 ${latest.date}" else "已留下一笔今天的变化",color = Color.White.copy(alpha = .7f),style = MaterialTheme.typography.bodySmall)
                        r.profile.target?.let { Text("目标 ${it.pretty()} kg",color = Lime,style = MaterialTheme.typography.bodySmall) }
                    }
                    FilledTonalButton(onClick = addWeight,colors = ButtonDefaults.filledTonalButtonColors(containerColor = Lime,contentColor = Ink)) { Icon(if(weight == null) AppIcons.Add else AppIcons.Edit,null,Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(if(weight == null) "记体重" else "修改") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(modifier = Modifier.weight(1f),shape = MaterialTheme.shapes.medium,color = SoftGreen,onClick = addMeal) {
                    Column(Modifier.padding(18.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(AppIcons.Restaurant,null,tint = Pine)
                        Text("饮食留影",style = MaterialTheme.typography.titleMedium)
                        Text(if(meals.isEmpty()) "用照片记住每一餐" else "今天已记录 ${meals.size} 餐",color = Muted,style = MaterialTheme.typography.bodySmall)
                        Text("＋ 记录饮食",color = Pine,style = MaterialTheme.typography.labelLarge)
                    }
                }
                Surface(modifier = Modifier.weight(1f),shape = MaterialTheme.shapes.medium,color = Color(0xFFF0EDE5),onClick = addTraining) {
                    Column(Modifier.padding(18.dp),verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(AppIcons.FitnessCenter,null,tint = Orange)
                        Text("训练日记",style = MaterialTheme.typography.titleMedium)
                        Text(if(trainings.isEmpty()) "给每一次努力留痕" else "今天训练 ${trainings.sumOf { it.minutes }} 分钟",color = Muted,style = MaterialTheme.typography.bodySmall)
                        Text("＋ 记录训练",color = Pine,style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        item { Panel {
            SectionTitle("体重小趋势","截至所选日期的近 7 天","查看趋势",trends)
            WeightChart(r.weights,date,7,r.profile.target)
        } }
        item { Panel {
            SectionTitle("本周的坚持","训练 $weekDays / ${r.profile.weeklyGoal} 天")
            Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("一","二","三","四","五","六","日").forEachIndexed { i,label ->
                    val d = monday.plusDays(i.toLong())
                    val active = r.trainings.any { it.date == d.toString() }
                    Column(horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(label,color = Muted,style = MaterialTheme.typography.bodySmall)
                        Box(Modifier.size(32.dp).background(if(active) Pine else SoftGreen,CircleShape),contentAlignment = Alignment.Center) {
                            if(active) Icon(AppIcons.Check,null,Modifier.size(18.dp),tint = Lime) else Text(d.dayOfMonth.toString(),color = Muted,style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        } }
        item { SectionTitle("当天记录",action = "查看记录",onClick = journal) }
        if(meals.isEmpty() && trainings.isEmpty()) item { Panel { EmptyState(AppIcons.Spa,"留一点时间给自己","吃好、动起来，慢慢积累。") } }
        items(meals,key = { "meal-${it.id}" }) { MealCard(it,vm,{ editMeal(it) }) }
        items(trainings,key = { "training-${it.id}" }) { TrainingCard(it,{ editTraining(it) }) }
        item { Text("记录生活，而不只是数字。",Modifier.fillMaxWidth().padding(vertical = 12.dp),color = Muted,style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun TrendsScreen(r: Records, date: LocalDate, onDate: (LocalDate) -> Unit, add: () -> Unit, edit: (Weight) -> Unit, delete: (Weight) -> Unit) {
    var days by rememberSaveable { mutableIntStateOf(7) }
    val points = Rules.window(r.weights,date,days)
    val delta = if(points.size > 1) points.last().kg - points.first().kg else null
    LazyColumn(Modifier.fillMaxSize(),contentPadding = PaddingValues(20.dp),verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageTitle("YOUR PROGRESS","看见每一点变化") { FilledIconButton(onClick = add) { Icon(AppIcons.Add,"记录体重") } } }
        item { DateNavigation(date,onDate) }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(7 to "周 · 7 天",30 to "月 · 30 天").forEachIndexed { index, pair -> SegmentedButton(selected = days == pair.first,onClick = { days = pair.first },shape = SegmentedButtonDefaults.itemShape(index,2)) { Text(pair.second) } }
            }
        }
        item { Panel {
            SectionTitle("体重波动","${date.minusDays((days-1).toLong())} — $date")
            Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("最近记录",points.lastOrNull()?.kg?.pretty() ?: "—","kg")
                Metric("期间变化",delta?.let { (if(it > 0) "+" else "") + it.pretty() } ?: "—","kg")
                Metric("记录天数",points.size.toString(),"天")
            }
            WeightChart(r.weights,date,days,r.profile.target)
            Text("只连接实际记录；未记录的日期不会补成 0。",color = Muted,style = MaterialTheme.typography.bodySmall)
            if(r.profile.target != null) Text("目标体重 ${r.profile.target.pretty()} kg",color = Orange,style = MaterialTheme.typography.bodySmall)
        } }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { onDate(date.minusDays(days.toLong()).coerceAtLeast(LocalDate.of(1900,1,1))) }) { Text("上一${if(days == 7) "周" else "月"}") }
            OutlinedButton(onClick = { onDate(minOf(date.plusDays(days.toLong()),LocalDate.now())) },enabled = date.isBefore(LocalDate.now())) { Text("下一${if(days == 7) "周" else "月"}") }
        } }
        if(points.isNotEmpty()) item { Panel(color = SoftGreen) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("期间平均",points.map { it.kg }.average().pretty(),"kg")
                Metric("最低",points.minOf { it.kg }.pretty(),"kg")
                Metric("最高",points.maxOf { it.kg }.pretty(),"kg")
            }
        } }
        item { SectionTitle("体重明细",action = "记录体重",onClick = add) }
        items(points.reversed(),key = { it.date }) { WeightCard(it,{ edit(it) },{ delete(it) }) }
        if(points.isEmpty()) item { OutlinedButton(onClick = add,modifier = Modifier.fillMaxWidth()) { Text("添加一笔体重") } }
    }
}

@Composable
fun MealCard(m: Meal, vm: RecordViewModel, edit: () -> Unit, delete: (() -> Unit)? = null) {
    var expanded by remember { mutableStateOf<String?>(null) }
    Panel {
        Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(AppIcons.Restaurant,null,Modifier.size(18.dp),tint = Pine); Text(m.category,style = MaterialTheme.typography.titleMedium); Text(m.date,color = Muted,style = MaterialTheme.typography.bodySmall) }
            RecordActions(edit,delete)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            m.photos.forEach { p -> AsyncImage(model = vm.photo(p),contentDescription = "${m.category}照片，点击查看",contentScale = ContentScale.Crop,modifier = Modifier.size(132.dp).clip(MaterialTheme.shapes.small).clickable { expanded = p }) }
        }
        if(m.isCheatMeal) Text("● 放纵餐",color = Orange,style = MaterialTheme.typography.labelMedium)
        if(m.note.isNotBlank()) Text(m.note,style = MaterialTheme.typography.bodyMedium)
    }
    if(expanded != null) PhotoViewer(vm.photo(expanded!!)) { expanded = null }
}

@Composable
fun TrainingCard(t: Training, edit: () -> Unit, delete: (() -> Unit)? = null) {
    Panel {
        Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.weight(1f),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(AppIcons.FitnessCenter,null,tint = Orange)
                Column { Text(t.name,style = MaterialTheme.typography.titleMedium); Text(t.date,color = Muted,style = MaterialTheme.typography.bodySmall) }
            }
            RecordActions(edit,delete)
        }
        Text(buildList { add("${t.minutes} 分钟"); if(t.sets > 0) add("${t.sets} 组"); if(t.reps > 0) add("${t.reps} 次/组"); if(t.load > 0) add("${t.load.pretty()} kg") }.joinToString("  ·  "),color = Pine)
        if(t.muscleGroup.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(t.muscleGroup,color = Muted,style = MaterialTheme.typography.bodySmall)
            if(t.difficulty.isNotBlank()) DifficultyBadge(t.difficulty)
        }
        if(t.note.isNotBlank()) Text(t.note,style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun WeightCard(w: Weight, edit: () -> Unit, delete: () -> Unit) {
    Panel {
        Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) { Text(w.date,color = Muted,style = MaterialTheme.typography.bodySmall); Text("${w.kg.pretty()} kg",fontFamily = NumberFont,fontSize = 26.sp) }
            RecordActions(edit,delete)
        }
        if(w.note.isNotBlank()) Text(w.note,style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RecordActions(edit: () -> Unit, delete: (() -> Unit)?) {
    var menu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menu = true }) { Icon(AppIcons.MoreHoriz,"记录操作") }
        DropdownMenu(expanded = menu,onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("编辑") },onClick = { menu = false; edit() },leadingIcon = { Icon(AppIcons.Edit,null) })
            if(delete != null) DropdownMenuItem(text = { Text("删除") },onClick = { menu = false; delete() },leadingIcon = { Icon(AppIcons.DeleteOutline,null) })
        }
    }
}

@Composable
fun RecordListScreen(r: Records, date: LocalDate, onDate: (LocalDate) -> Unit, vm: RecordViewModel, editWeight: (Weight) -> Unit, editMeal: (Meal) -> Unit, editTraining: (Training) -> Unit, delete: (String,String) -> Unit, addMeal: () -> Unit, addTraining: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf("全部") }
    var allDates by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    data class Entry(val key: String,val date: String,val value: Any)
    val entries = buildList {
        if(filter in listOf("全部","体重")) r.weights.forEach { add(Entry("w${it.date}",it.date,it)) }
        if(filter in listOf("全部","饮食")) r.meals.forEach { add(Entry("m${it.id}",it.date,it)) }
        if(filter in listOf("全部","训练")) r.trainings.forEach { add(Entry("t${it.id}",it.date,it)) }
    }.filter { (allDates || it.date == date.toString()) && when(val v = it.value) {
        is Weight -> "${v.date} ${v.kg} ${v.note}".contains(search,true)
        is Meal -> "${v.date} ${v.category} ${v.note}".contains(search,true)
        is Training -> "${v.date} ${v.name} ${v.note}".contains(search,true)
        else -> false
    } }.sortedByDescending { it.date }
    LazyColumn(Modifier.fillMaxSize(),contentPadding = PaddingValues(20.dp),verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageTitle("YOUR JOURNAL","把日子，认真收藏。") }
        item { DateNavigation(date,onDate) }
        item { Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) {
            Text("查看全部日期",style = MaterialTheme.typography.titleMedium)
            Switch(checked = allDates,onCheckedChange = { allDates = it })
        } }
        item { OutlinedTextField(value = search,onValueChange = { search = it.take(100) },modifier = Modifier.fillMaxWidth(),placeholder = { Text("搜索备注、餐次或训练名称") },leadingIcon = { Icon(AppIcons.Search,null) },singleLine = true,shape = MaterialTheme.shapes.medium) }
        item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("全部","体重","饮食","训练").forEach { f -> FilterChip(selected = filter == f,onClick = { filter = f },label = { Text(f) },modifier = Modifier.testTag("journal-filter-$f")) }
        } }
        item { Text("${if(allDates) "全部日期" else date.toString()} · ${entries.size} 条记录",color = Muted,style = MaterialTheme.typography.bodySmall) }
        if(entries.isEmpty()) item { Panel { EmptyState(AppIcons.EventNote,"还没有匹配的记录","切换日期或添加一条新的日常记录") } }
        items(entries,key = { it.key }) { entry -> when(val v = entry.value) {
            is Weight -> WeightCard(v,{ editWeight(v) },{ delete("weights",v.date) })
            is Meal -> MealCard(v,vm,{ editMeal(v) },{ delete("meals",v.id) })
            is Training -> TrainingCard(v,{ editTraining(v) },{ delete("trainings",v.id) })
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = addMeal,modifier = Modifier.weight(1f)) { Text("＋ 饮食") }
            OutlinedButton(onClick = addTraining,modifier = Modifier.weight(1f)) { Text("＋ 训练") }
        } }
    }
}

@Composable
fun SettingsScreen(r: Records, profile: () -> Unit, export: () -> Unit, restore: () -> Unit, busy: Boolean) {
    val latest = r.weights.lastOrNull()
    val bmi = r.profile.height?.let { h -> latest?.kg?.div((h/100)*(h/100)) }
    LazyColumn(Modifier.fillMaxSize().testTag("settings-list"),contentPadding = PaddingValues(20.dp),verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageTitle("A LITTLE BETTER, EVERY DAY","照顾自己的节奏。") }
        item { Panel(color = SoftGreen) {
            Icon(AppIcons.Spa,null,Modifier.size(40.dp),tint = Pine)
            Text("轻记",style = MaterialTheme.typography.headlineMedium)
            Text("体重 · 饮食 · 训练\n用小小的记录，积累长久的改变。",color = Muted)
            Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("记录天数",r.dates().size.toString(),"天")
                Metric("连续记录",Rules.streak(r.dates(),LocalDate.now()).toString(),"天")
                Metric("累计训练",r.trainings.sumOf { it.minutes }.toString(),"分钟")
            }
        } }
        item { Panel {
            SectionTitle("目标与身体信息",action = "设置",onClick = profile)
            SettingLine("目标体重",r.profile.target?.let { "${it.pretty()} kg" } ?: "未设置")
            SettingLine("身高",r.profile.height?.let { "${it.pretty()} cm" } ?: "未设置")
            SettingLine("性别",r.profile.sex.ifBlank { "未设置" })
            SettingLine("每周训练","${r.profile.weeklyGoal} 天")
            if(bmi != null) {
                SettingLine("BMI",bmi.pretty())
                Text("依据最近一次体重计算，仅作记录参考。",color = Muted,style = MaterialTheme.typography.bodySmall)
            }
        } }
        item { NutritionReferencePanel(r,profile) }
        item { Panel {
            SectionTitle("数据备份")
            Text("将记录、目标、饮食照片和自定义动作（含自选演示）导出为 ZIP 文件，方便换机恢复。",color = Muted,style = MaterialTheme.typography.bodyMedium)
            Button(onClick = export,enabled = !busy,modifier = Modifier.fillMaxWidth()) { Icon(AppIcons.FileUpload,null,Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("导出完整备份") }
            OutlinedButton(onClick = restore,enabled = !busy,modifier = Modifier.fillMaxWidth()) { Icon(AppIcons.FileDownload,null,Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("从备份恢复") }
        } }
        item { Panel {
            SectionTitle("你的记录，留在你的设备")
            Text("无需账号，离线使用。照片仅复制到应用私有目录；不会自动上传。卸载应用会删除本地数据，请定期导出备份，并妥善保管备份文件。",color = Muted,style = MaterialTheme.typography.bodyMedium)
        } }
        item { Panel {
            SectionTitle("动作演示来源")
            Text("ExerciseDB V1 / AscendAPI\noss.exercisedb.dev",color = Pine,style = MaterialTheme.typography.bodyMedium)
            Text("本版本包含非商业演示素材，仅供个人使用与测试。内置难度参考你提供的分级，未标注的动作采用补充参考等级，均可编辑。",color = Muted,style = MaterialTheme.typography.bodySmall)
        } }
        item { Text("轻记 1.3.0  ·  给健康一点耐心",color = Muted,style = MaterialTheme.typography.bodySmall,modifier = Modifier.padding(vertical = 12.dp)) }
    }
}

@Composable
private fun SettingLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) { Text(label,color = Muted); Text(value,fontWeight = FontWeight.Medium) }
}
