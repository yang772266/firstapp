package com.example.myfirstapp.parser

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.myfirstapp.domain.model.Course
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs

object ScreenshotCourseParser {

    private val presetColors = listOf(
        "#1E88E5", "#43A047", "#FB8C00", "#8E24AA",
        "#00ACC1", "#E53935", "#3949AB", "#D81B60",
        "#00897B", "#7CB342", "#FDD835", "#F4511E"
    )

    suspend fun parseFromImageUri(context: Context, imageUri: Uri): List<Course> {
        val inputImage = try {
            val bitmap = context.contentResolver.openInputStream(imageUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
            if (bitmap != null) {
                InputImage.fromBitmap(bitmap, 0)
            } else {
                InputImage.fromFilePath(context, imageUri)
            }
        } catch (_: Exception) {
            try {
                InputImage.fromFilePath(context, imageUri)
            } catch (e2: Exception) {
                e2.printStackTrace()
                return emptyList()
            }
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val visionText: Text = suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { result ->
                    if (continuation.isActive) continuation.resume(result)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(Text("", emptyList<Text.TextBlock>()))
                }
        }

        return parseTextResult(visionText)
    }

    private fun parseTextResult(visionText: Text): List<Course> {
        val courses = mutableListOf<Course>()
        val blocks = visionText.textBlocks

        var currentDay = 1
        var currentPeriod = 1

        for (block in blocks) {
            val text = block.text
            val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }

            if (lines.isEmpty()) continue

            // 检查是否包含星期或节次关键字
            for (line in lines) {
                val day = parseDayOfWeek(line)
                if (day != null) {
                    currentDay = day
                }
                val period = parsePeriod(line)
                if (period != null) {
                    currentPeriod = period
                }
            }

            // 解析可能包含课程信息的文本块
            val course = parseCourseBlock(lines, currentDay, currentPeriod)
            if (course != null && course.name.length >= 2) {
                courses.add(course)
            }
        }

        return courses.distinctBy { "${it.name}_${it.dayOfWeek}_${it.startPeriod}" }
    }

    private fun parseCourseBlock(lines: List<String>, defaultDay: Int, defaultPeriod: Int): Course? {
        val nameCandidate = lines.firstOrNull { line ->
            !line.contains("周") && !line.contains("节") && !line.contains("星期") &&
                    line.length >= 2 && !line.matches("\\d+".toRegex())
        } ?: return null

        var classroom = ""
        var teacher = ""
        var dayOfWeek = defaultDay
        var startPeriod = defaultPeriod
        var endPeriod = defaultPeriod + 1
        var startWeek = 1
        var endWeek = 20
        var weekType = 0

        for (line in lines) {
            val day = parseDayOfWeek(line)
            if (day != null) dayOfWeek = day

            val periodMatch = "(\\d+)-(\\d+)节".toRegex().find(line)
            if (periodMatch != null) {
                startPeriod = periodMatch.groupValues[1].toIntOrNull() ?: defaultPeriod
                endPeriod = periodMatch.groupValues[2].toIntOrNull() ?: (startPeriod + 1)
            }

            val weekMatch = "(\\d+)-(\\d+)周".toRegex().find(line)
            if (weekMatch != null) {
                startWeek = weekMatch.groupValues[1].toIntOrNull() ?: 1
                endWeek = weekMatch.groupValues[2].toIntOrNull() ?: 20
            }

            if (line != nameCandidate) {
                if (line.contains("楼") || line.contains("室") || line.contains("馆") || line.contains("-") || line.contains("A") || line.contains("B") || line.contains("C")) {
                    classroom = line
                } else if (teacher.isBlank() && line.length in 2..4 && !line.contains("节") && !line.contains("周")) {
                    teacher = line
                }
            }
        }

        val colorIndex = abs(nameCandidate.hashCode()) % presetColors.size
        val colorHex = presetColors[colorIndex]

        return Course(
            name = nameCandidate,
            classroom = classroom,
            teacher = teacher,
            dayOfWeek = dayOfWeek,
            startPeriod = startPeriod,
            endPeriod = endPeriod,
            startWeek = startWeek,
            endWeek = endWeek,
            weekType = weekType,
            colorHex = colorHex
        )
    }

    private fun parseDayOfWeek(text: String): Int? {
        return when {
            text.contains("周一") || text.contains("星期一") -> 1
            text.contains("周二") || text.contains("星期二") -> 2
            text.contains("周三") || text.contains("星期三") -> 3
            text.contains("周四") || text.contains("星期四") -> 4
            text.contains("周五") || text.contains("星期五") -> 5
            text.contains("周六") || text.contains("星期六") -> 6
            text.contains("周日") || text.contains("星期日") || text.contains("星期天") -> 7
            else -> null
        }
    }

    private fun parsePeriod(text: String): Int? {
        val match = "第?(\\d+)[节课]".toRegex().find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }
}
