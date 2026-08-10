package com.jobtracker.domain.repository

import com.jobtracker.domain.model.CvAnalysisResult
import com.jobtracker.domain.model.JobApplication
import com.jobtracker.domain.model.User
import kotlinx.coroutines.flow.Flow

interface JobRepository {
    fun getJobs(): Flow<List<JobApplication>>
    fun getJobById(id: String): Flow<JobApplication?>
    suspend fun insertJob(job: JobApplication): String
    suspend fun updateJob(job: JobApplication)
    suspend fun deleteJob(id: String)
    suspend fun syncJobsFromCloud()
    fun searchJobs(query: String): Flow<List<JobApplication>>
    fun getJobsByStatus(status: String): Flow<List<JobApplication>>
}

interface AuthRepository {
    val currentUser: User?
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, displayName: String): Result<User>
    suspend fun signOut()
    fun isLoggedIn(): Boolean
}

interface AiRepository {
    suspend fun analyseCv(cvText: String, jobDescription: String): Result<CvAnalysisResult>
}

interface StorageRepository {
    suspend fun uploadCv(fileBytes: ByteArray, fileName: String): Result<String>
    suspend fun extractTextFromPdf(fileBytes: ByteArray): Result<String>
}
