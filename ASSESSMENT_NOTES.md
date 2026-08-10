# JobTracker Pro — Assessment Specification Mapping
**Module:** Mobile/Cloud Implementation Project (20%)**

---

## Core CRUD Requirements ✅

| Requirement | Implementation | File |
|-------------|---------------|------|
| Read data | `JobApplicationDao.getAllJobs()` → Room Flow → Firestore sync | `dao/JobApplicationDao.kt`, `repository/Repositories.kt` |
| Insert data | `JobRepositoryImpl.insertJob()` → Room + Firestore | `repository/Repositories.kt`, `addeditjob/AddEditJobScreen.kt` |
| Edit data | `JobRepositoryImpl.updateJob()` → Room + Firestore | `repository/Repositories.kt`, `addeditjob/AddEditJobViewModel.kt` |
| Delete data | `JobRepositoryImpl.deleteJob()` → Room + Firestore | `repository/Repositories.kt`, `joblist/JobListScreen.kt` |

---

## Mobile — Systems (HW/SW)

| Spec Item | Implementation |
|-----------|---------------|
| SoC / Memory / Storage | App targets API 26+ (covers ARM/x86 SoC families); Room uses SQLite on-device storage |
| Sensors / User Equipment | File picker for CV PDF upload using `ActivityResultContracts.OpenDocument` |
| Kernel / Android | minSdk 26 (Android 8.0 Oreo) → targetSdk 35 (Android 15) |

---

## Mobile — Application Layer ✅

| Spec Requirement | Technology Used | File |
|-----------------|-----------------|------|
| **Jetpack Compose** | All screens built with Compose + Material 3 | `presentation/` |
| **Material Design** | `MaterialTheme`, `Material3` components throughout | `theme/Theme.kt` |
| **Retrofit** | HTTP client for OpenAI GPT-4o API calls | `data/remote/api/OpenAiService.kt`, `di/AppModule.kt` |
| **OkHttp** | OkHttpClient with logging interceptor for Retrofit | `di/AppModule.kt` NetworkModule |
| **Room** | Local SQLite persistence, offline-first | `data/local/` |
| **Cloud / NoSQL** | Firebase Firestore (NoSQL document database on GCP) | `data/remote/firebase/FirebaseDataSources.kt` |
| **OAuth 2.0 / JWT** | Firebase Authentication uses OAuth 2.0 + JWT tokens under the hood | `data/remote/firebase/FirebaseDataSources.kt` |
| **GCP** | Firebase is a Google Cloud Platform product (Firestore, Storage, Auth) | `data/remote/firebase/` |

---

## AI & Emerging Technologies ✅

| Feature | Implementation | Detail |
|---------|---------------|--------|
| **Native AI — CV Analyser** | GPT-4o via OpenAI API | Analyses CV vs job description; returns match score 0-100, present skills, missing skills, improvement suggestions |
| **Native AI — Chatbot** | GPT-4o-mini via OpenAI API | In-app assistant that guides new users; animated typing indicator; conversation history; fallback rule-based engine if API unavailable |
| **AI Architecture** | Clean Architecture use case pattern | `domain/usecase/UseCases.kt` → `AnalyseCvUseCase` |

---

## Networking & Communication ✅

| Layer | Technology | Usage |
|-------|-----------|-------|
| **Short range** | WiFi / Bluetooth (device connectivity) | Android system |
| **Long range / internet** | HTTPS (TLS 1.3) | All API calls (`usesCleartextTraffic="false"` in Manifest) |
| **REST API** | Retrofit + OkHttp | OpenAI GPT-4o endpoint |
| **Cloud sync** | Firebase SDK (WebSocket-based real-time) | Firestore live sync |
| **Background sync** | WorkManager periodic tasks (every 15 min) | `data/worker/SyncWorker.kt` |

---

## Cloud Architecture ✅

| Spec Item | Implementation |
|-----------|---------------|
| **GCP** | Firebase (Auth, Firestore, Storage) — all GCP products |
| **NoSQL DB** | Firestore — document-based NoSQL |
| **Cloud Storage** | Firebase Storage — CV PDF uploads |
| **Cluster / Nodes** | Handled by Firebase/GCP infrastructure |
| **Open source packages** | Room, Retrofit, OkHttp, Hilt, Kotlin Coroutines |
| **Proprietary** | Firebase SDK, OpenAI API |
| **Local** | Room SQLite — offline-first with cloud sync |
| **Online** | Firestore real-time + WorkManager background sync |

---

## Security ✅

| Requirement | Implementation |
|-------------|---------------|
| **OAuth 2.0** | Firebase Auth implements OAuth 2.0 standard |
| **JWT** | Firebase ID tokens are JWTs; auto-refreshed by SDK |
| **HTTPS only** | `android:usesCleartextTraffic="false"` in Manifest |
| **Encrypted storage** | `EncryptedSharedPreferences` (AES-256-SIV keys / AES-256-GCM values) — caches the user's extracted CV text locally, encrypted at rest (`data/local/secure/SecureCvCache.kt`) |
| **PDF parsing** | PdfBox-Android — real PDF content-stream parsing, not a raw byte scan (`data/repository/Repositories.kt`) |
| **Firestore rules** | User-scoped security rules (`firestore.rules`) |
| **Storage rules** | Auth-gated CV upload rules (`storage.rules`) |

---

## App Screens Summary

```
1. Login Screen          — Firebase email/password auth
2. Register Screen       — Create account → Firebase Auth
3. Dashboard             — Stats (total, interviews, offers, success rate),
                           quick actions, recent jobs list
4. Job List              — All applications, search, filter by status,
                           long-press delete, swipe actions
5. Add/Edit Job          — Full form: company, title, location, salary,
                           job URL, description, status, contact, notes,
                           deadline date picker
6. AI CV Analysis        — PDF upload or paste CV text → GPT-4o analysis
                           → match score, skill gap, suggestions
7. Analytics             — MPAndroidChart bar chart (weekly volume) +
                           pie chart (status distribution), success rate
8. AI Chatbot            — GPT-4o-mini in-app assistant for new users,
                           animated typing dots, conversation history,
                           offline fallback
```

---

## Architecture Pattern

```
Presentation (Compose + ViewModel)
        ↕  StateFlow / UiState
Domain (Use Cases + Repository interfaces)
        ↕  Kotlin Coroutines + Flow
Data (Room + Firebase + Retrofit)
```

**Dependency Injection:** Hilt (Dagger-based)  
**Async:** Kotlin Coroutines + StateFlow  
**Navigation:** Jetpack Navigation Compose  
**Image loading:** Coil  
**Charts:** MPAndroidChart  
