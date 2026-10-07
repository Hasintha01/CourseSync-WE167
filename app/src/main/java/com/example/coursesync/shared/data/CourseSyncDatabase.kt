package com.example.coursesync.shared.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.coursesync.shared.model.*

@Dao
interface CourseSyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putStudents(items: List<Student>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCourses(items: List<Course>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putGroups(items: List<ClassGroup>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCompleted(items: List<CompletedCourse>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDraft(draft: Draft)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSelection(selection: DraftSelection)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun putRegistration(registration: Registration)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun putRegistrationSelections(items: List<RegistrationSelection>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCases(items: List<StaffCase>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putNote(note: GuidanceNote)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putHistory(item: CaseHistory)

    @Query("SELECT * FROM students") suspend fun students(): List<Student>
    @Query("SELECT * FROM courses") suspend fun courses(): List<Course>
    @Query("SELECT * FROM class_groups") suspend fun groups(): List<ClassGroup>
    @Query("SELECT courseId FROM completed_courses WHERE studentId = :studentId") suspend fun completed(studentId: String): List<String>
    @Query("SELECT * FROM drafts WHERE studentId = :studentId") suspend fun drafts(studentId: String): List<Draft>
    @Query("SELECT * FROM drafts WHERE id = :id") suspend fun draft(id: String): Draft?
    @Query("SELECT * FROM draft_selections WHERE draftId = :draftId") suspend fun selections(draftId: String): List<DraftSelection>
    @Query("DELETE FROM draft_selections WHERE draftId = :draftId AND courseId = :courseId") suspend fun removeSelection(draftId: String, courseId: String)
    @Query("DELETE FROM draft_selections WHERE draftId = :draftId") suspend fun clearSelections(draftId: String)
    @Query("DELETE FROM drafts WHERE id = :id") suspend fun deleteDraft(id: String)
    @Query("SELECT * FROM registrations WHERE draftId = :draftId LIMIT 1") suspend fun registrationForDraft(draftId: String): Registration?
    @Query("SELECT * FROM registrations WHERE studentId = :studentId") suspend fun registrations(studentId: String): List<Registration>
    @Query("SELECT groupId, COUNT(*) AS seats FROM registration_selections GROUP BY groupId") suspend fun seatCounts(): List<SeatCount>
    @Query("SELECT * FROM staff_cases") suspend fun cases(): List<StaffCase>
    @Query("SELECT * FROM staff_cases WHERE id = :id") suspend fun caseById(id: String): StaffCase?
    @Query("SELECT * FROM guidance_notes WHERE caseId = :caseId") suspend fun notes(caseId: String): List<GuidanceNote>
    @Query("DELETE FROM guidance_notes WHERE id = :id") suspend fun deleteNote(id: String)
    @Query("SELECT * FROM case_history WHERE caseId = :caseId ORDER BY changedAt") suspend fun history(caseId: String): List<CaseHistory>
}

data class SeatCount(val groupId: String, val seats: Int)

@Database(entities = [Student::class, Course::class, ClassGroup::class, CompletedCourse::class, Draft::class,
    DraftSelection::class, Registration::class, RegistrationSelection::class, StaffCase::class,
    GuidanceNote::class, CaseHistory::class], version = 1, exportSchema = false)
@TypeConverters(CaseStatusConverter::class)
abstract class CourseSyncDatabase : RoomDatabase() {
    abstract fun dao(): CourseSyncDao

    companion object {
        @Volatile private var instance: CourseSyncDatabase? = null
        fun get(context: Context): CourseSyncDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, CourseSyncDatabase::class.java, "coursesync.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        SampleData.seed(db)
                    }
                }).build().also { instance = it }
        }
    }
}

class CaseStatusConverter {
    @TypeConverter fun toStatus(value: String): CaseStatus = CaseStatus.valueOf(value)
    @TypeConverter fun fromStatus(value: CaseStatus): String = value.name
}
