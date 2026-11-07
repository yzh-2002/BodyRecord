package com.bodyrecord.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import org.json.JSONArray

/** All schema changes belong in onUpgrade; UI code never accesses SQLite directly. */
class RecordDatabase(context: Context) : SQLiteOpenHelper(context, "bodyrecord.db", null, 4) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE weights(date TEXT PRIMARY KEY, kg REAL NOT NULL, note TEXT NOT NULL)")
        db.execSQL("CREATE TABLE meals(id TEXT PRIMARY KEY, date TEXT NOT NULL, category TEXT NOT NULL, photos TEXT NOT NULL, note TEXT NOT NULL, isCheatMeal INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE INDEX meals_date ON meals(date)")
        db.execSQL("CREATE TABLE trainings(id TEXT PRIMARY KEY, date TEXT NOT NULL, name TEXT NOT NULL, minutes INTEGER NOT NULL, sets INTEGER NOT NULL, reps INTEGER NOT NULL, load REAL NOT NULL, note TEXT NOT NULL, exerciseId TEXT, muscleGroup TEXT NOT NULL DEFAULT '', difficulty TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE INDEX trainings_date ON trainings(date)")
        db.execSQL("CREATE TABLE profile(id INTEGER PRIMARY KEY CHECK(id=1), target REAL, height REAL, weeklyGoal INTEGER NOT NULL, sex TEXT NOT NULL DEFAULT '')")
        createExercises(db)
        createJournalAndFoods(db)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if(oldVersion < 2) {
            db.execSQL("ALTER TABLE trainings ADD COLUMN exerciseId TEXT")
            db.execSQL("ALTER TABLE trainings ADD COLUMN muscleGroup TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE trainings ADD COLUMN difficulty TEXT NOT NULL DEFAULT ''")
            createExercises(db)
        }
        if(oldVersion < 3) db.execSQL("ALTER TABLE profile ADD COLUMN sex TEXT NOT NULL DEFAULT ''")
        if(oldVersion < 4) {
            db.execSQL("ALTER TABLE meals ADD COLUMN isCheatMeal INTEGER NOT NULL DEFAULT 0")
            createJournalAndFoods(db)
        }
    }
    private fun createJournalAndFoods(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE days(date TEXT PRIMARY KEY, type TEXT NOT NULL, cheat INTEGER NOT NULL, note TEXT NOT NULL)")
        db.execSQL("CREATE TABLE foods(id INTEGER PRIMARY KEY, payload TEXT NOT NULL)")
    }
    private fun createExercises(db: SQLiteDatabase) { db.execSQL("CREATE TABLE exercises(id TEXT PRIMARY KEY, payload TEXT NOT NULL)") }
    fun snapshot(): Records {
        val db = readableDatabase
        val weights = db.rawQuery("SELECT date,kg,note FROM weights ORDER BY date", null).use { c ->
            buildList { while (c.moveToNext()) add(Weight(c.getString(0), c.getDouble(1), c.getString(2))) }
        }
        val meals = db.rawQuery("SELECT id,date,category,photos,note,isCheatMeal FROM meals ORDER BY date DESC,rowid DESC", null).use { c ->
            buildList { while (c.moveToNext()) {
                val photos = JSONArray(c.getString(3))
                add(Meal(c.getString(0), c.getString(1), c.getString(2), List(photos.length()) { photos.getString(it) }, c.getString(4), c.getInt(5) != 0))
            } }
        }
        val trainings = db.rawQuery("SELECT id,date,name,minutes,sets,reps,load,note,exerciseId,muscleGroup,difficulty FROM trainings ORDER BY date DESC,rowid DESC", null).use { c ->
            buildList { while (c.moveToNext()) add(Training(c.getString(0),c.getString(1),c.getString(2),c.getInt(3),c.getInt(4),c.getInt(5),c.getDouble(6),c.getString(7),if(c.isNull(8)) null else c.getString(8),c.getString(9),c.getString(10))) }
        }
        val profile = db.rawQuery("SELECT target,height,weeklyGoal,sex FROM profile WHERE id=1",null).use { c ->
            if (c.moveToFirst()) Profile(if(c.isNull(0)) null else c.getDouble(0),if(c.isNull(1)) null else c.getDouble(1),c.getInt(2),c.getString(3)) else Profile()
        }
        val exercises = db.rawQuery("SELECT payload FROM exercises ORDER BY rowid",null).use { c ->
            buildList { while(c.moveToNext()) add(ExerciseCodec.decode(org.json.JSONObject(c.getString(0)))) }
        }
        val days = db.rawQuery("SELECT date,type,cheat,note FROM days ORDER BY date",null).use { c ->
            buildList { while(c.moveToNext()) add(DayAnnotation(c.getString(0),c.getString(1),c.getInt(2) != 0,c.getString(3))) }
        }
        val foods = db.rawQuery("SELECT payload FROM foods ORDER BY rowid",null).use { c ->
            buildList { while(c.moveToNext()) add(FoodCodec.decode(org.json.JSONObject(c.getString(0)))) }
        }
        return Records(weights, meals, trainings, profile, exercises, days, foods)
    }
    fun weight(w: Weight) { writableDatabase.insertWithOnConflict("weights",null,ContentValues().apply {
        put("date", w.date); put("kg",w.kg); put("note",w.note)
    }, SQLiteDatabase.CONFLICT_REPLACE) }
    fun meal(m: Meal) { writableDatabase.insertWithOnConflict("meals",null,ContentValues().apply {
        put("id",m.id); put("date",m.date); put("category",m.category); put("photos",JSONArray(m.photos).toString()); put("note",m.note); put("isCheatMeal",m.isCheatMeal)
    }, SQLiteDatabase.CONFLICT_REPLACE) }
    fun training(t: Training) { writableDatabase.insertWithOnConflict("trainings",null,ContentValues().apply {
        put("id",t.id); put("date",t.date); put("name",t.name); put("minutes",t.minutes)
        put("sets",t.sets); put("reps",t.reps); put("load",t.load); put("note",t.note)
        put("exerciseId",t.exerciseId); put("muscleGroup",t.muscleGroup); put("difficulty",t.difficulty)
    }, SQLiteDatabase.CONFLICT_REPLACE) }
    fun exercise(e: Exercise) { writableDatabase.insertWithOnConflict("exercises",null,ContentValues().apply {
        put("id",e.id); put("payload",ExerciseCodec.encode(e).toString())
    },SQLiteDatabase.CONFLICT_REPLACE) }
    fun saveTrainingWithDay(t: Training) = writableDatabase.transaction {
        training(t)
        execSQL("UPDATE days SET type='训练日' WHERE date=? AND type='休息日'",arrayOf(t.date))
    }
    fun food(f: Food) { writableDatabase.insertWithOnConflict("foods",null,ContentValues().apply {
        put("id",f.id); put("payload",FoodCodec.encode(f).toString())
    },SQLiteDatabase.CONFLICT_REPLACE) }
    fun day(d: DayAnnotation) { writableDatabase.insertWithOnConflict("days",null,ContentValues().apply {
        put("date",d.date); put("type",d.type); put("cheat",d.cheat); put("note",d.note)
    },SQLiteDatabase.CONFLICT_REPLACE) }
    fun profile(p: Profile) { writableDatabase.insertWithOnConflict("profile",null,ContentValues().apply {
        put("id",1); put("target",p.target); put("height",p.height); put("weeklyGoal",p.weeklyGoal); put("sex",p.sex)
    }, SQLiteDatabase.CONFLICT_REPLACE) }
    fun delete(table: String, key: String) {
        require(table in setOf("weights","meals","trainings","exercises","days","foods"))
        writableDatabase.delete(table, "${if(table in setOf("weights","days")) "date" else "id"}=?",arrayOf(key))
    }
    fun replace(r: Records) {
        val db = writableDatabase
        db.transaction {
            listOf("weights","meals","trainings","profile","exercises","days","foods").forEach { db.delete(it,null,null) }
            r.weights.forEach(::weight); r.meals.forEach(::meal); r.trainings.forEach(::training); profile(r.profile)
            r.exercises.forEach(::exercise)
            r.days.forEach(::day); r.foods.forEach(::food)
        }
    }
}
