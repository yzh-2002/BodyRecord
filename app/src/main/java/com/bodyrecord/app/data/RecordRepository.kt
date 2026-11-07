package com.bodyrecord.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class RecordRepository(private val context: Context) {
    private val db = RecordDatabase(context)
    val photoDir = File(context.filesDir,"photos").apply { mkdirs() }
    val mediaDir = File(context.filesDir,"exercise-media").apply { mkdirs() }
    private val mutex = Mutex()
    private suspend fun <T> locked(block: () -> T): T = withContext(Dispatchers.IO) { mutex.withLock { block() } }
    suspend fun load() = locked { db.snapshot() }
    suspend fun save(w: Weight) = locked {
        require(Rules.validDate(w.date) && Rules.weight(w.kg) && w.note.length <= 1000)
        db.weight(w)
    }
    suspend fun save(t: Training) = locked {
        require(Rules.validDate(t.date) && Rules.training(t) && t.note.length <= 1000)
        db.saveTrainingWithDay(t)
    }
    suspend fun saveDay(d: DayAnnotation) = locked {
        require(Journal.valid(d)) { "请检查日期与标记信息" }
        require(d.type != "休息日" || db.snapshot().trainings.none { it.date == d.date }) { "当天已有训练，请先修改训练记录再标记休息日" }
        if(d.type == "未标记" && !d.cheat && d.note.isBlank()) db.delete("days",d.date) else db.day(d)
    }
    suspend fun saveFood(f: Food) = locked {
        require(FoodCodec.valid(f)) { "请检查食物信息，营养素应为每 100 g 中的克数（0–100）" }
        require(f.custom || FoodCatalog.load(context).any { it.id == f.id }) { "找不到原始食物" }
        db.food(f)
    }
    suspend fun save(p: Profile) = locked {
        require((p.target == null || Rules.weight(p.target)) && (p.height == null || p.height.isFinite() && p.height in 80.0..250.0) && p.weeklyGoal in 1..7 && p.sex in listOf("","男","女"))
        db.profile(p)
    }
    suspend fun saveExercise(e: Exercise, uri: Uri? = null, removeMedia: Boolean = false) = locked {
        require(ExerciseCodec.valid(e)) { "请检查动作信息" }
        var added: File? = null
        try {
            val media = if(removeMedia) null else if(uri == null) e.media else {
                val file = File(mediaDir,"${UUID.randomUUID()}.gif")
                added = file
                context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { out ->
                    val buffer = ByteArray(8192); var total = 0
                    while(true) { val n = input.read(buffer); if(n < 0) break; total += n; require(total <= 12*1024*1024) { "演示图片不能超过 12 MB" }; out.write(buffer,0,n) }
                } } ?: error("无法读取演示图片，请重新选择")
                validateMedia(file)
                file.name
            }
            db.exercise(e.copy(media = media))
        } catch(e: Exception) { added?.delete(); throw e }
        cleanup()
    }
    suspend fun removeExercise(e: Exercise, builtin: Boolean) = locked {
        if(builtin) db.exercise(e.copy(hidden = true)) else db.delete("exercises",e.id)
        cleanup()
    }
    private fun validateMedia(file: File) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        file.inputStream().use { BitmapFactory.decodeStream(it,null,bounds) }
        require(bounds.outWidth in 1..4096 && bounds.outHeight in 1..4096) { "请选择有效的 GIF、WebP、PNG 或 JPG（边长不超过 4096）" }
    }
    suspend fun saveMeal(m: Meal, uris: List<Uri>) = locked {
        require(Rules.validDate(m.date) && uris.size in 1..6 && m.note.length <= 1000)
        val added = mutableListOf<File>()
        try {
            val paths = uris.map { uri ->
                val local = if(uri.scheme == "file") uri.path?.let(::File) else null
                if(local != null && local.parentFile?.canonicalFile == photoDir.canonicalFile && local.isFile) local.name
                else copyPhoto(uri).also { added += File(photoDir,it) }
            }
            db.meal(m.copy(photos = paths))
        } catch(e: Exception) { added.forEach { it.delete() }; throw e }
        cleanup()
    }
    private fun copyPhoto(uri: Uri): String {
        val source = File.createTempFile("photo-", ".tmp",context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input -> source.outputStream().use { out ->
                val buffer = ByteArray(8192); var total = 0
                while(true) { val n = input.read(buffer); if(n < 0) break; total += n; require(total <= 30 * 1024 * 1024) { "请选择小于 30 MB 的照片" }; out.write(buffer,0,n) }
            } } ?: error("无法读取图片，请重新选择")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.path,bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法识别这张图片" }
            var sample = 1
            while(maxOf(bounds.outWidth,bounds.outHeight) / sample > 2048) sample *= 2
            val bitmap = BitmapFactory.decodeFile(source.path,BitmapFactory.Options().apply { inSampleSize = sample }) ?: error("图片解码失败")
            val exif = ExifInterface(source)
            val matrix = Matrix().apply { if(exif.isFlipped) postScale(-1f,1f); postRotate(exif.rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
            val file = File(photoDir,"${UUID.randomUUID()}.jpg")
            try { file.outputStream().use { check(rotated.compress(Bitmap.CompressFormat.JPEG,88,it)) } }
            catch(e: Exception) { file.delete(); throw e }
            finally { if(rotated !== bitmap) rotated.recycle(); bitmap.recycle() }
            return file.name
        } finally { source.delete() }
    }
    suspend fun delete(table: String, id: String) = locked { db.delete(table,id); cleanup() }
    private fun cleanup() {
        val records = db.snapshot()
        val used = records.meals.flatMap { it.photos }.toSet()
        photoDir.listFiles()?.filter { it.name !in used }?.forEach { it.delete() }
        val media = records.exercises.mapNotNull { it.media }.toSet()
        mediaDir.listFiles()?.filter { it.name !in media }?.forEach { it.delete() }
    }
    suspend fun export(uri: Uri) = locked {
        val records = db.snapshot()
        val metadata = BackupCodec.encode(records).toByteArray(Charsets.UTF_8)
        val photos = records.meals.flatMap { it.photos }.distinct().map { File(photoDir,it) }
        val media = records.exercises.mapNotNull { it.media }.distinct().map { File(mediaDir,it) }
        require(listOf(records.weights.size,records.meals.size,records.trainings.size,records.exercises.size,records.days.size,records.foods.size).all { it <= 50000 } && photos.size + media.size < 10000 && metadata.size <= 12*1024*1024) { "记录数量超过当前备份格式上限" }
        require((photos + media).all { it.isFile && it.length() <= 12L*1024*1024 }) { "照片或演示文件丢失、过大，无法完整备份" }
        require((photos + media).sumOf { it.length() } + metadata.size <= 300L*1024*1024) { "完整备份超过 300 MB 上限" }
        context.contentResolver.openOutputStream(uri,"wt")?.use { output -> ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("data.json")); zip.write(metadata); zip.closeEntry()
            photos.forEach { file ->
                zip.putNextEntry(ZipEntry("photos/${file.name}")); file.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
            }
            media.forEach { file ->
                zip.putNextEntry(ZipEntry("exercise-media/${file.name}")); file.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
            }
        } } ?: error("无法写入备份文件")
    }
    suspend fun restore(uri: Uri) = locked {
        val stage = File(context.cacheDir,"restore-${UUID.randomUUID()}").apply { mkdirs() }
        val added = mutableListOf<File>()
        var committed = false
        try {
            var bytes = 0L; var entries = 0; val names = mutableSetOf<String>()
            context.contentResolver.openInputStream(uri)?.use { input -> ZipInputStream(input).use { zip ->
                while(true) {
                    val entry = zip.nextEntry ?: break
                    require(++entries <= 10000 && !entry.isDirectory && names.add(entry.name)) { "备份结构无效" }
                    require(entry.name == "data.json" || entry.name.startsWith("photos/") && BackupCodec.safePhoto(entry.name.removePrefix("photos/")) || entry.name.startsWith("exercise-media/") && ExerciseCodec.safeMedia(entry.name.removePrefix("exercise-media/"))) { "备份包含无效路径" }
                    val target = File(stage,entry.name).apply { parentFile?.mkdirs() }
                    var size = 0L
                    target.outputStream().use { out ->
                        val buffer = ByteArray(8192)
                        while(true) { val n = zip.read(buffer); if(n < 0) break; size += n; bytes += n
                            require(size <= 12L*1024*1024 && bytes <= 300L*1024*1024) { "备份过大（最多 300 MB）" }; out.write(buffer,0,n)
                        }
                    }
                    zip.closeEntry()
                }
            } } ?: error("无法打开备份")
            val records = BackupCodec.decode(File(stage,"data.json").readText(Charsets.UTF_8))
            val mapping = records.meals.flatMap { it.photos }.distinct().associateWith { old ->
                val source = File(stage,"photos/$old"); require(source.isFile) { "备份缺少照片" }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(source.path,bounds); require(bounds.outWidth > 0 && bounds.outHeight > 0) { "备份照片损坏" }
                val name = "${UUID.randomUUID()}.jpg"; val dest = File(photoDir,name); added += dest; source.copyTo(dest); name
            }
            val mediaMapping = records.exercises.mapNotNull { it.media }.distinct().associateWith { old ->
                val source = File(stage,"exercise-media/$old"); require(source.isFile) { "备份缺少动作演示" }; validateMedia(source)
                val name = "${UUID.randomUUID()}.${old.substringAfterLast('.')}"; val destination = File(mediaDir,name)
                added += destination; source.copyTo(destination); name
            }
            db.replace(records.copy(meals = records.meals.map { it.copy(photos = it.photos.map { p -> mapping.getValue(p) }) },
                exercises = records.exercises.map { it.copy(media = it.media?.let(mediaMapping::getValue)) }))
            committed = true
            cleanup()
        } finally {
            if(!committed) added.forEach { it.delete() }
            stage.deleteRecursively()
        }
    }
}
