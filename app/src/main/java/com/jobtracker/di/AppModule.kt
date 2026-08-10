package com.jobtracker.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jobtracker.data.local.JobTrackerDatabase
import com.jobtracker.data.local.dao.JobApplicationDao
import com.jobtracker.data.remote.api.OpenAiService
import com.jobtracker.data.remote.firebase.FirebaseAuthDataSource
import com.jobtracker.data.remote.firebase.FirebaseFirestoreDataSource
import com.jobtracker.data.remote.firebase.FirebaseStorageDataSource
import com.jobtracker.data.repository.AiRepositoryImpl
import com.jobtracker.data.repository.AuthRepositoryImpl
import com.jobtracker.data.repository.JobRepositoryImpl
import com.jobtracker.data.repository.StorageRepositoryImpl
import com.jobtracker.domain.repository.AiRepository
import com.jobtracker.domain.repository.AuthRepository
import com.jobtracker.domain.repository.JobRepository
import com.jobtracker.domain.repository.StorageRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): JobTrackerDatabase =
        Room.databaseBuilder(context, JobTrackerDatabase::class.java, "job_tracker_db")
            .fallbackToDestructiveMigration().build()

    @Provides @Singleton
    fun provideDao(db: JobTrackerDatabase): JobApplicationDao = db.jobApplicationDao()
}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides @Singleton fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    @Provides @Singleton fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    @Provides @Singleton fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton
    fun provideOpenAiService(client: OkHttpClient): OpenAiService = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenAiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindJobRepository(impl: JobRepositoryImpl): JobRepository
    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository
    @Binds @Singleton abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository
}
