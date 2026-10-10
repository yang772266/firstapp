package com.example.myfirstapp.parser

import com.example.myfirstapp.domain.model.Course
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlin.math.abs

object JsoupCourseParser {

    private val presetColors = listOf(
        "#1E88E5", "#43A047", "#FB8C00", "#8E24AA",
        "#00ACC1", "#E53935", "#3949AB", "#D81B60",
        "#00897B", "#7CB342", "#FDD835", "#F4511E"
    )

    fun parseHtmlContent(htmlContent: String): List<Course> {
        if (htmlContent.isBlank()) return emptyList()

        val doc: Document = Jsoup.parse(htmlContent)
        val courses = mutableListOf<Course>()

        // 查找教务系统表格 <table>
        val tables = doc.select("table")
        for (table in tables) {
            val parsedCourses = parseTable(table)
            if (parsedCourses.isNotEmpty()) {
                courses.addAll(parsedCourses)
                break
            }
        }

        if (courses.isEmpty()) {
            courses.addAll(parseTextBlocks(doc))
        }

        return courses.distinctBy { "${it.name}_${it.dayOfWeek}_${it.startPeriod}" }
    }

    private fun parseTable(table: Element): List<Course> {
        val courses = mutableListOf<Course>()
        val rows = table.select("tr")
        if (rows.size < 2) return emptyList()

        // 提取星期表头索引
        val dayMap = mutableMapOf<Int, Int>() // colIndex -> dayOfWeek (1..7)
        val headerRow = rows[0]
        val headerCells = headerRow.select("th, td")

        for ((colIndex, cell) in headerCells.withIndex()) {
            val text = cell.text().trim()
            val dayOfWeek = parseDayOfWeek(text)
            if (dayOfWeek != null) {
                dayMap[colIndex] = dayOfWeek
            }
        }

        if (dayMap.isEmpty()) {
            val startCol = if (headerCells.size >= 8) 1 else 0
            for (i in 0 until 7) {
                if (startCol + i < headerCells.size) {
                    dayMap[startCol + i] = i + 1
                }
            }
        }

        var periodCounter = 1
        for (r in 1 until rows.size) {
            val row = rows[r]
            val cells = row.select("td, th")
            if (cells.isEmpty()) continue

            val firstCellText = cells[0].text().trim()
            val rowPeriod = parsePeriod(firstCellText) ?: periodCounter

            val startColIndex = if (parsePeriod(firstCellText) != null) 1 else 0

            for (c in startColIndex until cells.size) {
                val dayOfWeek = dayMap[c] ?: (c - startColIndex + 1)
                if (dayOfWeek !in 1..7) continue

                val cell = cells[c]
                val course = parseCourseFromCell(cell, dayOfWeek, rowPeriod)
                if (course != null) {
                    courses.add(course)
                }
            }

            periodCounter += 2
        }

        return courses
    }

    private fun parseCourseFromCell(cell: Element, dayOfWeek: Int, rowPeriod: Int): Course? {
        // 优先检查元素（如 <p title="...">）中的 title 属性
        val titleElem = cell.select("[title]").firstOrNull { it.attr("title").isNotBlank() }
        val titleAttr = titleElem?.attr("title")

        if (!titleAttr.isNullOrBlank()) {
            val courseFromTitle = parseCourseFromTitleAttr(titleAttr, dayOfWeek, rowPeriod)
            if (courseFromTitle != null) {
                return courseFromTitle
            }
        }

        // 如果没有 title 属性，回退到常规单元格文本解析
        val cellHtml = cell.html().replace("(?i)<br\\s*/?>".toRegex(), "\n")
        val lines = Jsoup.parse(cellHtml).text().split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return null

        return parseCourseFromLines(lines, dayOfWeek, rowPeriod)
    }

    private fun parseCourseFromTitleAttr(titleAttr: String, defaultDay: Int, defaultPeriod: Int): Course? {
        // 解码与处理属性文本
        val decoded = Jsoup.parse(titleAttr).text()
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("<br/>", "\n")
            .replace("<br>", "\n")
            .replace("</p>", "\n")

        val lines = decoded.split("[\n;]".toRegex())
            .map { it.trim() }
            .filter { it.isNotBlank() }

        var name = ""
        var classroom = ""
        var teacher = ""
        var dayOfWeek = defaultDay
        var startPeriod = defaultPeriod
        var endPeriod = defaultPeriod + 1
        var startWeek = 1
        var endWeek = 20
        var weekType = 0

        for (line in lines) {
            val cleanLine = line.replace("<.*?>".toRegex(), "").trim()

            if (cleanLine.startsWith("课程名称：") || cleanLine.startsWith("课程名：")) {
                var rawName = cleanLine.substringAfter("：").trim()
                if (rawName.contains("@")) {
                    if (classroom.isBlank()) classroom = rawName.substringAfter("@").trim()
                    rawName = rawName.substringBefore("@").trim()
                }
                if (rawName.contains("-") && !rawName.contains("（") && !rawName.contains("(")) {
                    if (teacher.isBlank()) teacher = rawName.substringAfter("-").trim()
                    rawName = rawName.substringBefore("-").trim()
                }
                name = rawName
            } else if (cleanLine.startsWith("上课地点：") || cleanLine.startsWith("地点：") || cleanLine.startsWith("教室：")) {
                classroom = cleanLine.substringAfter("：").trim()
            } else if (cleanLine.startsWith("任课教师：") || cleanLine.startsWith("教师：") || cleanLine.startsWith("教师姓名：")) {
                teacher = cleanLine.substringAfter("：").trim()
            } else if (cleanLine.startsWith("上课时间：") || cleanLine.startsWith("时间：")) {
                val timeStr = cleanLine.substringAfter("：").trim()

                // 解析星期
                val day = parseDayOfWeek(timeStr)
                if (day != null) dayOfWeek = day

                // 解析节次 (如 "[01-02]节" 或 "1-2节")
                val periodMatch = "(?:\\[?(\\d+)-(\\d+)\\]?节|(\\d+)-(\\d+)节)".toRegex().find(timeStr)
                if (periodMatch != null) {
                    val p1 = periodMatch.groupValues[1].toIntOrNull() ?: periodMatch.groupValues[3].toIntOrNull() ?: defaultPeriod
                    val p2 = periodMatch.groupValues[2].toIntOrNull() ?: periodMatch.groupValues[4].toIntOrNull() ?: (p1 + 1)
                    startPeriod = p1
                    endPeriod = p2
                }

                // 解析周次 (如 "第4周" 或 "第1-16周")
                val weekMatch = "第?(\\d+)(?:-(\\d+))?周".toRegex().find(timeStr)
                if (weekMatch != null) {
                    startWeek = weekMatch.groupValues[1].toIntOrNull() ?: 1
                    endWeek = weekMatch.groupValues[2].toIntOrNull() ?: startWeek
                }
            }
        }

        // 如果 name 为空，尝试降级提取
        if (name.isBlank()) {
            for (line in lines) {
                if (line.contains("课程名称")) {
                    name = line.substringAfter("课程名称").replace("[:：]".toRegex(), "").trim()
                    break
                }
            }
        }

        if (name.isBlank()) return null

        val colorIndex = abs(name.hashCode()) % presetColors.size
        val colorHex = presetColors[colorIndex]

        return Course(
            name = name,
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

    private fun parseCourseFromLines(lines: List<String>, dayOfWeek: Int, period: Int): Course? {
        if (lines.isEmpty()) return null

        val name = lines[0]
        var classroom = ""
        var teacher = ""
        var startWeek = 1
        var endWeek = 20
        var weekType = 0
        var startPeriod = period
        var endPeriod = period + 1

        for (i in 1 until lines.size) {
            val line = lines[i]

            val weekMatch = "(\\d+)-(\\d+)周(?:\\(([单双])\\))?".toRegex().find(line)
            if (weekMatch != null) {
                startWeek = weekMatch.groupValues[1].toIntOrNull() ?: 1
                endWeek = weekMatch.groupValues[2].toIntOrNull() ?: 20
                val typeStr = weekMatch.groupValues.getOrNull(3)
                weekType = when (typeStr) {
                    "单" -> 1
                    "双" -> 2
                    else -> 0
                }
                continue
            }

            val periodMatch = "(\\d+)-(\\d+)节".toRegex().find(line)
            if (periodMatch != null) {
                startPeriod = periodMatch.groupValues[1].toIntOrNull() ?: period
                endPeriod = periodMatch.groupValues[2].toIntOrNull() ?: (startPeriod + 1)
                continue
            }

            if (line.contains("楼") || line.contains("室") || line.contains("馆") || line.contains("-")) {
                classroom = line
            } else if (teacher.isBlank() && line.length <= 4) {
                teacher = line
            } else if (classroom.isBlank()) {
                classroom = line
            }
        }

        val colorIndex = abs(name.hashCode()) % presetColors.size
        val colorHex = presetColors[colorIndex]

        return Course(
            name = name,
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
            text.contains("星期一") || text.contains("周一") -> 1
            text.contains("星期二") || text.contains("周二") -> 2
            text.contains("星期三") || text.contains("周三") -> 3
            text.contains("星期四") || text.contains("周四") -> 4
            text.contains("星期五") || text.contains("周五") -> 5
            text.contains("星期六") || text.contains("周六") -> 6
            text.contains("星期日") || text.contains("星期天") || text.contains("周日") -> 7
            text.contains("一") -> 1
            text.contains("二") -> 2
            text.contains("三") -> 3
            text.contains("四") -> 4
            text.contains("五") -> 5
            text.contains("六") -> 6
            text.contains("日") || text.contains("七") -> 7
            else -> null
        }
    }

    private fun parsePeriod(text: String): Int? {
        val match = "(\\d+)".toRegex().find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun parseTextBlocks(doc: Document): List<Course> {
        val courses = mutableListOf<Course>()
        val fullText = doc.text()
        val blocks = fullText.split("\\s+".toRegex())

        var currentDay = 1
        var currentPeriod = 1

        for (chunk in blocks) {
            if (chunk.contains("周一")) currentDay = 1
            else if (chunk.contains("周二")) currentDay = 2
            else if (chunk.contains("周三")) currentDay = 3
            else if (chunk.contains("周四")) currentDay = 4
            else if (chunk.contains("周五")) currentDay = 5
            else if (chunk.contains("周六")) currentDay = 6
            else if (chunk.contains("周日")) currentDay = 7

            val lines = chunk.split("\n").filter { it.isNotBlank() }
            if (lines.isNotEmpty()) {
                val course = parseCourseFromLines(lines, currentDay, currentPeriod)
                if (course != null && course.name.length >= 2) {
                    courses.add(course)
                    currentPeriod = (currentPeriod % 10) + 1
                }
            }
        }
        return courses
    }
}
