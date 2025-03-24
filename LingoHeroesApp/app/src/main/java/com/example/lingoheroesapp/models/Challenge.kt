package com.example.lingoheroesapp.models

data class Challenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: ChallengeType = ChallengeType.DAILY,
    val requiredValue: Int = 0,
    val currentProgress: Int = 0,
    val reward: Reward = Reward(),
    val isCompleted: Boolean = false,
    val isRewardClaimed: Boolean = false,
    val expiresAt: Long = 0, // timestamp
    val lastUpdateTime: Long = 0 // timestamp ostatniej aktualizacji postępu
)

data class Reward(
    val coins: Int = 0
)

enum class ChallengeType {
    DAILY,
    WEEKLY
} 