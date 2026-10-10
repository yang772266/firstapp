package com.example.myfirstapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.myfirstapp.domain.model.Course

@Composable
fun CourseCardItem(
    course: Course,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = try {
        Color(course.colorHex.toColorInt())
    } catch (_: Exception) {
        Color(0xFF42A5F5)
    }

    // 过滤多余班级后缀，清洗为精简干净的课程名称
    val cleanName = course.name
        .replace("（读写.*?）".toRegex(), "")
        .replace("（听说.*?）".toRegex(), "")
        .replace("（\\d+-.*?）".toRegex(), "")
        .replace("\\(读写.*?\\)".toRegex(), "")
        .replace("\\(听说.*?\\)".toRegex(), "")
        .trim()

    // 格式：课程名称 - 上课地点 - 任课教师
    val displayText = buildString {
        append(cleanName)
        if (course.classroom.isNotBlank()) {
            val loc = course.classroom.removePrefix("@").trim()
            if (loc.isNotBlank()) {
                append(" - $loc")
            }
        }
        if (course.teacher.isNotBlank()) {
            val tch = course.teacher.removePrefix("-").trim()
            if (tch.isNotBlank()) {
                append(" - $tch")
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 3.dp, vertical = 4.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = displayText,
                fontSize = 11.sp,
                lineHeight = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
