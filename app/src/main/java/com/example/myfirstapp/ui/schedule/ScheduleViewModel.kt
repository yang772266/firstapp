package com.example.myfirstapp.ui.schedule

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfirstapp.data.repository.CourseRepository
import com.example.myfirstapp.domain.model.Course
import com.example.myfirstapp.domain.model.TimeSlot
import com.example.myfirstapp.domain.model.isActiveInWeek
import com.example.myfirstapp.network.HbustCourseFetcher
import com.example.myfirstapp.parser.JsoupCourseParser
import com.example.myfirstapp.parser.ScreenshotCourseParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val currentWeek: Int = 3,
    val totalWeeks: Int = 20,
    val courses: List<Course> = emptyList(),
    val timeSlots: List<TimeSlot> = emptyList(),
    val isImportDialogVisible: Boolean = false,
    val isParsing: Boolean = false,
    val parsedCoursesForPreview: List<Course>? = null,
    val importMessage: String? = null
)

class ScheduleViewModel(
    private val repository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private var allCourseList: List<Course> = emptyList()

    init {
        loadTimeSlots()
        observeCourses()
    }

    fun selectWeek(week: Int) {
        _uiState.value = _uiState.value.copy(
            currentWeek = week,
            courses = filterCoursesForWeek(allCourseList, week)
        )
    }

    fun showImportDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(
            isImportDialogVisible = show,
            parsedCoursesForPreview = if (!show) null else _uiState.value.parsedCoursesForPreview,
            importMessage = if (!show) null else _uiState.value.importMessage
        )
    }

    fun fetchHbustSchedule(cookieString: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isParsing = true, importMessage = null)
            try {
                val courses = HbustCourseFetcher.fetchAndParseSchedule(cookieString)
                _uiState.value = _uiState.value.copy(
                    isParsing = false,
                    parsedCoursesForPreview = courses,
                    importMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isParsing = false,
                    parsedCoursesForPreview = null,
                    importMessage = e.message ?: "登录状态已失效，请重新登录教务系统"
                )
            }
        }
    }

    fun parseHtmlContent(htmlContent: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isParsing = true)
            val parsedCourses = JsoupCourseParser.parseHtmlContent(htmlContent)
            _uiState.value = _uiState.value.copy(
                isParsing = false,
                parsedCoursesForPreview = parsedCourses,
                importMessage = if (parsedCourses.isEmpty()) "未能从 HTML 网页中解析出有效课程" else null
            )
        }
    }

    fun parseImageUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isParsing = true)
            try {
                val parsedCourses = ScreenshotCourseParser.parseFromImageUri(context, uri)
                _uiState.value = _uiState.value.copy(
                    isParsing = false,
                    parsedCoursesForPreview = parsedCourses,
                    importMessage = if (parsedCourses.isEmpty()) "未能从图片识别出有效课表内容" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isParsing = false,
                    parsedCoursesForPreview = null,
                    importMessage = "读取或解析图片失败：${e.message}"
                )
            }
        }
    }

    fun confirmImport(courses: List<Course>, clearExisting: Boolean, targetWeek: Int = _uiState.value.currentWeek) {
        viewModelScope.launch {
            // 将本次导入的课程作用域锁定在当前选择的第 targetWeek 周
            val weekScopedCourses = courses.map { course ->
                course.copy(
                    startWeek = targetWeek,
                    endWeek = targetWeek
                )
            }
            repository.importCoursesForWeek(weekScopedCourses, clearExisting, targetWeek)
            _uiState.value = _uiState.value.copy(
                isImportDialogVisible = false,
                parsedCoursesForPreview = null,
                importMessage = "成功导入第 ${targetWeek} 周的 ${courses.size} 门课程！"
            )
        }
    }

    fun clearImportPreview() {
        _uiState.value = _uiState.value.copy(
            parsedCoursesForPreview = null,
            importMessage = null
        )
    }

    private fun observeCourses() {
        viewModelScope.launch {
            repository.allCourses.collect { courseList ->
                allCourseList = courseList
                _uiState.value = _uiState.value.copy(
                    courses = filterCoursesForWeek(courseList, _uiState.value.currentWeek)
                )
            }
        }
    }

    private fun filterCoursesForWeek(list: List<Course>, week: Int): List<Course> {
        return list.filter { it.isActiveInWeek(week) }
    }

    private fun loadTimeSlots() {
        val timeSlots = listOf(
            TimeSlot(1, "08:00", "08:45"), TimeSlot(2, "08:55", "09:40"),
            TimeSlot(3, "10:00", "10:45"), TimeSlot(4, "10:55", "11:40"),
            TimeSlot(5, "14:00", "14:45"), TimeSlot(6, "14:55", "15:40"),
            TimeSlot(7, "16:00", "16:45"), TimeSlot(8, "16:55", "17:40"),
            TimeSlot(9, "19:00", "19:45"), TimeSlot(10, "19:55", "20:40")
        )
        _uiState.value = _uiState.value.copy(timeSlots = timeSlots)
    }
}
