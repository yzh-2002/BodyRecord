package com.bodyrecord.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bodyrecord.app.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NutritionTest {
    private lateinit var context: Context
    @Before fun setup() { context = ApplicationProvider.getApplicationContext(); context.deleteDatabase("bodyrecord.db") }
    @Test fun foodsHaveTraceableIdsAndCorrectWeighedStates() {
        val foods = FoodCatalog.load(context)
        assertEquals(27,foods.size)
        assertEquals(FoodCatalog.categories.toSet(),foods.map { it.category }.toSet())
        assertTrue(foods.all { it.id > 0 && it.sourceName.isNotBlank() && it.state.isNotBlank() })
        val rice = foods.single { it.id == 169757 }
        assertEquals(28.17,rice.nutrients.carbs,.0001)
        assertEquals(2.69,rice.nutrients.protein,.0001)
        assertTrue(rice.state.contains("熟"))
        assertTrue(foods.single { it.id == 173904 }.state.contains("干"))
        assertEquals(100.0,foods.single { it.id == 171413 }.nutrients.fat,.0001)
    }
    @Test fun portionsScaleAllThreeNutrientsAndRejectInvalidInputs() {
        val rice = FoodCatalog.load(context).single { it.id == 169757 }
        assertEquals(MacroAmount(56.34,5.38,.56),rice.portion(200.0))
        assertThrows(IllegalArgumentException::class.java) { rice.portion(Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { rice.portion(0.0) }
        assertThrows(IllegalArgumentException::class.java) { rice.portion(5001.0) }
    }
    @Test fun standardTrainingRangesMatchProvidedImage() {
        val male = ImageIntakeReference.coefficients("男","力量训练日")
        assertEquals(IntakeReference(IntakeRange(3.0,3.5),IntakeRange(1.5,2.0),IntakeRange(.8)),male)
        val female = ImageIntakeReference.coefficients("女","力量训练日")
        assertEquals(IntakeReference(IntakeRange(2.5,3.0),IntakeRange(1.5),IntakeRange(.8)),female)
        val grams = ImageIntakeReference.grams(80.0,male)
        assertEquals(IntakeReference(IntakeRange(240.0,280.0),IntakeRange(120.0,160.0),IntakeRange(64.0)),grams)
    }
    @Test fun restAndNonGymRemainDistinctImageScenarios() {
        val rest = ImageIntakeReference.coefficients("男","休息 / 有氧日")
        val nonGym = ImageIntakeReference.coefficients("男","不健身")
        assertEquals(IntakeRange(1.5,2.0),rest.carbs)
        assertEquals(IntakeRange(1.5,2.0),rest.protein)
        assertEquals(IntakeRange(2.0,2.5),nonGym.carbs)
        assertEquals(IntakeRange(1.2),nonGym.protein)
    }
    @Test fun explicitSpecialBandsMatchImageWithoutInferringBmi() {
        val over28 = ImageIntakeReference.coefficients("女","力量训练日","图示 BMI > 28")
        assertEquals(IntakeReference(IntakeRange(2.1),IntakeRange(1.2),IntakeRange(.6)),over28)
        val over32 = ImageIntakeReference.coefficients("男","不健身","图示 BMI > 32")
        assertEquals(IntakeReference(IntakeRange(2.0),IntakeRange(.8),IntakeRange(.5)),over32)
        assertEquals(IntakeRange(3.0,3.5),ImageIntakeReference.coefficients("男","力量训练日").carbs)
        assertThrows(IllegalArgumentException::class.java) { ImageIntakeReference.coefficients("","力量训练日") }
        assertThrows(IllegalArgumentException::class.java) { ImageIntakeReference.grams(Double.NaN,over28) }
    }
    @Test fun sexPersistsAndBackupsV1V2RemainCompatible() {
        val profile = Profile(70.0,175.0,3,"女")
        RecordDatabase(context).use { db -> db.profile(profile); assertEquals(profile,db.snapshot().profile) }
        val records = Records(profile = profile)
        assertEquals(records,BackupCodec.decode(BackupCodec.encode(records)))
        for(version in 1..2) {
            val old = JSONObject(BackupCodec.encode(records)); old.put("version",version); old.getJSONObject("profile").remove("sex")
            assertEquals(profile.copy(sex = ""),BackupCodec.decode(old.toString()).profile)
        }
        val invalid = JSONObject(BackupCodec.encode(records)); invalid.getJSONObject("profile").put("sex","invalid")
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(invalid.toString()) }
    }
    @Test fun versionTwoMigrationPreservesProfileAndTraining() {
        val profile = Profile(70.0,175.0,4)
        val training = Training(date = "2026-01-01",name = "卧推",minutes = 30)
        RecordDatabase(context).use { db -> db.profile(profile); db.training(training) }
        context.openOrCreateDatabase("bodyrecord.db",0,null).use { old ->
            old.execSQL("ALTER TABLE profile RENAME TO profile_with_sex")
            old.execSQL("CREATE TABLE profile(id INTEGER PRIMARY KEY CHECK(id=1),target REAL,height REAL,weeklyGoal INTEGER NOT NULL)")
            old.execSQL("INSERT INTO profile SELECT id,target,height,weeklyGoal FROM profile_with_sex")
            old.execSQL("DROP TABLE profile_with_sex")
            old.execSQL("ALTER TABLE meals RENAME TO meals_v4")
            old.execSQL("CREATE TABLE meals(id TEXT PRIMARY KEY,date TEXT NOT NULL,category TEXT NOT NULL,photos TEXT NOT NULL,note TEXT NOT NULL)")
            old.execSQL("INSERT INTO meals SELECT id,date,category,photos,note FROM meals_v4")
            old.execSQL("DROP TABLE meals_v4")
            old.execSQL("DROP TABLE days")
            old.execSQL("DROP TABLE foods")
            old.version = 2
        }
        RecordDatabase(context).use { db ->
            assertEquals(profile,db.snapshot().profile)
            assertEquals(training,db.snapshot().trainings.single())
            assertEquals(4,db.readableDatabase.version)
        }
    }
}
