package com.example.coursesync.shared.validation

import com.example.coursesync.shared.model.*

enum class IssueType { EMPTY_SELECTION, UNKNOWN_GROUP, MISSING_PREREQUISITE, FULL_GROUP, TIME_OVERLAP }

data class ValidationIssue(val type: IssueType, val courseIds: Set<String> = emptySet(), val groupIds: Set<String> = emptySet())
data class ValidationResult(val issues: List<ValidationIssue>) { val isValid: Boolean get() = issues.isEmpty() }

object PlanValidator {
    fun validate(
        selections: List<DraftSelection>, groups: List<ClassGroup>, courses: List<Course>,
        completedCourseIds: Set<String>, occupiedSeats: Map<String, Int>
    ): ValidationResult {
        if (selections.isEmpty()) return ValidationResult(listOf(ValidationIssue(IssueType.EMPTY_SELECTION)))
        val groupById = groups.associateBy { it.id }
        val courseById = courses.associateBy { it.id }
        val issues = mutableListOf<ValidationIssue>()
        selections.forEach { selection ->
            val group = groupById[selection.groupId]
            if (group == null || group.courseId != selection.courseId) {
                issues += ValidationIssue(IssueType.UNKNOWN_GROUP, setOf(selection.courseId), setOf(selection.groupId))
            } else if ((occupiedSeats[group.id] ?: 0) >= group.capacity) {
                issues += ValidationIssue(IssueType.FULL_GROUP, setOf(selection.courseId), setOf(group.id))
            }
            val prerequisite = courseById[selection.courseId]?.prerequisiteId
            if (prerequisite != null && prerequisite !in completedCourseIds) {
                issues += ValidationIssue(IssueType.MISSING_PREREQUISITE, setOf(selection.courseId, prerequisite), setOf(selection.groupId))
            }
        }
        selections.forEachIndexed { index, first ->
            val a = groupById[first.groupId] ?: return@forEachIndexed
            selections.drop(index + 1).forEach { second ->
                val b = groupById[second.groupId] ?: return@forEach
                if (a.day == b.day && a.startMinute < b.endMinute && b.startMinute < a.endMinute) {
                    issues += ValidationIssue(IssueType.TIME_OVERLAP, setOf(first.courseId, second.courseId), setOf(a.id, b.id))
                }
            }
        }
        return ValidationResult(issues)
    }
}
