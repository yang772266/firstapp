package com.example.myfirstapp.parser

import com.example.myfirstapp.domain.model.Course
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.math.abs

object HbustJsoupParser {

    private val presetColors = listOf(
        "#1E88E5", "#43A047", "#FB8C00", "#8E24AA",
        "#00ACC1", "#E53935", "#3949AB", "#D81B60",
        "#00897B", "#7CB342", "#FDD835", "#F4511E"
    )

    fun parseScheduleHtml(htmlContent: String): List<Course> {
        if (htmlContent.isBlank() || !htmlContent.contains("kb_table")) {
            throw IllegalArgumentException("登录状态已失效，请重新从学校官网进入教务系统并登录")
        }

        val doc: Document = Jsoup.parse(htmlContent)
        // 1. 只选择 table.kb_table tbody p[title]
        val pElements = doc.select("table.kb_table tbody p[title]")

        if (pElements.isEmpty()) {
            throw IllegalArgumentException("登录状态已失效，请重新从学校官网进入教务系统并登录")
        }

        val courses = mutableListOf<Course>()

        for (p in pElements) {
            val titleAttr = p.attr("title")
            if (titleAttr.isBlank()) continue

            // 2. 将 <br> 标签替换为换行，并解码 HTML 实体
            val decodedTitle = Jsoup.parse(titleAttr).text()
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("<br/>", "\n")
                .replace("<br>", "\n")
                .replace("</p>", "\n")

            val lines = decodedTitle.split("[\n;]".toRegex())
                .map { it.trim() }
                .filter { it.isNotBlank() }

            var name = ""
            var location = ""
            var teacher = ""
            var dayOfWeek = 1
            var startPeriod = 1
            var endPeriod = 2

            // 3. 从 title 属性中仅按字段名提取“课程名称：”“上课地点：”“任课教师：”
            for (line in lines) {
                val cleanLine = line.replace("<.*?>".toRegex(), "").trim()

                if (cleanLine.startsWith("课程名称：") || cleanLine.startsWith("课程名：")) {
                    name = cleanLine.substringAfter("：").trim()
                } else if (cleanLine.startsWith("上课地点：") || cleanLine.startsWith("地点：") || cleanLine.startsWith("教室：")) {
                    location = cleanLine.substringAfter("：").trim()
                } else if (cleanLine.startsWith("任课教师：") || cleanLine.startsWith("教师：") || cleanLine.startsWith("教师姓名：")) {
                    teacher = cleanLine.substringAfter("：").trim()
                } else if (cleanLine.startsWith("上课时间：") || cleanLine.startsWith("时间：")) {
                    val timeStr = cleanLine.substringAfter("：").trim()
                    val day = parseDayOfWeek(timeStr)
                    if (day != null) dayOfWeek = day

                    val periodMatch = "\\[?(\\d+)-(\\d+)\\]?节".toRegex().find(timeStr)
                    if (periodMatch != null) {
                        startPeriod = periodMatch.groupValues[1].toIntOrNull() ?: 1
                        endPeriod = periodMatch.groupValues[2].toIntOrNull() ?: (startPeriod + 1)
                    }
                }
            }

            if (name.isNotBlank()) {
                val colorIndex = abs(name.hashCode()) % presetColors.size
                courses.add(
                    Course(
                        name = name,
                        classroom = location,
                        teacher = teacher, // 若无教师信息，teacher 保持为空字符串，不引发异常
                        dayOfWeek = dayOfWeek,
                        startPeriod = startPeriod,
                        endPeriod = endPeriod,
                        colorHex = presetColors[colorIndex]
                    )
                )
            }
        }

        if (courses.isEmpty()) {
            throw IllegalArgumentException("登录状态已失效，请重新从学校官网进入教务系统并登录")
        }

        return courses.distinctBy { "${it.name}_${it.dayOfWeek}_${it.startPeriod}" }
    }

    private fun parseDayOfWeek(text: String): Int? {
        return when {
            text.contains("星期一") || text.contains("周一") -> 1
            text.contains("星期二") || text.contains("周二") -> 2
            text.contains("星期三") || text.contains("周三") -> 3
            text.contains("星期四") || text.contains("周四") -> 4
            text.contains("星期五") || text.contains("周五") -> 5
            text.contains("星期六") || text.contains("周六") -> 6
            text.contains("星期日") || text.contains("星期天") || text.contains("周日") -> 7
            else -> null
        }
    }
}
