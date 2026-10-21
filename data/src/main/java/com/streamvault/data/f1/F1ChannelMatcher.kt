package com.streamvault.data.f1

import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.F1ChannelGroup
import com.streamvault.domain.model.F1DetectedChannel
import com.streamvault.domain.model.F1FeedType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class F1ChannelMatcher @Inject constructor() {

    private val includePatterns = listOf(
        "formula 1", "formula1", "formule 1", "formule1",
        "f1 tv", "f1tv", "f1 tv pro", "f1tvpro",
        "sky f1", "sky sports f1",
        "grand prix",
        "viaplay f1", "viaplay formule 1", "viaplay formula 1",
        "olav mol"
    )

    // "f1" alone is checked separately to avoid matching e.g. "wifi1"
    private val f1StandalonePattern = Regex("""(?<![a-z0-9])f1(?![a-z0-9])""")

    private val excludePatterns = listOf(
        "ryan hamilton", "nicky verstappen", "nick verstappen"
    )

    fun match(channels: List<Channel>): F1ChannelGroup {
        val matched = channels
            .filter { isF1Channel(it.name) }
            .map { F1DetectedChannel(it, detectFeedType(it.name)) }

        return F1ChannelGroup(
            main = matched.filter { it.feedType == F1FeedType.MAIN },
            onboard = matched.filter { it.feedType == F1FeedType.ONBOARD },
            pitLane = matched.filter { it.feedType == F1FeedType.PIT_LANE }
        )
    }

    private fun isF1Channel(name: String): Boolean {
        val lower = name.lowercase()
        if (excludePatterns.any { lower.contains(it) }) return false
        if (includePatterns.any { lower.contains(it) }) return true
        return f1StandalonePattern.containsMatchIn(lower)
    }

    private fun detectFeedType(name: String): F1FeedType {
        val lower = name.lowercase()
        return when {
            lower.contains("onboard") || lower.contains("on board") -> F1FeedType.ONBOARD
            lower.contains("pitlane") || lower.contains("pit lane") -> F1FeedType.PIT_LANE
            else -> F1FeedType.MAIN
        }
    }
}
