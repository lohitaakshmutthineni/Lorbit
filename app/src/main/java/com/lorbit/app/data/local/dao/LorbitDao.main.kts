package com.lorbit.app.data.local.dao

import androidx.room.*
import com.lorbit.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LorbitDao {
    // --- Subjects & Attendance ---
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("UPDATE subjects SET attendedClasses = attendedClasses + 1, totalClasses = totalClasses + 1 WHERE id = :subjectId")
    suspend fun markPresent(subjectId: Long)

    @Query("UPDATE subjects SET totalClasses = totalClasses + 1 WHERE id = :subjectId")
    suspend fun markAbsent(subjectId: Long)

    // --- Timetable ---
    @Query("SELECT * FROM timetable WHERE dayOfWeek = :day ORDER BY startTime ASC")
    fun getScheduleForDay(day: Int): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable ORDER BY dayOfWeek, startTime ASC")
    fun getAllTimetableSlots(): Flow<List<TimetableSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableSlot(slot: TimetableSlotEntity)

    @Delete
    suspend fun deleteTimetableSlot(slot: TimetableSlotEntity)

    // --- Assignments ---
    @Query("SELECT * FROM assignments ORDER BY isCompleted ASC, dueDate ASC")
    fun getAllAssignments(): Flow<List<AssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: AssignmentEntity)

    @Update
    suspend fun updateAssignment(assignment: AssignmentEntity)

    @Delete
    suspend fun deleteAssignment(assignment: AssignmentEntity)

    // --- Notes ---
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    // --- Flashcards ---
    @Query("SELECT * FROM flashcards ORDER BY masteryLevel ASC")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(card: FlashcardEntity)

    @Update
    suspend fun updateFlashcard(card: FlashcardEntity)

    @Delete
    suspend fun deleteFlashcard(card: FlashcardEntity)

    // --- Expenses ---
    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalExpenseAmount(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}