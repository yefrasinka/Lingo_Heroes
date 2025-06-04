package com.example.lingoheroesapp.models

import com.google.firebase.database.PropertyName

data class Challenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: ChallengeType = ChallengeType.DAILY,
    val requiredValue: Int = 0,
    val currentProgress: Int = 0,
    val reward: Reward = Reward(),
    
    // Obsługa obu nazw pól: isCompleted i completed
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    
    // Obsługa obu nazw pól: isRewardClaimed i rewardClaimed
    @get:PropertyName("isRewardClaimed")
    @set:PropertyName("isRewardClaimed") 
    var isRewardClaimed: Boolean = false,
    
    val expiresAt: Long = 0, // timestamp
    val lastUpdateTime: Long = 0 // timestamp ostatniej aktualizacji postępu
) {
    // Alternatywne nazwy pól dla Firebase
    @PropertyName("completed")
    fun isCompletedAlternative(): Boolean = isCompleted
    
    @PropertyName("completed") 
    fun setCompletedAlternative(value: Boolean) {
        isCompleted = value
    }
    
    @PropertyName("rewardClaimed")
    fun isRewardClaimedAlternative(): Boolean = isRewardClaimed
    
    @PropertyName("rewardClaimed")
    fun setRewardClaimedAlternative(value: Boolean) {
        isRewardClaimed = value
    }
    
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