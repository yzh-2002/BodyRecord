package com.bodyrecord.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.bodyrecord.app.ui.AppIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bodyrecord.app.data.*
import com.bodyrecord.app.ui.*
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(0xFFF6F7F2.toInt(),0xFFF6F7F2.toInt())
        )
        setContent { BodyTheme { BodyRecordApp() } }
    }
}

@Composable
fun BodyRecordApp(vm: RecordViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableIntStateOf(0) }
    var day by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val date = LocalDate.parse(day)
    var editor by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var exerciseSeed by rememberSaveable { mutableStateOf<String?>(null) }
    var suggestedSets by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf<Pair<String,String>?>(null) }
    var restoreWarning by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { if(it != null) vm.export(it) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { if(it != null) vm.restore(it) }
    val records = state.records
    BackHandler(enabled = page != 0 && editor == null && !restoreWarning && confirmDelete == null) { page = 0 }
    fun open(type: String, id: String? = null) { editor = type; editingId = id; exerciseSeed = null; suggestedSets = null }
    val close = { editor = null; editingId = null; exerciseSeed = null; suggestedSets = null }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    val tabs = listOf("今日","趋势","训练","饮食","记录","我的")
    val icons = listOf(AppIcons.GridView,AppIcons.ShowChart,AppIcons.FitnessCenter,AppIcons.Restaurant,AppIcons.EventNote,AppIcons.PersonOutline)
    Scaffold(containerColor = Paper,snackbarHost = { SnackbarHost(snackbar) },bottomBar = {
        NavigationBar(containerColor = Paper,tonalElevation = 0.dp) {
            tabs.forEachIndexed { index, title -> NavigationBarItem(selected = page == index,onClick = { page = index },icon = { Icon(icons[index],title) },label = { Text(title) },colors = NavigationBarItemDefaults.colors(indicatorColor = Lime)) }
        }
    }) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            if(state.loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
            else when(page) {
                0 -> HomeScreen(records,date,{ day = it.toString() },{ open("weight") },{ open("meal") },{ open("training") },{ page = 1 },{ page = 4 },vm,
                    { open("meal",it.id) },{ open("training",it.id) })
                1 -> TrendsScreen(records,date,{ day = it.toString() },{ open("weight") },{ w -> open("weight",w.date) },{ w -> confirmDelete = "weights" to w.date })
                2 -> ExerciseLibraryScreen(records,vm,{ e,sets -> open("training"); exerciseSeed = e.id; suggestedSets = sets })
                3 -> FoodScreen(records,vm,{ open("meal") })
                4 -> JournalScreen(records,date,{ day = it.toString() },vm,{ w -> open("weight",w.date) },{ m -> open("meal",m.id) },{ t -> open("training",t.id) },{ table,id -> confirmDelete = table to id },{ open("meal") },{ open("training") },{ open("weight") })
                5 -> SettingsScreen(records,{ open("profile") },{ exporter.launch("轻记备份-${LocalDate.now()}.zip") },{ restoreWarning = true },state.busy)
            }
            if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
    when(editor) {
        "weight" -> {
            val editDate = editingId ?: day
            WeightEditor(editDate,records.weights.find { it.date == editDate },state.busy,vm,close)
        }
        "meal" -> {
            val meal = records.meals.find { it.id == editingId }
            MealEditor(meal?.date ?: day,meal,state.busy,vm,close)
        }
        "training" -> {
            val training = records.trainings.find { it.id == editingId }
            TrainingEditor(training?.date ?: day,training,state.busy,vm,close,vm.exercises(records,true).find { it.id == exerciseSeed },suggestedSets)
        }
        "profile" -> ProfileEditor(records.profile,state.busy,vm,close)
    }
    if(confirmDelete != null) AlertDialog(onDismissRequest = { confirmDelete = null },title = { Text("删除这条记录？") },text = { Text(if(confirmDelete?.first == "meals") "删除后无法撤销，关联的饮食照片也会移除。" else "删除后无法撤销。") },
        confirmButton = { TextButton(enabled = !state.busy,onClick = { confirmDelete?.let { vm.delete(it.first,it.second) }; confirmDelete = null }) { Text("删除",color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } })
    if(restoreWarning) AlertDialog(onDismissRequest = { restoreWarning = false },title = { Text("从备份恢复") },text = { Text("恢复会替换当前记录、日历标记、食物库修改、目标设置、自定义动作及其演示。建议先导出当前备份，再选择轻记导出的 ZIP 文件（最多 300 MB）。") },
        confirmButton = { TextButton(onClick = { restoreWarning = false; importer.launch(arrayOf("application/zip","application/x-zip-compressed","application/octet-stream")) },enabled = !state.busy) { Text("选择备份") } },
        dismissButton = { TextButton(onClick = { restoreWarning = false }) { Text("取消") } })
}
