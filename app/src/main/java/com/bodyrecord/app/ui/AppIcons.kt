package com.bodyrecord.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Small, app-owned vector set: no large icon-pack dependency or bitmap scaling. */
object AppIcons {
    private fun icon(name: String, data: String) = ImageVector.Builder(name,24.dp,24.dp,24f,24f).addPath(
        pathData = PathParser().parsePathString(data).toNodes(),
        fill = null,stroke = SolidColor(Color.Black),strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round,strokeLineJoin = StrokeJoin.Round
    ).build()
    val ChevronLeft = icon("ChevronLeft","M15 6L9 12L15 18")
    val ChevronRight = icon("ChevronRight","M9 6L15 12L9 18")
    val ExpandMore = icon("ExpandMore","M6 9L12 15L18 9")
    val CalendarToday = icon("Calendar","M5 5H19Q21 5 21 7V19Q21 21 19 21H5Q3 21 3 19V7Q3 5 5 5ZM7 3V7M17 3V7M3 10H21")
    val ShowChart = icon("Chart","M3 4V21H22M6 15L11 10L15 13L21 6")
    val ArrowForward = icon("ArrowForward","M4 12H20M14 6L20 12L14 18")
    val Close = icon("Close","M6 6L18 18M18 6L6 18")
    val Add = icon("Add","M12 5V19M5 12H19")
    val Check = icon("Check","M5 12L10 17L20 7")
    val Edit = icon("Edit","M4 16L15 5Q16 4 17 5L19 7Q20 8 19 9L8 20H4V16ZM13 7L17 11")
    val AddPhotoAlternate = icon("AddPhoto","M12 4H5Q3 4 3 6V19Q3 21 5 21H18Q20 21 20 19V12M3 17L8 12L12 16L15 13L20 18M15 5H23M19 1V9M7 8H7.1")
    val GridView = icon("Dashboard","M3 3H10V10H3ZM14 3H21V10H14ZM3 14H10V21H3ZM14 14H21V21H14Z")
    val EventNote = icon("Journal","M6 3H18Q20 3 20 5V20H6Q3 20 3 17V6Q3 3 6 3ZM6 3V20M10 8H16M10 12H16M10 16H14")
    val PersonOutline = icon("Person","M16 7A4 4 0 1 1 8 7A4 4 0 1 1 16 7ZM4 21V19C4 12 20 12 20 19V21")
    val Restaurant = icon("Restaurant","M5 3V9Q5 12 8 12Q11 12 11 9V3M8 3V21M18 21V3C14 5 14 12 18 12")
    val FitnessCenter = icon("Training","M7 7L17 17M3 7L7 3M17 21L21 17M3 11L11 3M13 21L21 13M5 13L13 5M11 19L19 11")
    val Spa = icon("Leaf","M20 3C9 2 3 8 4 15C5 21 13 23 17 16C20 11 20 6 20 3ZM3 22L15 9M9 16V10M12 13H17")
    val MoreHoriz = icon("More","M5 12H5.1M12 12H12.1M19 12H19.1")
    val DeleteOutline = icon("Delete","M3 6H21M9 6V3H15V6M5 6L6 21H18L19 6M10 10V17M14 10V17")
    val Search = icon("Search","M17 10A7 7 0 1 1 3 10A7 7 0 1 1 17 10ZM15 15L21 21")
    val FileUpload = icon("Export","M12 16V3M7 8L12 3L17 8M3 15V21H21V15")
    val FileDownload = icon("Import","M12 3V16M7 11L12 16L17 11M3 15V21H21V15")
}
