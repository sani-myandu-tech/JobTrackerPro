# JobTracker Pro

An Android job-application tracker with AI-assisted CV matching, built with Kotlin
and Jetpack Compose. Offline-first (Room + Firestore sync), with two real OpenAI-backed
features: a CV-vs-job-description analyser and an in-app assistant chatbot.

## Features

- **Job tracking** — add, edit, and track applications through statuses (Applied,
  Interview, Offer, Rejected), with a dashboard and analytics (weekly volume,
  status breakdown)
- **AI CV Analysis** — upload a real PDF or paste your CV text, get a match score
  against a job description, missing-skills breakdown, and improvement suggestions
  (GPT-4o)
- **AI Chatbot assistant** — GPT-4o-mini powered, with a rule-based fallback if the
  API is unavailable
- **Offline-first** — Room local database with background sync to Firebase Firestore
  via WorkManager; the app is fully usable without a network connection
- **Auth & security** — Firebase Authentication, Firestore/Storage rules scoped to
  `request.auth.uid`, and an encrypted local cache (`EncryptedSharedPreferences`,
  AES-256) for the user's CV text between sessions

## Tech stack

Kotlin · Jetpack Compose · Material 3 · Hilt (DI) · Room · Firebase (Auth, Firestore,
Storage) · Retrofit/OkHttp · WorkManager · PdfBox-Android · MPAndroidChart

## Setup

See [SETUP_GUIDE.md](SETUP_GUIDE.md) for full setup steps (Firebase config, API keys,
running locally).

## Notes on this codebase

This started as a university capstone project and went through a second pass to
close gaps between what the original spec/assessment notes claimed and what the
code actually did — see the git history for specifics:

- CV text extraction now uses real PDF parsing (PdfBox-Android) instead of a raw
  byte-scan hack that only worked on simple, uncompressed PDFs
- The encrypted local storage mentioned in the original spec was a declared but
  unused dependency — it's now actually wired up, caching the user's CV text
  (genuinely personal data) encrypted at rest

Full technical writeup of what was found and fixed is in
[ASSESSMENT_NOTES.md](ASSESSMENT_NOTES.md).

## Known limitations

- PDF parsing can't OCR scanned/image-only PDFs — text-based PDFs only
- No automated test suite yet

## Author

Lungisani Mnyandu — [GitHub](https://github.com/sani-mnyandu-tech) · [LinkedIn](https://linkedin.com/in/lungisani-mnyandu)
