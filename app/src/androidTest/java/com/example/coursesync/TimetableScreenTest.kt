package com.example.coursesync

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.coursesync.feature.timetable.WeeklyTimetableScreen
import com.example.coursesync.shared.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimetableScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: CourseSyncDatabase
    private lateinit var repository: CourseSyncRepository
    @Before fun seed() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CourseSyncDatabase::class.java).build()
        db.dao().putStudents(SampleData.students)
        db.dao().putCourses(SampleData.courses)
        db.dao().putGroups(SampleData.groups)
        SampleData.drafts.forEach { db.dao().putDraft(it) }
        SampleData.selections.forEach { db.dao().putSelection(it) }
        SampleData.registrations.forEach { db.dao().putRegistration(it) }
        db.dao().putRegistrationSelections(SampleData.registrationSelections)
        repository = CourseSyncRepository(db)
    }
    @After fun close() { db.close() }
    private fun open(id: String) {
        compose.setContent { MaterialTheme { WeeklyTimetableScreen(repository, id, {}, {}, {}) } }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Offline demonstration", substring = true).fetchSemanticsNodes().any {
            it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Text)
        } && compose.onAllNodesWithText("No active draft", substring = true).fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
    }
    @Test fun clashCanBeResolvedThroughGroupPicker() {
        open("D-OVERLAP")
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Resolve clash · MA101").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Monday overlap: 09:30–10:00").assertExists()
        compose.onNodeWithText("Resolve clash · MA101").performScrollTo().performClick()
        compose.onNodeWithText("Choose group A").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Clash resolved. Your updated week is saved.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("No timetable clashes", substring = true).assertExists()
        runBlocking { Assert.assertEquals("MA101-A", repository.reopenDraft("D-OVERLAP")!!.second.single { it.courseId == "MA101" }.groupId) }
    }
    @Test fun emptyTimetableOffersCourseSelection() {
        open("D-EMPTY")
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Your week starts here").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Browse modules").assertExists()
    }
    @Test fun confirmedSnapshotHasNoMutationControls() {
        open("D-SEEDED")
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Network Basics").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Network Basics").assertExists()
        compose.onNodeWithText("Change group").assertDoesNotExist()
        compose.onNodeWithText("Remove class").assertDoesNotExist()
    }
}
