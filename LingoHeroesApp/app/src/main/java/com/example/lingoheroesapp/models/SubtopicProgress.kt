data class SubtopicProgress(
    val title: String = "",
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val lastTaskIndex: Int = 0  // Dodane pole do śledzenia ostatniego zadania
) 