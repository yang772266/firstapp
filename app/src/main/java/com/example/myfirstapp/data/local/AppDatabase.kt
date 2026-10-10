package com.example.myfirstapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myfirstapp.data.local.dao.CourseDao
import com.example.myfirstapp.data.local.entity.CourseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [CourseEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "campus_schedule.db"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database.courseDao())
                    }
                }
            }
        }

        suspend fun populateDatabase(courseDao: CourseDao) {
            val initialCourses = listOf(
                CourseEntity(1, "高等数学", "A-302", "张教授", dayOfWeek = 1, startPeriod = 1, endPeriod = 2, colorHex = "#1E88E5"),
                CourseEntity(2, "大学英语", "B-105", "李老师", dayOfWeek = 1, startPeriod = 3, endPeriod = 4, colorHex = "#43A047"),
                CourseEntity(3, "数据结构", "C-201", "王教授", dayOfWeek = 2, startPeriod = 5, endPeriod = 6, colorHex = "#FB8C00"),
                CourseEntity(4, "计算机网络", "A-404", "赵老师", dayOfWeek = 3, startPeriod = 7, endPeriod = 8, colorHex = "#8E24AA"),
                CourseEntity(5, "大学物理", "C-301", "刘教授", dayOfWeek = 4, startPeriod = 1, endPeriod = 2, colorHex = "#00ACC1"),
                CourseEntity(6, "近代史纲要", "B-202", "陈教授", dayOfWeek = 5, startPeriod = 5, endPeriod = 6, colorHex = "#E53935"),
                CourseEntity(7, "体育(羽毛球)", "体育馆", "郑教练", dayOfWeek = 5, startPeriod = 7, endPeriod = 8, colorHex = "#3949AB"),
                CourseEntity(8, "Python程序设计", "实验楼3", "孙老师", dayOfWeek = 6, startPeriod = 3, endPeriod = 4, colorHex = "#D81B60")
            )
            courseDao.insertCourses(initialCourses)
        }
    }
}
