package com.bodyrecord.app.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import com.bodyrecord.app.ui.AppIcons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bodyrecord.app.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun WeightChart(weights: List<Weight>, end: LocalDate, days: Int, target: Double? = null) {
    val points = Rules.window(weights,end,days)
    if(points.isEmpty()) {
        EmptyState(AppIcons.ShowChart,"这段时间还没有体重记录","记录第一笔体重，让变化有迹可循")
        return
    }
    val description = points.joinToString("，") { "${it.date}，${it.kg.pretty()}千克" }
    Canvas(Modifier.fillMaxWidth().height(210.dp).semantics { contentDescription = "近${days}天体重趋势：$description" }) {
        val left = 40.dp.toPx(); val right = size.width - 12.dp.toPx()
        val top = 16.dp.toPx(); val bottom = size.height - 28.dp.toPx()
        val rawMin = points.minOf { it.kg }; val rawMax = points.maxOf { it.kg }
        val span = maxOf(1.5,rawMax - rawMin)
        val low = floor((rawMin - span * .25) * 2) / 2
        val high = ceil((rawMax + span * .25) * 2) / 2
        fun y(kg: Double) = bottom - ((kg-low)/(high-low)).toFloat()*(bottom-top)
        fun x(w: Weight) = left + Rules.xFraction(w.date,end,days)*(right-left)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Muted.toArgb(); textSize = 10.dp.toPx() }
        for(i in 0..3) {
            val value = low + (high-low)*i/3
            val yy = y(value)
            drawLine(Color(0xFFE5E9E0),Offset(left,yy),Offset(right,yy),1.dp.toPx(),pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),5.dp.toPx())))
            drawIntoCanvas { it.nativeCanvas.drawText(value.pretty(),0f,yy+4.dp.toPx(),paint) }
        }
        if(target != null && target in low..high) {
            drawLine(Orange,Offset(left,y(target)),Offset(right,y(target)),1.dp.toPx(),pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(),4.dp.toPx())))
        }
        val line = Path().apply { points.forEachIndexed { i,w -> if(i == 0) moveTo(x(w),y(w.kg)) else lineTo(x(w),y(w.kg)) } }
        if(points.size > 1) {
            val fill = Path().apply { addPath(line); lineTo(x(points.last()),bottom); lineTo(x(points.first()),bottom); close() }
            drawPath(fill,Brush.verticalGradient(listOf(Pine.copy(alpha = .18f),Pine.copy(alpha = 0f)),top,bottom))
            drawPath(line,Pine,style = Stroke(width = 2.5.dp.toPx(),cap = StrokeCap.Round,join = StrokeJoin.Round))
        }
        points.forEach { w -> drawCircle(Color.White,4.5.dp.toPx(),Offset(x(w),y(w.kg))); drawCircle(Pine,3.dp.toPx(),Offset(x(w),y(w.kg))) }
        val ticks = if(days == 7) listOf(0,2,4,6) else listOf(0,9,19,29)
        ticks.forEach { index ->
            val label = end.minusDays((days-1-index).toLong()).format(DateTimeFormatter.ofPattern("M/d"))
            val xx = left + index.toFloat()/(days-1)*(right-left)
            drawIntoCanvas { it.nativeCanvas.drawText(label,(xx-paint.measureText(label)/2).coerceIn(0f,size.width-paint.measureText(label)),size.height-3.dp.toPx(),paint) }
        }
    }
}
