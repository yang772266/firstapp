package com.example.myfirstapp.domain.model

data class Course(
    val id: Long = 0,
    val name: String,
    val classroom: String,
    val teacher: String,
    val dayOfWeek: Int,    // 1=周一, 2=周二, ..., 7=周日
    val startPeriod: Int,  // 1-10
    val endPeriod: Int,    // 1-10
    val startWeek: Int = 1,
    val endWeek: Int = 20,
    val weekType: Int = 0, // 0=每周, 1=单周, 2=双周
    val colorHex: String = "#42A5F5"
)

fun Course.isActiveInWeek(week: Int): Boolean {
    if (week !in startWeek..endWeek) return false
    return when (weekType) {
        1 -> week % 2 != 0 // 单周
        2 -> week % 2 == 0 // 双周
        else -> true      // 每周
    }
}
