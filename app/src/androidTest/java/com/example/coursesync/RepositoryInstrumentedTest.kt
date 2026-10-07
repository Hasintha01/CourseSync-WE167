package com.example.coursesync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.coursesync.shared.data.*
import com.example.coursesync.shared.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryInstrumentedTest {
    @Test fun confirmationRejectsDuplicatesAndInvalidPlans() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CourseSyncDatabase::class.java).build()
        try {
            db.dao().putStudents(SampleData.students)
            db.dao().putCourses(SampleData.courses)
            db.dao().putGroups(SampleData.groups)
            SampleData.drafts.forEach { db.dao().putDraft(it) }
            SampleData.selections.forEach { db.dao().putSelection(it) }
            val repository = CourseSyncRepository(db)
            assertTrue(repository.confirmRegistration("D-EMPTY") is ConfirmationResult.Invalid)
            assertTrue(repository.confirmRegistration("D-VALID") is ConfirmationResult.Confirmed)
            assertTrue(repository.confirmRegistration("D-VALID") is ConfirmationResult.AlreadyConfirmed)
            assertEquals(1, repository.registrations("S1").size)
        } finally { db.close() }
    }
}
