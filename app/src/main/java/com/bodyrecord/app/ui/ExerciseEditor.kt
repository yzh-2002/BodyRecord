package com.bodyrecord.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.core.net.toUri
import com.bodyrecord.app.RecordViewModel
import com.bodyrecord.app.data.*
import java.util.UUID
import androidx.compose.ui.unit.dp

@Composable
fun ExerciseEditor(original: Exercise?, defaultGroup: String, busy: Boolean, vm: RecordViewModel, close: () -> Unit) {
    var name by rememberSaveable(original?.id) { mutableStateOf(original?.name ?: "") }
    var group by rememberSaveable(original?.id) { mutableStateOf(original?.group ?: defaultGroup) }
    var difficulty by rememberSaveable(original?.id) { mutableStateOf(original?.difficulty ?: "一般") }
    var equipment by rememberSaveable(original?.id) { mutableStateOf(original?.equipment ?: "") }
    var pattern by rememberSaveable(original?.id) { mutableStateOf(original?.pattern ?: "自定义") }
    var description by rememberSaveable(original?.id) { mutableStateOf(original?.description ?: "") }
    var steps by rememberSaveable(original?.id) { mutableStateOf(original?.steps?.joinToString("\n") ?: "") }
    var tips by rememberSaveable(original?.id) { mutableStateOf(original?.tips ?: "") }
    var mediaUri by rememberSaveable(original?.id) { mutableStateOf<String?>(null) }
    var removeMedia by rememberSaveable(original?.id) { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val id = rememberSaveable(original?.id) { original?.id ?: "custom_${UUID.randomUUID()}" }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri != null) { mediaUri = uri.toString(); removeMedia = false }
    }
    Editor(if(original == null) "新建训练动作" else "编辑训练动作",null,busy,close,{
        val value = Exercise(id,name.trim(),group.trim(),pattern.trim().ifBlank { "自定义" },difficulty,equipment.trim().ifBlank { "自重 / 自定义" },
            description.trim(),steps.lines().map { it.trim() }.filter { it.isNotBlank() },tips.trim(),original?.demoAsset ?: "",original?.demoSource ?: "",original?.media,original?.hidden ?: false)
        if(!ExerciseCodec.valid(value)) error = "请填写动作名称（最多 80 字）、分类（最多 24 字），步骤不超过 20 行"
        else vm.saveExercise(value,mediaUri?.toUri(),removeMedia,close)
    },saveLabel = "保存动作") {
        EntryField(name,{ name = it; error = null },"动作名称",modifier = Modifier.testTag("exercise-editor-name"))
        Text("肌群分类",style = MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ExerciseCatalog.groups.forEach { g -> FilterChip(selected = group == g,onClick = { group = g },label = { Text(g) }) }
        }
        EntryField(group,{ group = it },"分类（也可输入自己的分类）")
        Text("动作难度",style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExerciseCatalog.difficulties.forEach { d -> FilterChip(selected = difficulty == d,onClick = { difficulty = d },label = { Text(d) },modifier = Modifier.testTag("editor-difficulty-$d")) }
        }
        EntryField(equipment,{ equipment = it },"使用器械（可选）")
        EntryField(pattern,{ pattern = it },"动作类型，如划船、推举、弯举")
        EntryField(description,{ description = it },"动作介绍（可选）")
        EntryField(steps,{ steps = it },"动作步骤（每行一步，可选）")
        EntryField(tips,{ tips = it },"动作要点（可选）")
        OutlinedButton(onClick = { picker.launch(arrayOf("image/gif","image/webp","image/jpeg","image/png")) },enabled = !busy,modifier = Modifier.fillMaxWidth()) {
            Text(if(mediaUri != null) "已选择演示 · 点击重新选择" else "导入 GIF / WebP / 图片演示")
        }
        Text("演示文件会复制到应用内，单个文件最多 12 MB，图片边长最多 4096。",color = Muted,style = MaterialTheme.typography.bodySmall)
        if(mediaUri != null || original?.media != null && !removeMedia) TextButton(onClick = { mediaUri = null; removeMedia = true }) {
            Text(if(original?.demoAsset?.isNotEmpty() == true) "移除自选演示，使用内置动图" else "移除自选演示")
        }
        if(error != null) Text(error!!,color = MaterialTheme.colorScheme.error)
    }
}
