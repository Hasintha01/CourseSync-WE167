package com.example.coursesync.feature.timetable

import com.example.coursesync.shared.model.ClassGroup

internal val timetableDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
internal fun timetableTime(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

/** Separate overlapping blocks into lanes; adjacent sessions may share a lane. */
internal data class SessionPlacement(val group: ClassGroup, val lane: Int, val laneCount: Int)
internal fun timetablePlacements(groups: List<ClassGroup>): List<SessionPlacement> = groups.groupBy { it.day }.values.flatMap { day ->
    val result = mutableListOf<SessionPlacement>()
    val cluster = mutableListOf<ClassGroup>()
    var clusterEnd = -1
    fun flush() {
        val ends = mutableListOf<Int>()
        val lanes = cluster.map { group ->
            val available = ends.indexOfFirst { it <= group.startMinute }
            val lane = if (available < 0) ends.size.also { ends.add(group.endMinute) }
                else available.also { ends[it] = group.endMinute }
            group to lane
        }
        result += lanes.map { (group, lane) -> SessionPlacement(group, lane, ends.size) }
        cluster.clear()
    }
    day.sortedWith(compareBy<ClassGroup> { it.startMinute }.thenBy { it.id }).forEach { group ->
        if (cluster.isNotEmpty() && group.startMinute >= clusterEnd) flush()
        if (cluster.isEmpty()) clusterEnd = group.endMinute else clusterEnd = maxOf(clusterEnd, group.endMinute)
        cluster += group
    }
    if (cluster.isNotEmpty()) flush()
    result
}
