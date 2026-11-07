package com.bodyrecord.app

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.bodyrecord.app.data.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JournalFoodTest {
    private lateinit var context: Context
    private val date = "2026-01-02"
    @Before fun setup() { context = ApplicationProvider.getApplicationContext(); context.deleteDatabase("bodyrecord.db") }
    @Test fun calendarHasCorrectWeekdaysLeapDaysAndYearBoundaries() {
        for(m in listOf(YearMonth.of(2024,2),YearMonth.of(2026,11),YearMonth.of(2027,1),YearMonth.of(2021,2))) {
            val cells = Journal.monthCells(m)
            assertEquals(0,cells.size % 7)
            assertEquals((1..m.lengthOfMonth()).map(m::atDay),cells.filterNotNull())
            assertEquals(m.atDay(1).dayOfWeek.value - 1,cells.indexOf(m.atDay(1)))
        }
        assertTrue(Journal.monthCells(YearMonth.of(2024,2)).contains(LocalDate.of(2024,2,29)))
    }
    @Test fun summaryDistinguishesMissingRestTrainingGroupsAndCheatMeals() {
        assertEquals(DaySummary(false,false,false,emptyList(),false),Journal.summary(Records(),date))
        val r = Records(days = listOf(DayAnnotation(date,"休息日")),trainings = listOf(
            Training(date = date,name = "划船",minutes = 20,muscleGroup = "背"),
            Training(date = date,name = "下拉",minutes = 20,muscleGroup = "背"),
            Training(date = date,name = "弯举",minutes = 10,muscleGroup = "肱二头")),
            meals = listOf(Meal(date = date,category = "午餐",photos = listOf("meal.jpg"),isCheatMeal = true)))
        assertEquals(DaySummary(true,false,true,listOf("背","肱二头"),true),Journal.summary(r,date))
        assertTrue(Journal.summary(r.copy(trainings = emptyList()),date).rest)
    }
    @Test fun foodOverridesHideEditRestoreAndKeepBuiltinDataIntact() {
        val builtin = FoodCatalog.load(context)
        val rice = builtin.first()
        val edited = rice.copy(name = "常吃的米饭",hidden = true)
        val custom = rice.copy(id = -1,name = "自制便当")
        assertEquals(26,FoodCatalog.merge(builtin,listOf(edited)).size)
        assertEquals(edited,FoodCatalog.merge(builtin,listOf(edited),true).first { it.id == rice.id })
        assertEquals(28,FoodCatalog.merge(builtin,listOf(custom)).size)
        assertEquals(-2,FoodCatalog.nextCustomId(listOf(custom)))
        assertEquals(rice,FoodCatalog.merge(builtin,emptyList()).first { it.id == rice.id })
    }
    @Test fun daysFoodsAndCheatFlagPersistAndRoundTripBackup() {
        val f = FoodCatalog.load(context).first().copy(id = -1,name = "自制主食")
        val day = DayAnnotation(date,"休息日",true,"朋友聚餐")
        val meal = Meal(date = date,category = "午餐",photos = listOf("meal.jpg"),isCheatMeal = true)
        RecordDatabase(context).use { db -> db.food(f); db.day(day); db.meal(meal) }
        val r = RecordDatabase(context).use { it.snapshot() }
        assertEquals(listOf(day),r.days); assertEquals(listOf(f),r.foods); assertTrue(r.meals.single().isCheatMeal)
        assertEquals(r,BackupCodec.decode(BackupCodec.encode(r)))
        RecordDatabase(context).use { db -> db.replace(r); assertEquals(r,db.snapshot()) }
    }
    @Test fun oldBackupsDefaultToNoAnnotationsOrFoodEditsAndInvalidNewDataIsRejected() {
        val j = JSONObject(BackupCodec.encode(Records()))
        j.put("version",3); j.remove("days"); j.remove("foods")
        assertEquals(Records(),BackupCodec.decode(j.toString()))
        val invalid = Records(days = listOf(DayAnnotation("2026-02-30","休息日")))
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(BackupCodec.encode(invalid)) }
        val f = FoodCatalog.load(context).first().copy(nutrients = MacroAmount(101.0,2.0,1.0))
        assertFalse(FoodCodec.valid(f))
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(BackupCodec.encode(Records(foods = listOf(f)))) }
    }
    @Test fun versionThreeMigrationPreservesExistingRecordsAndDefaultsMealFlag() {
        val meal = Meal(date = date,category = "晚餐",photos = listOf("old.jpg"))
        val training = Training(date = date,name = "划船",minutes = 20,muscleGroup = "背")
        val profile = Profile(sex = "女")
        RecordDatabase(context).use { db -> db.meal(meal); db.training(training); db.profile(profile) }
        context.openOrCreateDatabase("bodyrecord.db",0,null).use { old ->
            old.execSQL("ALTER TABLE meals RENAME TO meals_v4")
            old.execSQL("CREATE TABLE meals(id TEXT PRIMARY KEY,date TEXT NOT NULL,category TEXT NOT NULL,photos TEXT NOT NULL,note TEXT NOT NULL)")
            old.execSQL("INSERT INTO meals SELECT id,date,category,photos,note FROM meals_v4")
            old.execSQL("DROP TABLE meals_v4"); old.execSQL("DROP TABLE days"); old.execSQL("DROP TABLE foods"); old.version = 3
        }
        RecordDatabase(context).use { db ->
            val r = db.snapshot()
            assertEquals(4,db.readableDatabase.version); assertEquals(meal,r.meals.single())
            assertEquals(training,r.trainings.single()); assertEquals(profile,r.profile)
            assertTrue(r.days.isEmpty()); assertTrue(r.foods.isEmpty())
        }
    }
    @Test fun repositoryHandlesRestToTrainingAndZipRestoresFoodsAndDayNotes() = runBlocking {
        val repo = RecordRepository(context)
        val rest = DayAnnotation(date,"休息日",true,"聚餐")
        val f = FoodCatalog.load(context).first().copy(hidden = true)
        repo.saveDay(rest); repo.saveFood(f)
        val zip = File(context.cacheDir,"calendar-food.zip")
        repo.export(Uri.fromFile(zip))
        repo.delete("days",date); repo.delete("foods",f.id.toString())
        repo.restore(Uri.fromFile(zip))
        assertEquals(rest,repo.load().days.single()); assertEquals(f,repo.load().foods.single())
        repo.save(Training(date = date,name = "卧推",minutes = 20,muscleGroup = "胸"))
        assertEquals(rest.copy(type = "训练日"),repo.load().days.single())
        assertTrue(runCatching { repo.saveDay(rest) }.isFailure)
        assertTrue(Journal.summary(repo.load(),date).training)
    }
}
