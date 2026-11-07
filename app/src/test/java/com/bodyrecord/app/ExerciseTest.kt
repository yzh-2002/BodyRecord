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
class ExerciseTest {
    private lateinit var context: Context
    @Before fun setup() { context = ApplicationProvider.getApplicationContext(); context.deleteDatabase("bodyrecord.db") }
    @Test fun catalogCoversEveryExplicitPlanActionAndDifficulty() {
        val all = ExerciseCatalog.load(context)
        assertEquals(70,all.size)
        assertEquals(ExerciseCatalog.groups.toSet(),all.map { it.group }.toSet())
        val expected = mapOf(
            "器械下拉" to "容易","器械划船" to "容易","坐姿钢线划船" to "容易","高位下拉" to "一般","引体向上" to "一般",
            "半程硬拉" to "困难","杠铃划船" to "困难","哑铃划船" to "困难","T杠划船" to "困难",
            "器械推胸" to "容易","蝴蝶机夹胸" to "容易","龙门架夹胸" to "容易","杠铃卧推" to "一般","哑铃卧推" to "一般","史密斯卧推" to "一般","双杠臂屈伸" to "困难",
            "器械推举" to "容易","史密斯推举" to "容易","哑铃推举" to "一般","杠铃推举" to "一般",
            "蝴蝶机反向飞鸟" to "容易","哑铃俯身飞鸟" to "一般","龙门架反向飞鸟" to "一般","龙门架绳索面拉" to "困难",
            "杠铃弯举" to "容易","哑铃弯举" to "容易","龙门架弯举" to "容易","集中弯举" to "容易","器械弯举" to "容易","牧师椅弯举" to "容易",
            "窄距卧推" to "困难","杠铃仰卧臂屈伸" to "困难","哑铃颈后臂屈伸" to "困难","龙门架绳索臂屈伸" to "一般","绳索过顶臂屈伸" to "一般","直杆下压" to "一般",
            "器械腿弯举" to "容易","器械腿屈伸" to "容易","哈克机" to "一般","倒蹬机" to "一般","自由深蹲" to "困难","箭步蹲" to "困难","直腿硬拉" to "困难"
        )
        expected.forEach { (name,level) -> assertEquals(name,level,all.single { it.name == name }.difficulty) }
        listOf("前平举","侧平举","提拉").forEach { type -> assertTrue(all.filter { it.pattern == type }.all { it.difficulty == "困难" }) }
        assertTrue(all.all { it.description.isNotBlank() && it.steps.isNotEmpty() && it.tips.isNotBlank() })
        all.filter { it.demoAsset.isNotEmpty() }.forEach { e ->
            context.assets.open("exercise-media/${e.demoAsset}").use { input ->
                val header = ByteArray(6); assertEquals(6,input.read(header)); assertTrue(String(header).startsWith("GIF8"))
            }
        }
    }
    @Test fun splitPlansMatchSuppliedGrouping() {
        val plans = TrainingPlans.plans
        assertEquals(listOf(3,4,4),plans.map { it.days.size })
        assertEquals(listOf("背","肩后束","肱二头"),plans[0].days[0].blocks.map { it.group })
        assertEquals(listOf("胸","肩前中束","肱三头"),plans[0].days[1].blocks.map { it.group })
        assertEquals(listOf("肱二头","肱三头"),plans[1].days[2].blocks.map { it.group })
        assertEquals(listOf("肩前中束","肩后束"),plans[2].days[2].blocks.map { it.group })
    }
    @Test fun overridesHideAndRestoreWithoutDuplicatingBuiltin() {
        val builtin = ExerciseCatalog.load(context)
        val first = builtin.first()
        val custom = first.copy(id = "custom-test",name = "我的下拉",group = "自选肌群")
        val edited = first.copy(difficulty = "困难")
        val merged = ExerciseCatalog.merge(builtin,listOf(edited,custom))
        assertEquals(71,merged.size); assertEquals("困难",merged.single { it.id == first.id }.difficulty)
        assertEquals(69,ExerciseCatalog.merge(builtin,listOf(first.copy(hidden = true))).size)
        assertEquals(70,ExerciseCatalog.merge(builtin,listOf(first.copy(hidden = true)),true).size)
    }
    @Test fun customExerciseAndTrainingSnapshotRoundTrip() {
        val e = ExerciseCatalog.load(context).first().copy(id = "custom-test",name = "我的动作")
        val t = Training(date = "2026-01-01",name = e.name,minutes = 20,exerciseId = e.id,muscleGroup = e.group,difficulty = e.difficulty)
        RecordDatabase(context).use { db ->
            db.exercise(e); db.training(t)
            val original = db.snapshot(); assertEquals(original,BackupCodec.decode(BackupCodec.encode(original)))
            db.delete("exercises",e.id)
            assertEquals(t,db.snapshot().trainings.single())
        }
    }
    @Test fun oldVersionBackupStillLoads() {
        val original = Records(trainings = listOf(Training(date = "2026-01-01",name = "跑步",minutes = 30)))
        val json = JSONObject(BackupCodec.encode(original)); json.put("version",1); json.remove("exercises")
        val training = json.getJSONArray("trainings").getJSONObject(0)
        listOf("exerciseId","muscleGroup","difficulty").forEach { training.remove(it) }
        assertEquals(original,BackupCodec.decode(json.toString()))
    }
    @Test fun upgradeFromV1PreservesExistingRecords() {
        context.openOrCreateDatabase("bodyrecord.db",0,null).use { old ->
            old.execSQL("CREATE TABLE weights(date TEXT PRIMARY KEY,kg REAL NOT NULL,note TEXT NOT NULL)")
            old.execSQL("CREATE TABLE meals(id TEXT PRIMARY KEY,date TEXT,category TEXT,photos TEXT,note TEXT)")
            old.execSQL("CREATE TABLE trainings(id TEXT PRIMARY KEY,date TEXT,name TEXT,minutes INTEGER,sets INTEGER,reps INTEGER,load REAL,note TEXT)")
            old.execSQL("CREATE TABLE profile(id INTEGER PRIMARY KEY,target REAL,height REAL,weeklyGoal INTEGER)")
            old.execSQL("INSERT INTO weights VALUES ('2026-01-01',72.5,'原记录')")
            old.execSQL("INSERT INTO trainings VALUES ('old-training','2026-01-01','卧推',30,3,10,20.0,'原训练')")
            old.version = 1
        }
        RecordDatabase(context).use { db ->
            val r = db.snapshot(); assertEquals(Weight("2026-01-01",72.5,"原记录"),r.weights.single())
            val t = r.trainings.single(); assertEquals("卧推",t.name); assertEquals("",t.muscleGroup); assertNull(t.exerciseId)
            assertTrue(r.exercises.isEmpty()); assertEquals(4,db.readableDatabase.version)
        }
    }
}
