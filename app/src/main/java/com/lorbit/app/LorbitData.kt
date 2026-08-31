package com.lorbit.app

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// ==================== DATABASE ENTITIES ====================

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val college: String,
    val semester: String = "Semester 1",
    val isActive: Boolean = true
)
@Entity(tableName = "custom_categories")
data class CustomCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 1,
    val name: String
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 1,
    val name: String,
    val code: String,
    val professor: String = "",
    val room: String = "",
    val credits: Int = 3,
    val targetAttendance: Float = 75.0f,
    val attendedClasses: Int = 0,
    val totalClasses: Int = 0
)

@Entity(tableName = "timetable")
data class TimetableSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 1,
    val subjectId: Long = 0,
    val subjectName: String,
    val dayOfWeek: Int, // 1 = Mon ... 7 = Sun
    val startTime: String,
    val endTime: String,
    val room: String = ""
)

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 1,
    val subjectName: String,
    val title: String,
    val dueDate: String,
    val priority: String = "Medium",
    val isCompleted: Boolean = false
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 1,
    val subjectName: String = "General",
    val title: String,
    val content: String,
    val pdfUri: String? = null,
    val pdfFileName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 1,
    val title: String,
    val amount: Double,
    val category: String = "Food",
    val date: String
)

// ==================== ROOM DAO ====================

@Dao
interface LorbitDao {
    // Accounts
    @Query("SELECT * FROM user_accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<UserAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: UserAccountEntity): Long

    @Query("UPDATE user_accounts SET isActive = (id = :activeId)")
    suspend fun setActiveAccount(activeId: Long)

    @Query("SELECT * FROM user_accounts WHERE isActive = 1 LIMIT 1")
    fun getActiveAccount(): Flow<UserAccountEntity?>

    // Custom "Other" Subjects History
    @Query("SELECT * FROM custom_categories WHERE userId = :userId ORDER BY id DESC")
    fun getCustomCategories(userId: Long): Flow<List<CustomCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCustomCategory(category: CustomCategoryEntity)

    // Partitioned Data by userId
    @Query("SELECT * FROM subjects WHERE userId = :userId ORDER BY name ASC")
    fun getSubjectsForUser(userId: Long): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE userId = :userId AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findSubjectByName(userId: Long, name: String): SubjectEntity?

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

    // Timetable
    @Query("SELECT * FROM timetable WHERE userId = :userId AND dayOfWeek = :day ORDER BY startTime ASC")
    fun getScheduleForDay(userId: Long, day: Int): Flow<List<TimetableSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableSlot(slot: TimetableSlotEntity)

    @Delete
    suspend fun deleteTimetableSlot(slot: TimetableSlotEntity)

    // Assignments
    @Query("SELECT * FROM assignments WHERE userId = :userId ORDER BY isCompleted ASC, dueDate ASC")
    fun getAssignmentsForUser(userId: Long): Flow<List<AssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: AssignmentEntity)

    @Update
    suspend fun updateAssignment(assignment: AssignmentEntity)

    @Delete
    suspend fun deleteAssignment(assignment: AssignmentEntity)

    // Notes
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotesForUser(userId: Long): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    // Expenses
    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY id DESC")
    fun getExpensesForUser(userId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId")
    fun getTotalExpenseAmount(userId: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}

// ==================== DATABASE ====================

@Database(
    entities = [
        UserAccountEntity::class,
        CustomCategoryEntity::class,
        SubjectEntity::class,
        TimetableSlotEntity::class,
        AssignmentEntity::class,
        NoteEntity::class,
        ExpenseEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class LorbitDatabase : RoomDatabase() {
    // ...
    abstract fun dao(): LorbitDao

    companion object {
        @Volatile
        private var INSTANCE: LorbitDatabase? = null

        fun getDatabase(context: Context): LorbitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LorbitDatabase::class.java,
                    "lorbit_college.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    database.dao().insertAccount(
                                        UserAccountEntity(name = "Primary Account", college = "My University", semester = "Semester 1", isActive = true)
                                    )
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}