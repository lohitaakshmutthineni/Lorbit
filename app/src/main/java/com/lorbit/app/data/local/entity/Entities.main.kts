package com.lorbit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String,
    val professor: String = "",
    val room: String = "",
    val credits: Int = 3,
    val colorHex: Long = 0xFF3B82F6,
    val targetAttendance: Float = 75.0f,
    val attendedClasses: Int = 0,
    val totalClasses: Int = 0
)

@Entity(tableName = "timetable")
data class TimetableSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val subjectName: String,
    val dayOfWeek: Int, // 1 = Mon, 2 = Tue, ..., 7 = Sun
    val startTime: String, // "09:00 AM"
    val endTime: String,   // "10:30 AM"
    val room: String = "",
    val colorHex: Long = 0xFF3B82F6
)

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectName: String,
    val title: String,
    val dueDate: String,
    val priority: String = "Medium", // High, Medium, Low
    val isCompleted: Boolean = false
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectName: String = "General",
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckName: String,
    val question: String,
    val answer: String,
    val masteryLevel: Int = 1 // 1 to 5 (Leitner system)
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String = "Food", // Food, Transport, Books, Hostel, Other
    val date: String,
    val isSplit: Boolean = false,
    val splitWith: String = ""
)