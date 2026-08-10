package com.jobtracker.data.repository

import com.jobtracker.BuildConfig
import com.jobtracker.data.local.dao.JobApplicationDao
import com.jobtracker.data.local.entity.toDomain
import com.jobtracker.data.local.entity.toEntity
import com.jobtracker.data.remote.api.AnalysisJson
import com.jobtracker.data.remote.api.OpenAiMessage
import com.jobtracker.data.remote.api.OpenAiRequest
import com.jobtracker.data.remote.api.OpenAiService
import com.jobtracker.data.remote.api.ResponseFormat
import com.jobtracker.data.remote.firebase.FirebaseAuthDataSource
import com.jobtracker.data.remote.firebase.FirebaseFirestoreDataSource
import com.jobtracker.data.remote.firebase.FirebaseStorageDataSource
import com.jobtracker.domain.model.ApplicationStatus
import com.jobtracker.domain.model.CvAnalysisResult
import com.jobtracker.domain.model.JobApplication
import com.jobtracker.domain.model.User
import com.jobtracker.domain.repository.AiRepository
import com.jobtracker.domain.repository.AuthRepository
import com.jobtracker.domain.repository.JobRepository
import com.jobtracker.domain.repository.StorageRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepositoryImpl @Inject constructor(
    private val dao: JobApplicationDao,
    private val firestoreSource: FirebaseFirestoreDataSource,
    private val authSource: FirebaseAuthDataSource
) : JobRepository {

    override fun getJobs(): Flow<List<JobApplication>> =
        dao.getAllJobs().map { it.map { e -> e.toDomain() } }

    override fun getJobById(id: String): Flow<JobApplication?> =
        dao.getJobById(id).map { it?.toDomain() }

    override suspend fun insertJob(job: JobApplication): String {
        val id = UUID.randomUUID().toString()
        val userId = authSource.currentUser?.uid ?: ""
        val newJob = job.copy(id = id, userId = userId, createdAt = Date(), updatedAt = Date())
        dao.insertJob(newJob.toEntity())
        try {
            firestoreSource.saveJob(userId, newJob.toFirestoreMap())
            dao.markAsSynced(id)
        } catch (e: Exception) { /* Will sync via WorkManager */ }
        return id
    }

    override suspend fun updateJob(job: JobApplication) {
        val updated = job.copy(updatedAt = Date())
        dao.updateJob(updated.toEntity())
        try {
            val userId = authSource.currentUser?.uid ?: return
            firestoreSource.saveJob(userId, updated.toFirestoreMap())
            dao.markAsSynced(job.id)
        } catch (e: Exception) { /* Will sync via WorkManager */ }
    }

    override suspend fun deleteJob(id: String) {
        dao.deleteJob(id)
        try {
            val userId = authSource.currentUser?.uid ?: return
            firestoreSource.deleteJob(userId, id)
        } catch (e: Exception) { /* ignored */ }
    }

    override suspend fun syncJobsFromCloud() {
        val userId = authSource.currentUser?.uid ?: return
        val cloudJobs = firestoreSource.getJobs(userId)
        val entities = cloudJobs.mapNotNull { map ->
            try { map.toJobApplicationEntity() } catch (e: Exception) { null }
        }
        dao.insertJobs(entities.map { it.copy(isSynced = true) })
    }

    override fun searchJobs(query: String): Flow<List<JobApplication>> =
        dao.searchJobs(query).map { it.map { e -> e.toDomain() } }

    override fun getJobsByStatus(status: String): Flow<List<JobApplication>> =
        dao.getJobsByStatus(status).map { it.map { e -> e.toDomain() } }
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authSource: FirebaseAuthDataSource
) : AuthRepository {
    override val currentUser: User? get() = authSource.currentUser

    override suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        authSource.signIn(email, password)
    }

    override suspend fun register(email: String, password: String, displayName: String): Result<User> = runCatching {
        authSource.register(email, password, displayName)
    }

    override suspend fun signOut() = authSource.signOut()
    override fun isLoggedIn() = authSource.isLoggedIn()
}

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val openAiService: OpenAiService
) : AiRepository {

    override suspend fun analyseCv(cvText: String, jobDescription: String): Result<CvAnalysisResult> = runCatching {
        val prompt = buildPrompt(cvText, jobDescription)
        val response = openAiService.analyse(
            auth = "Bearer ${BuildConfig.OPENAI_API_KEY}",
            request = OpenAiRequest(
                messages = listOf(
                    OpenAiMessage("system", SYSTEM_PROMPT),
                    OpenAiMessage("user", prompt)
                ),
                response_format = ResponseFormat()
            )
        )
        val content = response.choices.firstOrNull()?.message?.content
            ?: throw Exception("Empty response from AI")
        val gson = Gson()
        val parsed = gson.fromJson(content, AnalysisJson::class.java)
        CvAnalysisResult(
            matchScore = parsed.match_score.coerceIn(0, 100),
            presentSkills = parsed.present_skills,
            missingSkills = parsed.missing_skills,
            suggestions = parsed.suggestions,
            summary = parsed.summary,
            analysedAt = System.currentTimeMillis()
        )
    }

    private fun buildPrompt(cvText: String, jobDescription: String) = """
        CV TEXT:
        $cvText
        
        JOB DESCRIPTION:
        $jobDescription
    """.trimIndent()

    companion object {
        private const val SYSTEM_PROMPT = """You are an expert career coach and ATS analyst.
Analyse the provided CV against the job description and respond ONLY with valid JSON in this exact format:
{
  "match_score": <integer 0-100>,
  "present_skills": ["skill1", "skill2"],
  "missing_skills": ["skill1", "skill2"],
  "suggestions": ["suggestion1", "suggestion2", "suggestion3"],
  "summary": "2-3 sentence overall assessment"
}
Be specific, practical, and constructive."""
    }
}

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val storageSource: FirebaseStorageDataSource,
    private val authSource: FirebaseAuthDataSource
) : StorageRepository {

    override suspend fun uploadCv(fileBytes: ByteArray, fileName: String): Result<String> = runCatching {
        val userId = authSource.currentUser?.uid ?: throw Exception("Not authenticated")
        storageSource.uploadCv(userId, fileBytes, fileName)
    }

    override suspend fun extractTextFromPdf(fileBytes: ByteArray): Result<String> = runCatching {
        // Real PDF parsing via PdfBox-Android — decodes actual content streams
        // (including compressed/FlateDecode streams), unlike a raw byte-to-text scan.
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(
            java.io.ByteArrayInputStream(fileBytes)
        )
        val text = try {
            com.tom_roush.pdfbox.text.PDFTextStripper().getText(document)
                .replace(Regex("\\s+"), " ")
                .trim()
        } finally {
            document.close()
        }
        if (text.length < 50) throw Exception("Could not extract text from PDF. Please ensure it is not a scanned/image-only PDF.")
        text.take(8000) // Limit to avoid token overflow
    }
}

// ── Firestore mapping helpers ──────────────────────────────────────────────

fun JobApplication.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "userId" to userId, "companyName" to companyName,
    "jobTitle" to jobTitle, "jobDescription" to jobDescription,
    "location" to location, "salary" to salary, "status" to status.name,
    "appliedDate" to appliedDate.time, "deadlineDate" to deadlineDate?.time,
    "notes" to notes, "jobUrl" to jobUrl, "contactName" to contactName,
    "contactEmail" to contactEmail, "createdAt" to createdAt.time,
    "updatedAt" to updatedAt.time
)

fun Map<String, Any>.toJobApplicationEntity(): com.jobtracker.data.local.entity.JobApplicationEntity {
    return com.jobtracker.data.local.entity.JobApplicationEntity(
        id = this["id"] as? String ?: UUID.randomUUID().toString(),
        userId = this["userId"] as? String ?: "",
        companyName = this["companyName"] as? String ?: "",
        jobTitle = this["jobTitle"] as? String ?: "",
        jobDescription = this["jobDescription"] as? String ?: "",
        location = this["location"] as? String ?: "",
        salary = this["salary"] as? String ?: "",
        status = this["status"] as? String ?: ApplicationStatus.APPLIED.name,
        appliedDate = (this["appliedDate"] as? Long) ?: System.currentTimeMillis(),
        deadlineDate = this["deadlineDate"] as? Long,
        notes = this["notes"] as? String ?: "",
        jobUrl = this["jobUrl"] as? String ?: "",
        contactName = this["contactName"] as? String ?: "",
        contactEmail = this["contactEmail"] as? String ?: "",
        matchScore = null, missingSkillsJson = null, presentSkillsJson = null,
        suggestionsJson = null, analysisSummary = null, analysedAt = null,
        createdAt = (this["createdAt"] as? Long) ?: System.currentTimeMillis(),
        updatedAt = (this["updatedAt"] as? Long) ?: System.currentTimeMillis(),
        isSynced = true
    )
}
