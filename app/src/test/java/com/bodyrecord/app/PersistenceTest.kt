package com.bodyrecord.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bodyrecord.app.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PersistenceTest {
    private lateinit var db: RecordDatabase
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("bodyrecord.db")
        db = RecordDatabase(context)
    }
    @After fun close() { db.close() }
    private fun sample() = Records(
        listOf(Weight("2026-01-01",72.5,"晨起")),
        listOf(Meal(id = "meal-1",date = "2026-01-01",category = "午餐",photos = listOf("test-photo.jpg"),note = "米饭蔬菜")),
        listOf(Training(id = "train-1",date = "2026-01-01",name = "深蹲",minutes = 35,sets = 4,reps = 12,load = 40.0)),
        Profile(68.0,175.0,4)
    )
    @Test fun dataAndBackupRoundTripWithoutLosingPhotosOrOptionalFields() {
        val original = sample()
        db.replace(original)
        assertEquals(original,db.snapshot())
        assertEquals(original,BackupCodec.decode(BackupCodec.encode(original)))
        assertEquals(Records(),BackupCodec.decode(BackupCodec.encode(Records())))
    }
    @Test fun sameDayWeightUpdatesInsteadOfDuplicating() {
        db.weight(Weight("2026-01-01",72.5)); db.weight(Weight("2026-01-01",72.0))
        assertEquals(listOf(Weight("2026-01-01",72.0)),db.snapshot().weights)
    }
    @Test fun invalidRestoreDoesNotModifyExistingDatabase() {
        db.replace(sample())
        val bad = JSONObject(BackupCodec.encode(sample()))
        bad.getJSONArray("weights").getJSONObject(0).put("kg",-1)
        assertThrows(IllegalArgumentException::class.java) { db.replace(BackupCodec.decode(bad.toString())) }
        assertEquals(sample(),db.snapshot())
    }
    @Test fun rejectDuplicateAndUnsafePhotoEntries() {
        val bad = JSONObject(BackupCodec.encode(sample()))
        val weights = bad.getJSONArray("weights"); weights.put(weights.getJSONObject(0))
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(bad.toString()) }
        assertFalse(BackupCodec.safePhoto("../private.jpg"))
        assertFalse(BackupCodec.safePhoto("/absolute.jpg"))
        assertTrue(BackupCodec.safePhoto("test-photo.jpg"))
    }
    @Test fun deletingMealPreservesOtherRecordTypes() {
        db.replace(sample()); db.delete("meals","meal-1")
        assertEquals(sample().copy(meals = emptyList()),db.snapshot())
    }
}
