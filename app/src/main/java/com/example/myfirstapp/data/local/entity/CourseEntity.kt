package com.example.myfirstapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myfirstapp.domain.model.Course

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val classroom: String,
    val teacher: String,
    val dayOfWeek: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val startWeek: Int = 1,
    val endWeek: Int = 20,
    val weekType: Int = 0, // 0=每周, 1=单周, 2=双周
    val colorHex: String = "#1E88E5",
    val note: String = ""
)

fun CourseEntity.toDomainModel(): Course {
    return Course(
        id = id,
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

fun Course.toEntity(): CourseEntity {
    return CourseEntity(
        id = id,
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
