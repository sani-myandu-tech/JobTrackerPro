# 🚀 JobTracker Pro — Setup Guide

## What this app covers (assessment spec)
- ✅ Full CRUD (Read, Insert, Edit, Delete) via Room + Firebase Firestore
- ✅ Jetpack Compose + Material 3 UI
- ✅ Retrofit / OkHttp networking (OpenAI API)
- ✅ Firebase Authentication (Email/Password — uses OAuth 2.0 under the hood)
- ✅ Firebase Firestore (NoSQL cloud database — GCP)
- ✅ Firebase Storage (GCP cloud storage for CV uploads)
- ✅ Room (local SQLite persistence + offline-first sync)
- ✅ Native AI — OpenAI GPT-4o CV analysis (match score, skills gap, suggestions)
- ✅ In-app AI Chatbot — GPT-4o-mini powered assistant for new users
- ✅ Real PDF parsing (PdfBox-Android) for the CV analyser
- ✅ Encrypted local cache (EncryptedSharedPreferences, AES-256) for the user's CV text
- ✅ MPAndroidChart — analytics charts
- ✅ WorkManager — background cloud sync

---

## Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 26+
- A Firebase account (free): https://firebase.google.com
- An OpenAI API key (free trial available): https://platform.openai.com/api-keys

No extra setup is needed for PdfBox-Android or `security-crypto` — both are declared
in `app/build.gradle.kts` and resolve automatically from Maven Central on Gradle sync.

---

## Step 1 — Open in Android Studio

1. Extract the ZIP
2. Open Android Studio → **File → Open** → select the `JobTrackerPro` folder
3. Wait for Gradle sync (first sync ~500MB, since PdfBox-Android adds a few MB more than before)

---

## Step 2 — Configure Firebase

1. Go to https://console.firebase.google.com
2. **Add project** → name it `JobTrackerPro`
3. **Add app** → Android → package name: `com.jobtracker`
4. Download `google-services.json`
5. Copy it into the `app/` folder (replace `google-services.json.template`, rename to `google-services.json`)

### Enable these Firebase services:
| Service | Steps |
|---------|-------|
| Authentication | Build → Authentication → Sign-in method → Email/Password → Enable |
| Firestore | Build → Firestore Database → Create database → Production mode → choose region |
| Storage | Build → Storage → Get started → Production mode |

### Deploy security rules (paste into Firebase Console):
- **Firestore** → Rules → paste contents of `firestore.rules` → Publish
- **Storage** → Rules → paste contents of `storage.rules` → Publish

---

## Step 3 — Configure local.properties

1. Copy `local.properties.template` → rename to `local.properties`
2. Set your Android SDK path:
   - Windows: `sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk`
   - Mac/Linux: `sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk`
3. Set your OpenAI key:
   ```
   OPENAI_API_KEY=sk-your-key-here
   ```
   Get a key from https://platform.openai.com/api-keys

> Find your exact SDK path in Android Studio: **File → Project Structure → SDK Location**

> `local.properties` is git-ignored — it will never be committed, so your API key stays local.

---

## Step 4 — Run

1. Connect Android device (USB debugging enabled) or start emulator (API 26+)
2. Click **▶ Run**

---

## Using the AI Feature

1. Add a job application with a **job description** (paste the full JD)
2. Open that job → tap the **⋮ menu** → AI CV Analysis
3. Either upload your CV as a real PDF file, or paste your CV text directly
4. Tap **Analyse My CV**
5. View your match score, missing skills, and suggestions

If you've analysed a CV before, a **"Use last CV"** button appears on the paste-text
tab — it pulls your most recently used CV text from an encrypted local cache, so you
don't have to re-upload or re-paste it for every new job.

> **Note:** The AI feature requires a valid OpenAI API key with available credits.
> Each analysis uses approximately 1,500–2,000 tokens (~$0.01–0.02 with GPT-4o).

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Gradle sync fails | Check internet connection; File → Invalidate Caches → Restart |
| `google-services.json` error | Ensure the real file is in `app/` (not the template) |
| Firebase auth fails | Check Email/Password is enabled in Firebase Console |
| AI returns error | Verify OpenAI API key in `local.properties`; check you have API credits |
| "Could not extract text from PDF" | The PDF is likely scanned/image-only (no real text layer) — PdfBox-Android can only read actual text content, not OCR a scanned image |
| App crashes on launch | Check Logcat in Android Studio for the specific error |

---

## Project Structure

```
app/src/main/java/com/jobtracker/
├── data/
│   ├── local/          ← Room database, DAOs, entities
│   │   └── secure/      ← Encrypted local cache (SecureCvCache)
│   ├── remote/         ← Firebase data sources, OpenAI Retrofit service
│   ├── repository/     ← Repository implementations (incl. PdfBox text extraction)
│   └── worker/         ← WorkManager background sync
├── di/                 ← Hilt dependency injection modules
├── domain/
│   ├── model/          ← Data classes (JobApplication, User, etc.)
│   ├── repository/     ← Repository interfaces (abstractions)
│   └── usecase/        ← Business logic use cases
└── presentation/
    ├── auth/           ← Login & Register screens
    ├── dashboard/      ← Home screen
    ├── joblist/        ← All applications list
    ├── addeditjob/     ← Add/Edit form
    ├── aianalysis/     ← AI CV analysis feature
    ├── analytics/      ← Charts & insights
    ├── components/     ← Shared Compose components
    ├── navigation/     ← NavHost & routes
    └── theme/          ← Material 3 theme & colours
```

---

## Known limitations

- **PDF parsing** reads real text content from PDFs but cannot OCR scanned/image-only
  PDFs — if a CV was scanned as an image rather than exported as text, upload will fail
  with a clear error rather than silently returning garbage.
- **No automated test suite yet.** Given the CV analysis flow is now the most complex
  logic in the app, unit tests for `AnalyseCvUseCase` and `SecureCvCache` would be the
  highest-value next addition.
