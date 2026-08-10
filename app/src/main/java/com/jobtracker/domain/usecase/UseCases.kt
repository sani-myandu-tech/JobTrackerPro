package com.jobtracker.domain.usecase

import com.jobtracker.data.local.secure.SecureCvCache
import com.jobtracker.domain.model.AnalyticsData
import com.jobtracker.domain.model.ApplicationStatus
import com.jobtracker.domain.model.CvAnalysisResult
import com.jobtracker.domain.model.JobApplication
import com.jobtracker.domain.model.User
import com.jobtracker.domain.model.WeeklyCount
import com.jobtracker.domain.repository.AiRepository
import com.jobtracker.domain.repository.AuthRepository
import com.jobtracker.domain.repository.JobRepository
import com.jobtracker.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class GetJobsUseCase @Inject constructor(private val repo: JobRepository) {
    operator fun invoke(): Flow<List<JobApplication>> = repo.getJobs()
}

class GetJobByIdUseCase @Inject constructor(private val repo: JobRepository) {
    operator fun invoke(id: String): Flow<JobApplication?> = repo.getJobById(id)
}

class SaveJobUseCase @Inject constructor(private val repo: JobRepository) {
    suspend operator fun invoke(job: JobApplication): String {
        return if (job.id.isEmpty()) repo.insertJob(job) else { repo.updateJob(job); job.id }
    }
}

class DeleteJobUseCase @Inject constructor(private val repo: JobRepository) {
    suspend operator fun invoke(id: String) = repo.deleteJob(id)
}

class SearchJobsUseCase @Inject constructor(private val repo: JobRepository) {
    operator fun invoke(query: String): Flow<List<JobApplication>> = repo.searchJobs(query)
}

class SignInUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> =
        repo.signIn(email, password)
}

class RegisterUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, name: String): Result<User> =
        repo.register(email, password, name)
}

class SignOutUseCase @Inject constructor(
    private val repo: AuthRepository,
    private val secureCvCache: SecureCvCache
) {
    suspend operator fun invoke() {
        repo.signOut()
        // Wipe the encrypted CV cache so it isn't left behind for the next person on this device.
        secureCvCache.clear()
    }
}

class GetCurrentUserUseCase @Inject constructor(private val repo: AuthRepository) {
    operator fun invoke(): User? = repo.currentUser
}

class AnalyseCvUseCase @Inject constructor(
    private val aiRepo: AiRepository,
    private val storageRepo: StorageRepository,
    private val secureCvCache: SecureCvCache
) {
    /**
     * Analyse a CV supplied as a real PDF file. [pdfBytes] must be valid PDF binary —
     * this path parses actual PDF content streams and will fail on plain text bytes.
     * Use [invokeWithText] for manually pasted CV text instead.
     */
    suspend operator fun invoke(
        pdfBytes: ByteArray,
        fileName: String,
        jobDescription: String,
        jobId: String
    ): Result<CvAnalysisResult> {
        val cvTextResult = storageRepo.extractTextFromPdf(pdfBytes)
        val cvText = cvTextResult.getOrElse { return Result.failure(it) }

        // Cache the extracted text encrypted at rest, so the user doesn't have to
        // re-upload/re-paste their CV for the next job they analyse against.
        secureCvCache.saveLastCvText(cvText)

        storageRepo.uploadCv(pdfBytes, fileName)
        return aiRepo.analyseCv(cvText, jobDescription)
    }

    /** Analyse a CV supplied as plain, manually pasted text — no PDF parsing involved. */
    suspend fun invokeWithText(
        cvText: String,
        jobDescription: String
    ): Result<CvAnalysisResult> {
        if (cvText.isBlank()) return Result.failure(Exception("CV text is empty"))
        secureCvCache.saveLastCvText(cvText)
        return aiRepo.analyseCv(cvText, jobDescription)
    }
}

class GetAnalyticsUseCase @Inject constructor(private val repo: JobRepository) {
    operator fun invoke(): Flow<AnalyticsData> = repo.getJobs().map { jobs ->
        val total = jobs.size
        val interviews = jobs.count { it.status == ApplicationStatus.INTERVIEW }
        val offers = jobs.count { it.status == ApplicationStatus.OFFER }
        val rejections = jobs.count { it.status == ApplicationStatus.REJECTED }
        val successRate = if (total > 0) (interviews + offers).toFloat() / total * 100f else 0f

        val weekFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val cal = Calendar.getInstance()
        val weeklyMap = mutableMapOf<String, Int>()
        repeat(6) { i ->
            cal.time = java.util.Date()
            cal.add(Calendar.WEEK_OF_YEAR, -(5 - i))
            val label = weekFormat.format(cal.time)
            weeklyMap[label] = 0
        }
        jobs.forEach { job ->
            cal.time = job.appliedDate
            val label = weekFormat.format(cal.time)
            weeklyMap[label] = (weeklyMap[label] ?: 0) + 1
        }

        val statusDist = ApplicationStatus.entries.associateWith { status ->
            jobs.count { it.status == status }
        }

        AnalyticsData(
            totalApplications = total,
            interviews = interviews,
            offers = offers,
            rejections = rejections,
            successRate = successRate,
            weeklyData = weeklyMap.map { WeeklyCount(it.key, it.value) },
            statusDistribution = statusDist
        )
    }
}
