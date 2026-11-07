package com.bodyrecord.app

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.bodyrecord.app.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RepositoryTest {
    private lateinit var context: Context
    private lateinit var repo: RecordRepository
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("bodyrecord.db")
        File(context.filesDir,"photos").deleteRecursively()
        repo = RecordRepository(context)
    }
    @Test fun photoImportBackupRestoreAndDeleteAreComplete() = runBlocking {
        val source = File(context.cacheDir,"input.jpg")
        val bitmap = Bitmap.createBitmap(80,60,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.GREEN)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG,90,it) }
        bitmap.recycle()
        val meal = Meal(id = "test-meal",date = "2026-01-01",category = "早餐",photos = emptyList(),note = "早餐",isCheatMeal = true)
        repo.saveMeal(meal,listOf(Uri.fromFile(source)))
        val saved = repo.load().meals.single()
        assertTrue(saved.isCheatMeal)
        assertTrue(File(repo.photoDir,saved.photos.single()).isFile)
        source.delete()
        assertTrue(File(repo.photoDir,saved.photos.single()).isFile)
        val backup = File(context.cacheDir,"backup.zip")
        repo.export(Uri.fromFile(backup))
        repo.delete("meals",saved.id)
        assertTrue(repo.load().meals.isEmpty())
        assertTrue(repo.photoDir.listFiles().isNullOrEmpty())
        repo.restore(Uri.fromFile(backup))
        val restored = repo.load().meals.single()
        assertEquals(saved.copy(photos = restored.photos),restored)
        assertTrue(File(repo.photoDir,restored.photos.single()).isFile)
    }
    @Test fun unsafeZipAndMissingPhotosDoNotOverwriteRecords() = runBlocking {
        val original = Weight("2026-01-01",70.0)
        repo.save(original)
        val backup = File(context.cacheDir,"bad.zip")
        ZipOutputStream(backup.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("../outside.jpg")); zip.write(byteArrayOf(1)); zip.closeEntry()
        }
        assertTrue(runCatching { repo.restore(Uri.fromFile(backup)) }.isFailure)
        assertEquals(listOf(original),repo.load().weights)
        val incomplete = Records(meals = listOf(Meal(date = "2026-01-01",category = "早餐",photos = listOf("missing.jpg"))))
        ZipOutputStream(backup.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("data.json")); zip.write(BackupCodec.encode(incomplete).toByteArray()); zip.closeEntry()
        }
        assertTrue(runCatching { repo.restore(Uri.fromFile(backup)) }.isFailure)
        assertEquals(listOf(original),repo.load().weights)
        assertTrue(repo.photoDir.listFiles().isNullOrEmpty())
    }
    @Test fun customDemoSurvivesBackupAndMissingDemoDoesNotOverwrite() = runBlocking {
        val source = File(context.cacheDir,"demo.gif")
        context.assets.open("exercise-media/EIeI8Vf.gif").use { input -> source.outputStream().use { input.copyTo(it) } }
        val exercise = ExerciseCatalog.load(context).first().copy(id = "custom-demo",demoAsset = "",demoSource = "")
        repo.saveExercise(exercise,Uri.fromFile(source))
        val saved = repo.load().exercises.single()
        assertTrue(File(repo.mediaDir,saved.media!!).isFile)
        val backup = File(context.cacheDir,"exercise-backup.zip")
        repo.export(Uri.fromFile(backup))
        repo.removeExercise(saved,false)
        assertTrue(repo.load().exercises.isEmpty())
        assertTrue(repo.mediaDir.listFiles().isNullOrEmpty())
        repo.restore(Uri.fromFile(backup))
        val restored = repo.load().exercises.single()
        assertEquals(saved.copy(media = restored.media),restored)
        assertTrue(File(repo.mediaDir,restored.media!!).isFile)
        val original = repo.load()
        val broken = File(context.cacheDir,"broken-demo.zip")
        ZipOutputStream(broken.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("data.json")); zip.write(BackupCodec.encode(original).toByteArray()); zip.closeEntry()
        }
        assertTrue(runCatching { repo.restore(Uri.fromFile(broken)) }.isFailure)
        assertEquals(original,repo.load())
    }
}
