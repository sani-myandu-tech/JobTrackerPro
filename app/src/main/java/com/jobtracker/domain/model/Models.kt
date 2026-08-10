package com.jobtracker.domain.model

data class CvAnalysisResult(
    val matchScore: Int = 0,
    val missingSkills: List<String> = emptyList(),
    val presentSkills: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val summary: String = "",
    val analysedAt: Long = System.currentTimeMillis()
)

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = ""
)

data class AnalyticsData(
    val totalApplications: Int = 0,
    val interviews: Int = 0,
    val offers: Int = 0,
    val rejections: Int = 0,
    val successRate: Float = 0f,
    val weeklyData: List<WeeklyCount> = emptyList(),
    val statusDistribution: Map<ApplicationStatus, Int> = emptyMap()
)

data class WeeklyCount(
    val weekLabel: String,
    val count: Int
)
