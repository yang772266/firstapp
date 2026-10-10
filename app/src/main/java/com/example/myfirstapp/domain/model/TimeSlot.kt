package com.example.myfirstapp.domain.model

data class TimeSlot(
    val period: Int,       // 节次 1-10
    val startTime: String, // 例如 "08:00"
    val endTime: String    // 例如 "08:45"
)
