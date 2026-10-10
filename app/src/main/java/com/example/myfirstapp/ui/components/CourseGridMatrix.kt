package com.example.myfirstapp.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.domain.model.Course
import com.example.myfirstapp.domain.model.TimeSlot

@Composable
fun CourseGridMatrix(
    courses: List<Course>,
    timeSlots: List<TimeSlot>,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        val timeColumnWidth = 36.dp
        val headerHeight = 36.dp
        val spacing = 3.dp

        // 动态计算单列宽度：确保 周一 至 周五 5 天完整展示在屏幕内，绝不切边
        val columnWidth = ((totalWidth - timeColumnWidth - (spacing * 6)) / 5).coerceAtLeast(56.dp)

        // 动态计算单节高度：确保 1~8 节（4大节）正好充满屏幕高度；9、10 节在屏幕外，下滑查看
        val singleRowHeight = ((totalHeight - headerHeight - (spacing * 9)) / 8).coerceAtLeast(58.dp)

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. 顶部星期表头 Row (与下方课表网格同步水平滚动)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左上角节次固定表头
                Card(
                    modifier = Modifier
                        .width(timeColumnWidth)
                        .height(headerHeight),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "节次",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(spacing))

                // 7天星期表头 (周一至周五完整显示，周六周日右滑可看)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    days.forEach { day ->
                        Card(
                            modifier = Modifier
                                .width(columnWidth)
                                .height(headerHeight),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing))

            // 2. 课表网格内容区域 (固定左侧时间栏 + 可双向平滑滚动网格)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // 左侧 1~10 单节节次侧栏 (1~8节全屏展示，9~10节下滑展示)
                Column(
                    modifier = Modifier
                        .width(timeColumnWidth)
                        .verticalScroll(verticalScrollState),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    timeSlots.forEach { slot ->
                        Card(
                            modifier = Modifier
                                .width(timeColumnWidth)
                                .height(singleRowHeight),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (slot.period < 10) "0${slot.period}" else "${slot.period}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(spacing))

                // 右侧 7天 × 10节 课程网格 (课程卡片跨越 2 节或多节整体展示)
                val gridTotalHeight = singleRowHeight * 10 + spacing * 9
                val gridTotalWidth = columnWidth * 7 + spacing * 6

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(horizontalScrollState)
                        .verticalScroll(verticalScrollState)
                ) {
                    Box(
                        modifier = Modifier
                            .width(gridTotalWidth)
                            .height(gridTotalHeight)
                    ) {
                        // 遍历 7 天 (周一至周日)
                        for (dayIndex in 1..7) {
                            val dayCourses = courses.filter { it.dayOfWeek == dayIndex }
                            val xOffset = (columnWidth + spacing) * (dayIndex - 1)

                            for (course in dayCourses) {
                                val startP = course.startPeriod.coerceIn(1, 10)
                                val endP = course.endPeriod.coerceIn(startP, 10)
                                val periodSpan = endP - startP + 1

                                val yOffset = (singleRowHeight + spacing) * (startP - 1)
                                val cardHeight = singleRowHeight * periodSpan + spacing * (periodSpan - 1)

                                Box(
                                    modifier = Modifier
                                        .offset(x = xOffset, y = yOffset)
                                        .width(columnWidth)
                                        .height(cardHeight)
                                ) {
                                    CourseCardItem(
                                        course = course,
                                        onClick = { onCourseClick(course) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
