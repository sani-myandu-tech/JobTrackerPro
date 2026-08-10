package com.jobtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jobtracker.domain.model.ApplicationStatus
import com.jobtracker.domain.model.CvAnalysisResult
import com.jobtracker.domain.model.JobApplication
import java.util.Date

@Entity(tableName = "job_applications")
data class JobApplicationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val companyName: String,
    val jobTitle: String,
    val jobDescription: String,
    val location: String,
    val salary: String,
    val status: String,
    val appliedDate: Long,
    val deadlineDate: Long?,
    val notes: String,
    val jobUrl: String,
    val contactName: String,
    val contactEmail: String,
    val matchScore: Int?,
    val missingSkillsJson: String?,
    val presentSkillsJson: String?,
    val suggestionsJson: String?,
    val analysisSummary: String?,
    val analysedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val isSynced: Boolean = false
)

fun JobApplicationEntity.toDomain(): JobApplication {
    val gson = com.google.gson.Gson()
    val cvAnalysis = if (matchScore != null) CvAnalysisResult(
        matchScore = matchScore,
        missingSkills = missingSkillsJson?.let { gson.fromJson(it, Array<String>::class.java).toList() } ?: emptyList(),
        presentSkills = presentSkillsJson?.let { gson.fromJson(it, Array<String>::class.java).toList() } ?: emptyList(),
        suggestions = suggestionsJson?.let { gson.fromJson(it, Array<String>::class.java).toList() } ?: emptyList(),
        summary = analysisSummary ?: "",
        analysedAt = analysedAt ?: 0L
    ) else null

    return JobApplication(
        id = id,
        userId = userId,
        companyName = companyName,
        jobTitle = jobTitle,
        jobDescription = jobDescription,
        location = location,
        salary = salary,
        status = ApplicationStatus.entries.firstOrNull { it.name == status } ?: ApplicationStatus.APPLIED,
        appliedDate = Date(appliedDate),
        deadlineDate = deadlineDate?.let { Date(it) },
        notes = notes,
        jobUrl = jobUrl,
        contactName = contactName,
        contactEmail = contactEmail,
        cvAnalysisResult = cvAnalysis,
        createdAt = Date(createdAt),
        updatedAt = Date(updatedAt)
    )
}

fun JobApplication.toEntity(): JobApplicationEntity {
    val gson = com.google.gson.Gson()
    return JobApplicationEntity(
        id = id,
        userId = userId,
        companyName = companyName,
        jobTitle = jobTitle,
        jobDescription = jobDescription,
        location = location,
        salary = salary,
        status = status.name,
        appliedDate = appliedDate.time,
        deadlineDate = deadlineDate?.time,
        notes = notes,
        jobUrl = jobUrl,
        contactName = contactName,
        contactEmail = contactEmail,
        matchScore = cvAnalysisResult?.matchScore,
        missingSkillsJson = cvAnalysisResult?.missingSkills?.let { gson.toJson(it) },
        presentSkillsJson = cvAnalysisResult?.presentSkills?.let { gson.toJson(it) },
        suggestionsJson = cvAnalysisResult?.suggestions?.let { gson.toJson(it) },
        analysisSummary = cvAnalysisResult?.summary,
        analysedAt = cvAnalysisResult?.analysedAt,
        createdAt = createdAt.time,
        updatedAt = updatedAt.time,
        isSynced = false
    )
}
