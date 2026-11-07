package com.bodyrecord.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.bodyrecord.app.ui.AppIcons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Double.pretty() = String.format(Locale.ROOT,"%.1f",this)
fun decimal(value: String) = value.trim().replace(',','.').toDoubleOrNull()

@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(),shape = RoundedCornerShape(24.dp),color = color) {
        Column(Modifier.padding(20.dp),verticalArrangement = Arrangement.spacedBy(12.dp),content = content)
    }
}

@Composable
fun PageTitle(kicker: String, title: String, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(kicker,style = MaterialTheme.typography.labelMedium,color = Muted,letterSpacing = 2.sp)
            Text(title,style = MaterialTheme.typography.headlineLarge)
        }
        trailing()
    }
}

@Composable
fun DateNavigation(date: LocalDate, onDate: (LocalDate) -> Unit) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = { onDate(date.minusDays(1)) },enabled = date.year > 1900 || date.dayOfYear > 1) { Icon(AppIcons.ChevronLeft,"前一天") }
        Row(Modifier.clickable {
            DatePickerDialog(context,{ _,y,m,d -> onDate(LocalDate.of(y,m+1,d)) },date.year,date.monthValue-1,date.dayOfMonth).apply {
                datePicker.maxDate = System.currentTimeMillis()
                datePicker.minDate = LocalDate.of(1900,1,1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.show()
        }.padding(10.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(AppIcons.CalendarToday,null,Modifier.size(16.dp),tint = Pine)
            Text(if(date == LocalDate.now()) "今天 · ${date.format(DateTimeFormatter.ofPattern("M月d日"))}" else date.format(DateTimeFormatter.ofPattern("yyyy年M月d日")),style = MaterialTheme.typography.titleSmall)
            Icon(AppIcons.ExpandMore,null,Modifier.size(16.dp))
        }
        IconButton(onClick = { onDate(date.plusDays(1)) },enabled = date.isBefore(LocalDate.now())) { Icon(AppIcons.ChevronRight,"后一天") }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp),horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon,null,Modifier.size(32.dp),tint = Pine.copy(alpha = .55f))
        Text(title,fontWeight = FontWeight.SemiBold)
        Text(subtitle,color = Muted,style = MaterialTheme.typography.bodySmall)
        if(action != null) TextButton(onClick = onAction) { Text(action); Icon(AppIcons.ArrowForward,null,Modifier.padding(start = 6.dp).size(16.dp)) }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null, action: String? = null, onClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) {
        Column { Text(title,style = MaterialTheme.typography.titleMedium); if(subtitle != null) Text(subtitle,color = Muted,style = MaterialTheme.typography.bodySmall) }
        if(action != null) TextButton(onClick = onClick,contentPadding = PaddingValues(horizontal = 8.dp)) { Text(action) }
    }
}

@Composable
fun Metric(label: String, value: String, suffix: String, modifier: Modifier = Modifier) {
    Column(modifier,verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label,color = Muted,style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.Bottom,horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value,fontFamily = NumberFont,fontSize = 28.sp,fontWeight = FontWeight.Medium)
            if(suffix.isNotEmpty()) Text(suffix,color = Muted,modifier = Modifier.padding(bottom = 4.dp),style = MaterialTheme.typography.bodySmall)
        }
    }
}
