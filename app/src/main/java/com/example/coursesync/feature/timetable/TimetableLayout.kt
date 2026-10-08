package com.example.coursesync.feature.timetable

import com.example.coursesync.shared.model.ClassGroup

internal val timetableDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
internal fun timetableTime(minute: Int): String {
    val hours = minute / 60
    val minutes = minute % 60
    return "%02d:%02d".format(hours, minutes)
}

/** Separate overlapping blocks into lanes; adjacent sessions may share a lane. */
internal data class SessionPlacement(val group: ClassGroup, val lane: Int, val laneCount: Int)
internal fun timetablePlacements(groups: List<ClassGroup>): List<SessionPlacement> = groups.groupBy { it.day }.values.flatMap { day ->
    val result = mutableListOf<SessionPlacement>()
    val cluster = mutableListOf<ClassGroup>()
    var clusterEnd = -1
    fun flushCluster() {
        // Track when each lane becomes available for the next class.
        val laneEndMinutes = mutableListOf<Int>()
        val lanes = cluster.map { group ->
            val available = laneEndMinutes.indexOfFirst { it <= group.startMinute }
            val lane = if (available < 0) laneEndMinutes.size.also { laneEndMinutes.add(group.endMinute) }
                else available.also { laneEndMinutes[it] = group.endMinute }
            group to lane
        }
        result += lanes.map { (group, lane) -> SessionPlacement(group, lane, laneEndMinutes.size) }
        cluster.clear()
    }
    day.sortedWith(compareBy<ClassGroup> { it.startMinute }.thenBy { it.id }).forEach { group ->
        if (cluster.isNotEmpty() && group.startMinute >= clusterEnd) flushCluster()
        if (cluster.isEmpty()) clusterEnd = group.endMinute else clusterEnd = maxOf(clusterEnd, group.endMinute)
        cluster += group
    }
    if (cluster.isNotEmpty()) flushCluster()
    result
}
