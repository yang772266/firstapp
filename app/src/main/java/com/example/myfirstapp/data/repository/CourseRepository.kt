package com.example.myfirstapp.data.repository

import com.example.myfirstapp.data.local.dao.CourseDao
import com.example.myfirstapp.data.local.entity.toDomainModel
import com.example.myfirstapp.data.local.entity.toEntity
import com.example.myfirstapp.domain.model.Course
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CourseRepository(private val courseDao: CourseDao) {

    val allCourses: Flow<List<Course>> = courseDao.getAllCourses().map { entityList ->
        entityList.map { entity -> entity.toDomainModel() }
    }

    suspend fun importCoursesForWeek(courses: List<Course>, clearExisting: Boolean, targetWeek: Int) {
        if (clearExisting) {
            courseDao.deleteCoursesForWeek(targetWeek)
        }
        val entities = courses.map { it.toEntity() }
        courseDao.insertCourses(entities)
    }

    suspend fun importCourses(courses: List<Course>, clearExisting: Boolean) {
        if (clearExisting) {
            courseDao.deleteAllCourses()
        }
        val entities = courses.map { it.toEntity() }
        courseDao.insertCourses(entities)
    }
}
