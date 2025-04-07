package com.example.lingoheroesapp.models

data class Challenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: ChallengeType = ChallengeType.DAILY,
    val requiredValue: Int = 0,
    val currentProgress: Int = 0,
    val reward: Reward = Reward(),
    var isCompleted: Boolean = false,
    var isRewardClaimed: Boolean = false,
    val expiresAt: Long = 0, // timestamp
    val lastUpdateTime: Long = 0 // timestamp ostatniej aktualizacji postępu
) {
    // Wymagany przez Firebase bezargumentowy konstruktor
    constructor() : this(
        id = "",
        title = "",
        description = "",
        type = ChallengeType.DAILY,
        requiredValue = 0,
        currentProgress = 0,
        reward = Reward(),
        isCompleted = false,
        isRewardClaimed = false,
        expiresAt = 0,
        lastUpdateTime = 0
    )
}

data class Reward(
    val coins: Int = 0
) {
    // Wymagany przez Firebase
    constructor() : this(0)
}

enum class ChallengeType {
    DAILY,
    WEEKLY
} 