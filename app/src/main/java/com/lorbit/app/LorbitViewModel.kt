package com.lorbit.app

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class LorbitViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = LorbitDatabase.getDatabase(application).dao()

    // 1. Multi-Account Management
    val accounts: StateFlow<List<UserAccountEntity>> = dao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAccount: StateFlow<UserAccountEntity?> = dao.getActiveAccount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val activeUserId: Long
        get() = activeAccount.value?.id ?: 1L

    // 2. Data partitioned by Active Profile
    val subjects: StateFlow<List<SubjectEntity>> = activeAccount
        .map { acc -> acc?.id ?: 1L }
        .distinctUntilChanged()
        .transformLatest { userId -> emitAll(dao.getSubjectsForUser(userId)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCategories: StateFlow<List<String>> = activeAccount
        .map { acc -> acc?.id ?: 1L }
        .distinctUntilChanged()
        .transformLatest { userId ->
            emitAll(dao.getCustomCategories(userId).map { list -> list.map { it.name } })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assignments: StateFlow<List<AssignmentEntity>> = activeAccount
        .map { acc -> acc?.id ?: 1L }
        .distinctUntilChanged()
        .transformLatest { userId -> emitAll(dao.getAssignmentsForUser(userId)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = activeAccount
        .map { acc -> acc?.id ?: 1L }
        .distinctUntilChanged()
        .transformLatest { userId -> emitAll(dao.getNotesForUser(userId)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = activeAccount
        .map { acc -> acc?.id ?: 1L }
        .distinctUntilChanged()
        .transformLatest { userId -> emitAll(dao.getExpensesForUser(userId)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalExpense: StateFlow<Double> = activeAccount
        .map { acc -> acc?.id ?: 1L }
        .distinctUntilChanged()
        .transformLatest { userId -> emitAll(dao.getTotalExpenseAmount(userId).map { it ?: 0.0 }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val currentDayNumber: Int = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        else -> 7
    }

    val todaySchedule: StateFlow<List<TimetableSlotEntity>> = activeAccount
        .map { acc -> acc?.id ?: 1L }
        .distinctUntilChanged()
        .transformLatest { userId -> emitAll(dao.getScheduleForDay(userId, currentDayNumber)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- User Accounts ---
    fun addAccount(name: String, college: String, semester: String) {
        viewModelScope.launch {
            dao.insertAccount(UserAccountEntity(name = name, college = college, semester = semester, isActive = false))
        }
    }

    fun switchAccount(accountId: Long) {
        viewModelScope.launch {
            dao.setActiveAccount(accountId)
        }
    }

    fun addCustomCategory(name: String) {
        viewModelScope.launch {
            dao.insertCustomCategory(CustomCategoryEntity(userId = activeUserId, name = name))
        }
    }

    // --- Timetable Auto-Sync ---
    fun addTimetableSlot(subjectName: String, dayOfWeek: Int, startTime: String, endTime: String, room: String) {
        viewModelScope.launch {
            val existing = dao.findSubjectByName(activeUserId, subjectName)
            val subId = existing?.id ?: dao.insertSubject(
                SubjectEntity(userId = activeUserId, name = subjectName, code = subjectName.take(3).uppercase() + "101", room = room)
            )
            dao.insertTimetableSlot(
                TimetableSlotEntity(userId = activeUserId, subjectId = subId, subjectName = subjectName, dayOfWeek = dayOfWeek, startTime = startTime, endTime = endTime, room = room)
            )
        }
    }

    fun deleteTimetableSlot(slot: TimetableSlotEntity) {
        viewModelScope.launch { dao.deleteTimetableSlot(slot) }
    }

    // --- AI Screenshot Scanners ---
    fun scanAndImportTimetable(bitmap: Bitmap, apiKey: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val slots = GeminiVisionScanner.scanTimetableImage(bitmap, apiKey)
            slots.forEach { slot ->
                addTimetableSlot(slot.subjectName, slot.dayOfWeek, slot.startTime, slot.endTime, slot.room)
            }
            onComplete(slots.size)
        }
    }

    fun scanAndImportAttendance(bitmap: Bitmap, apiKey: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val subjectsFromScan = GeminiVisionScanner.scanAttendanceImage(bitmap, apiKey)
            subjectsFromScan.forEach { sub ->
                val existing = dao.findSubjectByName(activeUserId, sub.name)
                if (existing != null) {
                    dao.updateSubject(existing.copy(attendedClasses = sub.attendedClasses, totalClasses = sub.totalClasses))
                } else {
                    dao.insertSubject(sub.copy(userId = activeUserId))
                }
            }
            onComplete(subjectsFromScan.size)
        }
    }

    // --- Attendance Actions ---
    fun markPresent(subjectId: Long) {
        viewModelScope.launch { dao.markPresent(subjectId) }
    }

    fun markAbsent(subjectId: Long) {
        viewModelScope.launch { dao.markAbsent(subjectId) }
    }

    fun addSubject(name: String, code: String, professor: String, room: String) {
        viewModelScope.launch {
            dao.insertSubject(SubjectEntity(userId = activeUserId, name = name, code = code, professor = professor, room = room))
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch { dao.deleteSubject(subject) }
    }

    // --- Assignments ---
    fun toggleAssignment(assignment: AssignmentEntity) {
        viewModelScope.launch {
            dao.updateAssignment(assignment.copy(isCompleted = !assignment.isCompleted))
        }
    }

    fun addAssignment(title: String, subjectName: String, dueDate: String, priority: String) {
        viewModelScope.launch {
            dao.insertAssignment(AssignmentEntity(userId = activeUserId, title = title, subjectName = subjectName, dueDate = dueDate, priority = priority))
        }
    }

    fun deleteAssignment(assignment: AssignmentEntity) {
        viewModelScope.launch { dao.deleteAssignment(assignment) }
    }

    // --- Notes & PDF ---
    fun addNote(title: String, content: String, subjectName: String, pdfUri: String? = null, pdfFileName: String? = null) {
        viewModelScope.launch {
            dao.insertNote(NoteEntity(userId = activeUserId, title = title, content = content, subjectName = subjectName, pdfUri = pdfUri, pdfFileName = pdfFileName))
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch { dao.deleteNote(note) }
    }

    // --- Expenses ---
    fun addExpense(title: String, amount: Double, category: String) {
        viewModelScope.launch {
            dao.insertExpense(ExpenseEntity(userId = activeUserId, title = title, amount = amount, category = category, date = "Today"))
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch { dao.deleteExpense(expense) }
    }

    // --- Bunk Calculator Algorithm ---
    fun calculateBunkStatus(attended: Int, total: Int, targetPercentage: Float = 75f): Pair<String, Boolean> {
        if (total == 0) return Pair("No classes held yet", true)
        val currentPct = (attended.toFloat() / total.toFloat()) * 100f

        return if (currentPct >= targetPercentage) {
            val safeBunks = ((attended * 100) / targetPercentage).toInt() - total
            if (safeBunks > 0) {
                Pair("You can safely skip $safeBunks class${if (safeBunks > 1) "es" else ""}", true)
            } else {
                Pair("On track! Don't miss the next class", true)
            }
        } else {
            val needed = Math.ceil(((targetPercentage * total - 100 * attended) / (100 - targetPercentage)).toDouble()).toInt()
            Pair("Need to attend $needed more class${if (needed > 1) "es" else ""} to reach ${targetPercentage.toInt()}%", false)
        }
    }
}