package com.bodyrecord.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.bodyrecord.app.data.*
import com.bodyrecord.app.ui.BodyTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35],qualifiers = "zh-rCN-w411dp-h891dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var application: Application
    @Before fun setup() {
        application = ApplicationProvider.getApplicationContext()
        application.deleteDatabase("bodyrecord.db")
    }
    private fun launch() {
        val vm = RecordViewModel(application)
        compose.setContent { BodyTheme { BodyRecordApp(vm) } }
        compose.waitUntil(15000) { compose.onAllNodesWithText("每一小步，都算数。").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun addWeightPersistsAndTrendAndSettingsNavigate() {
        launch()
        compose.onNodeWithText("记体重").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("72.5")
        compose.onNodeWithText("保存记录").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("体重已保存").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("72.5").assertExists()
        compose.onNodeWithText("趋势").performClick()
        compose.onNodeWithText("看见每一点变化").assertExists()
        compose.onNodeWithText("月 · 30 天").performClick()
        compose.onNodeWithText("我的").performClick()
        compose.onNodeWithText("照顾自己的节奏。").assertExists()
        val db = RecordDatabase(application)
        org.junit.Assert.assertEquals(72.5,db.snapshot().weights.single().kg,0.001)
        db.close()
    }
    @Test fun invalidWeightShowsErrorWithoutSaving() {
        launch()
        compose.onNodeWithText("记体重").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("0")
        compose.onNodeWithText("保存记录").performClick()
        compose.onNodeWithText("请输入 20–500 kg 之间的有效体重").assertExists()
        org.junit.Assert.assertTrue(RecordDatabase(application).use { it.snapshot().weights.isEmpty() })
    }
    @Test fun saveTrainingAndFindItInHistory() {
        launch()
        compose.onNodeWithText("＋ 记录训练").performClick()
        compose.onNodeWithText("跑步").performClick()
        compose.onNodeWithTag("training-minutes").performTextInput("30")
        compose.onNodeWithText("保存记录").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("训练已保存").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("记录").performClick()
        compose.onNodeWithTag("calendar-day-${LocalDate.now()}").performClick()
        compose.onNodeWithTag("journal-filter-训练").performClick()
        compose.onNodeWithText("跑步").assertExists()
        val saved = RecordDatabase(application).use { it.snapshot().trainings.single() }
        org.junit.Assert.assertEquals("跑步",saved.name)
        org.junit.Assert.assertEquals(30,saved.minutes)
    }
    @Test fun captureHomeAndTrends() {
        val today = LocalDate.now()
        RecordDatabase(application).use { db ->
            listOf(74.8,74.5,74.7,74.2,74.0,74.3,73.9).forEachIndexed { i,kg -> db.weight(Weight(today.minusDays((6-i).toLong()).toString(),kg)) }
            db.profile(Profile(70.0,175.0,3))
            db.training(Training(date = today.toString(),name = "力量训练",minutes = 45,sets = 4,reps = 12,load = 20.0))
        }
        launch()
        screenshot("home")
        compose.onNodeWithText("趋势").performClick()
        compose.waitForIdle()
        screenshot("trends")
        compose.onNodeWithText("我的").performClick()
        compose.waitForIdle()
        screenshot("settings")
    }
    @Test fun planSelectionPrefillsExerciseDifficultyAndSets() {
        launch()
        compose.onNodeWithText("训练").performClick()
        compose.onNodeWithTag("exercise-tab-1").performClick()
        compose.onAllNodesWithText("选动作")[0].performClick()
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-builtin_machine_pulldown"))
        compose.onNodeWithTag("exercise-builtin_machine_pulldown").performClick()
        compose.onNodeWithText("选择此动作并记录").performClick()
        compose.onNodeWithTag("training-minutes").performTextInput("30")
        compose.onNodeWithText("保存记录").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("训练已保存").fetchSemanticsNodes().isNotEmpty() }
        val saved = RecordDatabase(application).use { it.snapshot().trainings.single() }
        org.junit.Assert.assertEquals("器械下拉",saved.name)
        org.junit.Assert.assertEquals("背",saved.muscleGroup)
        org.junit.Assert.assertEquals("容易",saved.difficulty)
        org.junit.Assert.assertEquals(3,saved.sets)
    }
    @Test fun customExerciseCanBeCreatedWithDifficulty() {
        launch()
        compose.onNodeWithText("训练").performClick()
        compose.onNodeWithContentDescription("新建动作").performClick()
        compose.onNodeWithTag("exercise-editor-name").performTextInput("自定义划船")
        compose.onNodeWithTag("editor-difficulty-困难").performClick()
        compose.onNodeWithText("保存动作").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("动作已保存").fetchSemanticsNodes().isNotEmpty() }
        val saved = RecordDatabase(application).use { it.snapshot().exercises.single() }
        org.junit.Assert.assertEquals("自定义划船",saved.name)
        org.junit.Assert.assertEquals("困难",saved.difficulty)
    }
    @Test fun captureExerciseLibraryDetailAndPlan() {
        launch()
        compose.onNodeWithText("训练").performClick()
        screenshot("exercise-library")
        compose.onNodeWithTag("exercise-search").performTextInput("杠铃卧推")
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-builtin_barbell_bench"))
        compose.onNodeWithTag("exercise-builtin_barbell_bench").performClick()
        compose.onNodeWithText("动作详情").assertExists()
        screenshot("exercise-detail")
        compose.onNodeWithContentDescription("关闭动作详情").performClick()
        compose.onNodeWithTag("exercise-list").performScrollToIndex(0)
        compose.onNodeWithTag("exercise-tab-1").performClick()
        screenshot("exercise-plans")
    }
    @Test fun foodCategoriesAndPortionsWorkOffline() {
        launch()
        compose.onNodeWithText("饮食").performClick()
        compose.onNodeWithTag("food-list").performScrollToNode(hasTestTag("food-169757"))
        compose.onNodeWithTag("food-169757").performClick()
        compose.onNodeWithText("食物详情").assertExists()
        compose.onNodeWithTag("food-portion").performTextReplacement("200")
        compose.onNodeWithContentDescription("关闭食物详情").performClick()
        compose.onNodeWithTag("food-list").performScrollToIndex(0)
        compose.onNodeWithTag("food-category-蛋白质").performClick()
        compose.onNodeWithTag("food-list").performScrollToNode(hasTestTag("food-171477"))
        compose.onNodeWithTag("food-171477").assertExists()
        compose.onNodeWithTag("food-list").performScrollToIndex(0)
        compose.onNodeWithTag("food-category-脂肪").performClick()
        compose.onNodeWithTag("food-list").performScrollToNode(hasTestTag("food-171413"))
        compose.onNodeWithTag("food-171413").assertExists()
    }
    @Test fun imageReferenceShowsLatestWeightRangesWithoutCreatingGoals() {
        RecordDatabase(application).use { db -> db.profile(Profile(sex = "男")); db.weight(Weight(LocalDate.now().toString(),80.0)) }
        launch()
        compose.onNodeWithText("我的").performClick()
        compose.onNodeWithTag("settings-list").performScrollToNode(hasText("图示摄入参考"))
        compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("intake-value-碳水"))
        compose.onNodeWithText("240.0–280.0 g/天").assertExists()
        val profile = RecordDatabase(application).use { it.snapshot().profile }
        org.junit.Assert.assertEquals(Profile(sex = "男"),profile)
        screenshot("intake-reference")
    }
    @Test fun calendarCanMarkRestAndCheatAndReopenDayDetails() {
        launch()
        compose.onNodeWithText("记录").performClick()
        compose.onNodeWithTag("calendar-day-${LocalDate.now()}").performClick()
        compose.onNodeWithText("编辑标记").performClick()
        compose.onNodeWithTag("day-type-休息日").performClick()
        compose.onNodeWithTag("day-cheat").performClick()
        compose.onNodeWithTag("day-note").performTextInput("聚餐休息")
        compose.onNodeWithText("保存标记").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("日历标记已保存").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("● 休息日").assertExists()
        compose.onNodeWithText("● 当天含放纵餐").assertExists()
        compose.onNodeWithText("返回月历").performClick()
        compose.onNodeWithTag("calendar-day-${LocalDate.now()}").assertContentDescriptionContains("休息日",substring = true)
        screenshot("journal-calendar")
        compose.onNodeWithTag("calendar-day-${LocalDate.now()}").performClick()
        compose.onNodeWithText("聚餐休息").assertExists()
        screenshot("journal-day")
    }
    @Test fun builtinFoodCanBeHiddenAndRecovered() {
        launch()
        compose.onNodeWithText("饮食").performClick()
        compose.onNodeWithTag("food-list").performScrollToNode(hasTestTag("food-169757"))
        compose.onNodeWithTag("food-169757").performClick()
        compose.onNodeWithTag("food-remove").performClick()
        compose.onNodeWithText("隐藏",useUnmergedTree = true).performClick()
        compose.waitUntil(15000) { RecordDatabase(application).use { it.snapshot().foods.any { f -> f.id == 169757 && f.hidden } } }
        compose.onNodeWithTag("food-list").performScrollToNode(hasTestTag("food-show-hidden"))
        compose.onNodeWithTag("food-show-hidden").performClick()
        compose.onNodeWithTag("food-list").performScrollToNode(hasTestTag("food-169757"))
        compose.onNodeWithTag("food-169757").performClick()
        compose.onNodeWithTag("food-remove").performClick()
        compose.waitUntil(15000) { RecordDatabase(application).use { it.snapshot().foods.any { f -> f.id == 169757 && !f.hidden } } }
    }
    @Test fun customFoodCanBeAddedEditedAndDeleted() {
        launch()
        compose.onNodeWithText("饮食").performClick()
        compose.onNodeWithTag("food-list").performScrollToNode(hasTestTag("food-add"))
        compose.onNodeWithTag("food-add").performClick()
        compose.onNodeWithTag("food-editor-name").performTextInput("自制燕麦")
        compose.onNodeWithTag("food-editor-state").performTextInput("包装成品")
        compose.onNodeWithTag("food-editor-carbs").performScrollTo().performTextInput("60.25")
        compose.onNodeWithTag("food-editor-protein").performScrollTo().performTextInput("12")
        compose.onNodeWithTag("food-editor-fat").performScrollTo().performTextInput("6")
        compose.onNodeWithText("保存食物").performClick()
        compose.waitUntil(15000) { RecordDatabase(application).use { it.snapshot().foods.any { f -> f.custom } } }
        compose.onNodeWithTag("food-list").performScrollToNode(hasTestTag("food--1"))
        compose.onNodeWithTag("food--1").performClick()
        compose.onNodeWithTag("food-edit").performClick()
        compose.onNodeWithTag("food-editor-name").performTextReplacement("常吃的燕麦")
        compose.onNodeWithText("保存食物").performClick()
        compose.waitUntil(15000) { RecordDatabase(application).use { it.snapshot().foods.any { f -> f.name == "常吃的燕麦" } } }
        org.junit.Assert.assertEquals(60.25,RecordDatabase(application).use { it.snapshot().foods.single().nutrients.carbs },.0001)
        compose.onNodeWithTag("food-list").performScrollToNode(hasTestTag("food--1"))
        compose.onNodeWithTag("food--1").performClick()
        compose.onNodeWithTag("food-remove").performClick()
        compose.onNodeWithText("删除",useUnmergedTree = true).performClick()
        compose.waitUntil(15000) { RecordDatabase(application).use { it.snapshot().foods.isEmpty() } }
    }
    private fun screenshot(name: String) {
        val out = File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            out.outputStream().buffered().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG,100,stream) }
            bitmap.recycle()
        }
    }
}
