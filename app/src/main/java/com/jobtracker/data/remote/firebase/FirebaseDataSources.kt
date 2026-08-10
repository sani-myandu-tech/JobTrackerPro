package com.jobtracker.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jobtracker.domain.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth
) {
    val currentUser: User?
        get() = auth.currentUser?.let {
            User(uid = it.uid, email = it.email ?: "", displayName = it.displayName ?: "")
        }

    suspend fun signIn(email: String, password: String): User {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("Sign in failed")
        return User(uid = user.uid, email = user.email ?: "", displayName = user.displayName ?: "")
    }

    suspend fun register(email: String, password: String, displayName: String): User {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("Registration failed")
        val profileUpdate = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(displayName).build()
        user.updateProfile(profileUpdate).await()
        return User(uid = user.uid, email = user.email ?: "", displayName = displayName)
    }

    fun signOut() = auth.signOut()
    fun isLoggedIn() = auth.currentUser != null
}

@Singleton
class FirebaseFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authDataSource: FirebaseAuthDataSource
) {
    private fun jobsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("jobs")

    suspend fun getJobs(userId: String): List<Map<String, Any>> {
        val snapshot = jobsCollection(userId).get().await()
        return snapshot.documents.mapNotNull { it.data?.also { d -> d["id"] = it.id } }
    }

    suspend fun saveJob(userId: String, jobData: Map<String, Any>) {
        val id = jobData["id"] as? String
        if (id != null && id.isNotEmpty()) {
            jobsCollection(userId).document(id).set(jobData).await()
        } else {
            jobsCollection(userId).add(jobData).await()
        }
    }

    suspend fun deleteJob(userId: String, jobId: String) {
        jobsCollection(userId).document(jobId).delete().await()
    }
}

@Singleton
class FirebaseStorageDataSource @Inject constructor(
    private val storage: FirebaseStorage
) {
    suspend fun uploadCv(userId: String, fileBytes: ByteArray, fileName: String): String {
        val ref = storage.reference.child("cvs/$userId/$fileName")
        ref.putBytes(fileBytes).await()
        return ref.downloadUrl.await().toString()
    }
}
