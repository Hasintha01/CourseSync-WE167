package com.example.coursesync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.coursesync.shared.data.*
import com.example.coursesync.shared.model.*
import com.example.coursesync.shared.validation.IssueType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import android.content.Context
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TimetableRepositoryTest {
    @Test fun resolveRemoveAndAddSurviveDatabaseReopening() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "timetable-test-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, CourseSyncDatabase::class.java, name).build()
        var db = open()
        try {
            db.dao().putStudents(SampleData.students)
            db.dao().putCourses(SampleData.courses)
            db.dao().putGroups(SampleData.groups)
            SampleData.drafts.forEach { db.dao().putDraft(it) }
            SampleData.selections.forEach { db.dao().putSelection(it) }
            var repository = CourseSyncRepository(db)
            assertTrue(repository.validateDraft("D-OVERLAP").issues.any { it.type == IssueType.TIME_OVERLAP })
            repository.changeSelection("D-OVERLAP", "MA101", "MA101-A")
            assertTrue(repository.validateDraft("D-OVERLAP").isValid)
            db.close()
            db = open()
            repository = CourseSyncRepository(db)
            assertEquals("MA101-A", repository.reopenDraft("D-OVERLAP")!!.second.single { it.courseId == "MA101" }.groupId)
            repository.removeSelection("D-OVERLAP", "MA101")
            repository.removeSelection("D-OVERLAP", "CS101")
            assertEquals(IssueType.EMPTY_SELECTION, repository.validateDraft("D-OVERLAP").issues.single().type)
            repository.addSelection("D-OVERLAP", "HI101", "HI101-A")
            val registration = (repository.confirmRegistration("D-OVERLAP") as ConfirmationResult.Confirmed).registration
            assertEquals("HI101-A", repository.registrationSelections(registration.id).single().groupId)
            try {
                repository.removeSelection("D-OVERLAP", "HI101")
                fail("Confirmed timetable must remain read only")
            } catch (_: IllegalArgumentException) { }
            assertEquals(1, repository.registrationSelections(registration.id).size)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
