package com.example.coursesync.shared.data

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.coursesync.shared.model.*

/** Canonical demonstration dataset. See PROJECT-CONTRACT.md for the five repeatable scenarios. */
object SampleData {
    val students = listOf(Student("S1", "Asha Perera"))
    val courses = listOf(
        Course("CS101", "Programming Fundamentals"), Course("CS201", "Data Structures", "CS101"),
        Course("MA101", "Discrete Mathematics"), Course("UX101", "Interface Design"),
        Course("NW101", "Network Basics"), Course("HI101", "Academic Writing")
    )
    val groups = listOf(
        ClassGroup("CS101-A", "CS101", "A", 1, 540, 600, 20),
        ClassGroup("CS201-A", "CS201", "A", 2, 540, 600, 20),
        ClassGroup("MA101-A", "MA101", "A", 1, 600, 660, 20),
        ClassGroup("MA101-B", "MA101", "B", 1, 570, 630, 20),
        ClassGroup("UX101-A", "UX101", "A", 3, 540, 600, 0),
        ClassGroup("NW101-A", "NW101", "A", 4, 540, 600, 20),
        ClassGroup("HI101-A", "HI101", "A", 5, 540, 600, 20)
    )
    val drafts = listOf(
        Draft("D-VALID", "S1", "Valid plan"), Draft("D-OVERLAP", "S1", "Overlapping classes"),
        Draft("D-PREREQ", "S1", "Missing prerequisite"), Draft("D-FULL", "S1", "Full group"),
        Draft("D-EMPTY", "S1", "Empty plan"), Draft("D-SEEDED", "S1", "Confirmed example")
    )
    val selections = listOf(
        DraftSelection("D-VALID", "CS101", "CS101-A"), DraftSelection("D-VALID", "MA101", "MA101-A"),
        DraftSelection("D-OVERLAP", "CS101", "CS101-A"), DraftSelection("D-OVERLAP", "MA101", "MA101-B"),
        DraftSelection("D-PREREQ", "CS201", "CS201-A"), DraftSelection("D-FULL", "UX101", "UX101-A"),
        DraftSelection("D-SEEDED", "NW101", "NW101-A")
    )
    val registrations = listOf(Registration("REG-1", "D-SEEDED", "S1", 1L))
    val registrationSelections = listOf(RegistrationSelection("REG-1", "NW101", "NW101-A"))
    val cases = listOf(StaffCase("CASE-1", "S1", "Plan guidance", CaseStatus.OPEN))
    val notes = listOf(GuidanceNote("NOTE-1", "CASE-1", "Check the published course requirements.", 1L, 1L))
    val history = listOf(CaseHistory("HISTORY-1", "CASE-1", null, CaseStatus.OPEN, 1L))

    fun seed(db: SupportSQLiteDatabase) {
        db.beginTransaction()
        try {
            students.forEach { db.execSQL("INSERT INTO students VALUES (?, ?)", arrayOf(it.id, it.name)) }
            courses.forEach { db.execSQL("INSERT INTO courses VALUES (?, ?, ?)", arrayOf<Any?>(it.id, it.title, it.prerequisiteId)) }
            groups.forEach { db.execSQL("INSERT INTO class_groups VALUES (?, ?, ?, ?, ?, ?, ?)", arrayOf<Any?>(it.id, it.courseId, it.label, it.day, it.startMinute, it.endMinute, it.capacity)) }
            drafts.forEach { db.execSQL("INSERT INTO drafts VALUES (?, ?, ?)", arrayOf(it.id, it.studentId, it.name)) }
            selections.forEach { db.execSQL("INSERT INTO draft_selections VALUES (?, ?, ?)", arrayOf(it.draftId, it.courseId, it.groupId)) }
            registrations.forEach { db.execSQL("INSERT INTO registrations VALUES (?, ?, ?, ?)", arrayOf<Any?>(it.id, it.draftId, it.studentId, it.confirmedAt)) }
            registrationSelections.forEach { db.execSQL("INSERT INTO registration_selections VALUES (?, ?, ?)", arrayOf(it.registrationId, it.courseId, it.groupId)) }
            cases.forEach { db.execSQL("INSERT INTO staff_cases VALUES (?, ?, ?, ?)", arrayOf<Any?>(it.id, it.studentId, it.subject, it.status.name)) }
            notes.forEach { db.execSQL("INSERT INTO guidance_notes VALUES (?, ?, ?, ?, ?)", arrayOf<Any?>(it.id, it.caseId, it.text, it.createdAt, it.updatedAt)) }
            history.forEach { db.execSQL("INSERT INTO case_history VALUES (?, ?, ?, ?, ?)", arrayOf<Any?>(it.id, it.caseId, it.fromStatus?.name, it.toStatus.name, it.changedAt)) }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}
