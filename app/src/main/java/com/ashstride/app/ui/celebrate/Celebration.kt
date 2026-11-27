package com.ashstride.app.ui.celebrate

import com.ashstride.app.domain.Challenge
import com.ashstride.app.domain.FlameInfo
import com.ashstride.app.domain.LevelInfo
import com.ashstride.app.domain.ShardReward
import com.ashstride.app.domain.Waypoint

/** Something worth stopping for. Detected in AppModel by comparing the state with what has already been shown. */
sealed interface Celebration {
    data class LevelUp(val level: LevelInfo, val tierChanged: Boolean) : Celebration
    data class FlameStage(val info: FlameInfo, val streak: Int) : Celebration
    data class WaypointReached(val waypoint: Waypoint, val chapterTitle: String) : Celebration
    data class ChallengeWon(val challenge: Challenge) : Celebration
    data class ShardsEarned(val reward: ShardReward) : Celebration
}
