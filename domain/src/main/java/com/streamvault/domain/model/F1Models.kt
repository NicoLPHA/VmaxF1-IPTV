package com.streamvault.domain.model

enum class F1SessionType {
    FP1, FP2, FP3, SPRINT_QUALIFYING, SPRINT, QUALIFYING, RACE
}

enum class F1SessionStatus {
    SCHEDULED, LIVE, COMPLETED
}

enum class F1FeedType {
    MAIN, ONBOARD, PIT_LANE
}

data class F1Session(
    val type: F1SessionType,
    val startTimeUtcMs: Long,
    val durationMs: Long,
    val status: F1SessionStatus
)

data class F1RaceWeekend(
    val round: Int,
    val raceName: String,
    val circuitName: String,
    val country: String,
    val sessions: List<F1Session>
)

data class F1Calendar(
    val season: Int,
    val races: List<F1RaceWeekend>,
    val fetchedAtMs: Long
)

data class F1DetectedChannel(
    val channel: Channel,
    val feedType: F1FeedType
)

data class F1ChannelGroup(
    val main: List<F1DetectedChannel>,
    val onboard: List<F1DetectedChannel>,
    val pitLane: List<F1DetectedChannel>
) {
    val isEmpty: Boolean get() = main.isEmpty() && onboard.isEmpty() && pitLane.isEmpty()
    val totalCount: Int get() = main.size + onboard.size + pitLane.size
}
