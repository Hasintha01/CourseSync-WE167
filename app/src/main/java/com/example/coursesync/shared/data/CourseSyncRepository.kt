package com.example.coursesync.shared.data

import androidx.room.withTransaction
import com.example.coursesync.shared.model.*
import com.example.coursesync.shared.validation.*
import java.util.UUID

sealed interface ConfirmationResult {
    data class Confirmed(val registration: Registration) : ConfirmationResult
    data class Invalid(val issues: List<ValidationIssue>) : ConfirmationResult
    data class AlreadyConfirmed(val registration: Registration) : ConfirmationResult
}

/** One shared entry point for the four feature packages. All mutations are persisted in Room. */
class CourseSyncRepository(private val database: CourseSyncDatabase) {
    private val dao get() = database.dao()

    suspend fun students() = dao.students()
    suspend fun courses() = dao.courses()
    suspend fun groups() = dao.groups()
    suspend fun drafts(studentId: String) = dao.drafts(studentId)
    suspend fun reopenDraft(draftId: String): Pair<Draft, List<DraftSelection>>? =
        dao.draft(draftId)?.let { it to dao.selections(draftId) }
    suspend fun registrations(studentId: String) = dao.registrations(studentId)
    suspend fun cases() = dao.cases()
    suspend fun notes(caseId: String) = dao.notes(caseId)
    suspend fun caseHistory(caseId: String) = dao.history(caseId)

    suspend fun saveDraft(studentId: String, name: String, selections: List<DraftSelection> = emptyList()): Draft {
        require(name.isNotBlank())
        val draft = Draft(UUID.randomUUID().toString(), studentId, name.trim())
        database.withTransaction {
            require(dao.students().any { it.id == studentId })
            dao.putDraft(draft)
            selections.forEach { addSelectionInternal(draft.id, it.courseId, it.groupId) }
        }
        return draft
    }

    suspend fun renameDraft(draftId: String, name: String) = database.withTransaction {
        require(name.isNotBlank())
        val draft = requireNotNull(dao.draft(draftId))
        require(dao.registrationForDraft(draftId) == null) { "Confirmed draft cannot change" }
        dao.putDraft(draft.copy(name = name.trim()))
    }

    suspend fun deleteDraft(draftId: String) = database.withTransaction {
        requireNotNull(dao.draft(draftId))
        require(dao.registrationForDraft(draftId) == null) { "Confirmed draft cannot be deleted" }
        dao.clearSelections(draftId)
        dao.deleteDraft(draftId)
    }

    suspend fun addSelection(draftId: String, courseId: String, groupId: String) = database.withTransaction {
        addSelectionInternal(draftId, courseId, groupId)
    }

    suspend fun changeSelection(draftId: String, courseId: String, newGroupId: String) = database.withTransaction {
        require(dao.selections(draftId).any { it.courseId == courseId })
        addSelectionInternal(draftId, courseId, newGroupId)
    }

    suspend fun removeSelection(draftId: String, courseId: String) = database.withTransaction {
        requireNotNull(dao.draft(draftId))
        require(dao.registrationForDraft(draftId) == null) { "Confirmed draft cannot change" }
        dao.removeSelection(draftId, courseId)
    }

    private suspend fun addSelectionInternal(draftId: String, courseId: String, groupId: String) {
        requireNotNull(dao.draft(draftId))
        require(dao.registrationForDraft(draftId) == null) { "Confirmed draft cannot change" }
        require(dao.courses().any { it.id == courseId })
        require(dao.groups().any { it.id == groupId && it.courseId == courseId })
        dao.putSelection(DraftSelection(draftId, courseId, groupId))
    }

    suspend fun validateDraft(draftId: String): ValidationResult = database.withTransaction {
        val draft = requireNotNull(dao.draft(draftId))
        PlanValidator.validate(dao.selections(draftId), dao.groups(), dao.courses(),
            dao.completed(draft.studentId).toSet(), dao.seatCounts().associate { it.groupId to it.seats })
    }

    suspend fun confirmRegistration(draftId: String, confirmedAt: Long = System.currentTimeMillis()): ConfirmationResult = database.withTransaction {
        val draft = requireNotNull(dao.draft(draftId))
        dao.registrationForDraft(draftId)?.let { return@withTransaction ConfirmationResult.AlreadyConfirmed(it) }
        val selections = dao.selections(draftId)
        val result = PlanValidator.validate(selections, dao.groups(), dao.courses(),
            dao.completed(draft.studentId).toSet(), dao.seatCounts().associate { it.groupId to it.seats })
        if (!result.isValid) return@withTransaction ConfirmationResult.Invalid(result.issues)
        val registration = Registration(UUID.randomUUID().toString(), draftId, draft.studentId, confirmedAt)
        dao.putRegistration(registration)
        dao.putRegistrationSelections(selections.map { RegistrationSelection(registration.id, it.courseId, it.groupId) })
        ConfirmationResult.Confirmed(registration)
    }

    suspend fun addGuidanceNote(caseId: String, text: String, now: Long = System.currentTimeMillis()): GuidanceNote {
        require(text.isNotBlank())
        val note = GuidanceNote(UUID.randomUUID().toString(), caseId, text.trim(), now, now)
        database.withTransaction { requireNotNull(dao.caseById(caseId)); dao.putNote(note) }
        return note
    }

    suspend fun updateGuidanceNote(note: GuidanceNote, text: String, now: Long = System.currentTimeMillis()) {
        require(text.isNotBlank())
        database.withTransaction {
            require(dao.notes(note.caseId).any { it.id == note.id })
            dao.putNote(note.copy(text = text.trim(), updatedAt = now))
        }
    }

    suspend fun deleteGuidanceNote(caseId: String, noteId: String) = database.withTransaction {
        require(dao.notes(caseId).any { it.id == noteId })
        dao.deleteNote(noteId)
    }

    suspend fun updateCaseStatus(caseId: String, status: CaseStatus, now: Long = System.currentTimeMillis()) = database.withTransaction {
        val current = requireNotNull(dao.caseById(caseId))
        if (current.status != status) {
            dao.putCases(listOf(current.copy(status = status)))
            dao.putHistory(CaseHistory(UUID.randomUUID().toString(), caseId, current.status, status, now))
        }
    }
}
