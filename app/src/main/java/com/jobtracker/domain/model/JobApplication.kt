package com.jobtracker.domain.model

import java.util.Date

data class JobApplication(
    val id: String = "",
    val userId: String = "",
    val companyName: String = "",
    val jobTitle: String = "",
    val jobDescription: String = "",
    val location: String = "",
    val salary: String = "",
    val status: ApplicationStatus = ApplicationStatus.APPLIED,
    val appliedDate: Date = Date(),
    val deadlineDate: Date? = null,
    val notes: String = "",
    val jobUrl: String = "",
    val contactName: String = "",
    val contactEmail: String = "",
    val cvAnalysisResult: CvAnalysisResult? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class ApplicationStatus(val displayName: String, val color: Long) {
    APPLIED("Applied", 0xFF2196F3),
    INTERVIEW("Interview", 0xFF9C27B0),
    OFFER("Offer", 0xFF4CAF50),
    REJECTED("Rejected", 0xFFF44336),
    WITHDRAWN("Withdrawn", 0xFF9E9E9E),
    SAVED("Saved", 0xFFFF9800)
}
