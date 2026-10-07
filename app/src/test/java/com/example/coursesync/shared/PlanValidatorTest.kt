package com.example.coursesync.shared

import com.example.coursesync.shared.data.SampleData
import com.example.coursesync.shared.validation.*
import org.junit.Assert.*
import org.junit.Test

class PlanValidatorTest {
    private fun validate(id: String) = PlanValidator.validate(
        SampleData.selections.filter { it.draftId == id }, SampleData.groups, SampleData.courses,
        emptySet(), emptyMap()
    )

    @Test fun documentedScenarios() {
        assertTrue(validate("D-VALID").isValid)
        assertEquals(IssueType.TIME_OVERLAP, validate("D-OVERLAP").issues.single().type)
        assertEquals(setOf("CS101-A", "MA101-B"), validate("D-OVERLAP").issues.single().groupIds)
        assertEquals(IssueType.MISSING_PREREQUISITE, validate("D-PREREQ").issues.single().type)
        assertEquals(IssueType.FULL_GROUP, validate("D-FULL").issues.single().type)
        assertEquals(IssueType.EMPTY_SELECTION, validate("D-EMPTY").issues.single().type)
    }

    @Test fun adjacentClassesDoNotClash() {
        assertTrue(validate("D-VALID").issues.none { it.type == IssueType.TIME_OVERLAP })
    }
}
