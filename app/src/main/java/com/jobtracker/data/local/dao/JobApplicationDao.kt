package com.jobtracker.data.local.dao

import androidx.room.*
import com.jobtracker.data.local.entity.JobApplicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobApplicationDao {

    @Query("SELECT * FROM job_applications ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<JobApplicationEntity>>

    @Query("SELECT * FROM job_applications WHERE id = :id")
    fun getJobById(id: String): Flow<JobApplicationEntity?>

    @Query("SELECT * FROM job_applications WHERE companyName LIKE '%' || :query || '%' OR jobTitle LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchJobs(query: String): Flow<List<JobApplicationEntity>>

    @Query("SELECT * FROM job_applications WHERE status = :status ORDER BY createdAt DESC")
    fun getJobsByStatus(status: String): Flow<List<JobApplicationEntity>>

    @Query("SELECT * FROM job_applications WHERE isSynced = 0")
    suspend fun getUnsyncedJobs(): List<JobApplicationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: JobApplicationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<JobApplicationEntity>)

    @Update
    suspend fun updateJob(job: JobApplicationEntity)

    @Query("DELETE FROM job_applications WHERE id = :id")
    suspend fun deleteJob(id: String)

    @Query("UPDATE job_applications SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
