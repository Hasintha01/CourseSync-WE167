package com.example.coursesync.shared.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(@PrimaryKey val id: String, val name: String)

@Entity(tableName = "courses")
data class Course(@PrimaryKey val id: String, val title: String, val prerequisiteId: String? = null)

/** Times are minutes after midnight; day uses 1=Monday through 7=Sunday. */
@Entity(tableName = "class_groups")
data class ClassGroup(@PrimaryKey val id: String, val courseId: String, val label: String, val day: Int, val startMinute: Int, val endMinute: Int, val capacity: Int)

@Entity(tableName = "completed_courses", primaryKeys = ["studentId", "courseId"])
data class CompletedCourse(val studentId: String, val courseId: String)

@Entity(tableName = "drafts")
data class Draft(@PrimaryKey val id: String, val studentId: String, val name: String)

@Entity(tableName = "draft_selections", primaryKeys = ["draftId", "courseId"], indices = [Index("groupId")])
data class DraftSelection(val draftId: String, val courseId: String, val groupId: String)

@Entity(tableName = "registrations", indices = [Index(value = ["draftId"], unique = true)])
data class Registration(@PrimaryKey val id: String, val draftId: String, val studentId: String, val confirmedAt: Long)

@Entity(tableName = "registration_selections", primaryKeys = ["registrationId", "courseId"])
data class RegistrationSelection(val registrationId: String, val courseId: String, val groupId: String)

@Entity(tableName = "staff_cases")
data class StaffCase(@PrimaryKey val id: String, val studentId: String, val subject: String, val status: CaseStatus)

enum class CaseStatus { OPEN, IN_REVIEW, RESOLVED }

@Entity(tableName = "guidance_notes")
data class GuidanceNote(@PrimaryKey val id: String, val caseId: String, val text: String, val createdAt: Long, val updatedAt: Long)

@Entity(tableName = "case_history")
data class CaseHistory(@PrimaryKey val id: String, val caseId: String, val fromStatus: CaseStatus?, val toStatus: CaseStatus, val changedAt: Long)
