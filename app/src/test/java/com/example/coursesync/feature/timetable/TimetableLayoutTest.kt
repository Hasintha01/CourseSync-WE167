package com.example.coursesync.feature.timetable

import com.example.coursesync.shared.model.ClassGroup
import org.junit.Assert.*
import org.junit.Test

class TimetableLayoutTest {
    private fun group(id: String, start: Int, end: Int, day: Int = 1) = ClassGroup(id, id, "A", day, start, end, 20)
    @Test fun adjacentClassesUseFullWidth() {
        val positions = timetablePlacements(listOf(group("A", 540, 600), group("B", 600, 660)))
        assertTrue(positions.all { it.lane == 0 && it.laneCount == 1 })
    }
    @Test fun overlappingClassesRemainVisible() {
        val positions = timetablePlacements(listOf(group("A", 540, 600), group("B", 570, 630)))
        assertEquals(setOf(0, 1), positions.map { it.lane }.toSet())
        assertTrue(positions.all { it.laneCount == 2 })
    }
    @Test fun chainOverlapReusesLanesWithoutHidingSessions() {
        val positions = timetablePlacements(listOf(group("A", 540, 600), group("B", 570, 660), group("C", 600, 630)))
        assertEquals(0, positions.single { it.group.id == "C" }.lane)
        assertTrue(positions.all { it.laneCount == 2 })
    }
    @Test fun daysAreIndependentAndEmptyPlanIsSafe() {
        assertTrue(timetablePlacements(emptyList()).isEmpty())
        assertTrue(timetablePlacements(listOf(group("A", 540, 600), group("B", 540, 600, 7))).all { it.laneCount == 1 })
    }
}
