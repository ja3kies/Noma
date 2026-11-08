# Noma

A calm, offline-first Android notes app. Its visual rhythm is inspired by familiar iOS productivity apps, but the implementation is entirely native Android: **Kotlin, Jetpack Compose and Material 3 / M3 Expressive design roles**.

## Implemented

- Create, edit and delete notes with immediate write-through local persistence.
- Search by title or body, filter pinned notes, and group pinned notes above recent notes.
- Archive and restore notes without deleting them; search the archive and undo archive actions.
- Sort by recent edit or alphabetical title, while preserving pinned sections.
- Note previews and last-edited timestamps.
- Delete confirmation with an Undo snackbar.
- Light and dark themes following system appearance, cohesive semantic colors, typography and shapes.
- English and French resources, accessible labels, Android Back handling, edge-to-edge content and IME resizing.
- Local private SQLite database (no network access, account or third-party service).
- Explicit loading, empty, no-results and storage error/retry states.

## Build and run

Requirements: Android Studio with JDK 17, Android SDK Platform 36 / Build Tools 36.0.0 and Gradle 8.13. Android Studio can import this directory and sync the Gradle Kotlin DSL project.

CLI with Gradle 8.13 installed:

```bash
gradle :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
gradle :app:installDebug
```

The Android GitHub Actions job installs the required SDK and runs the same validation tasks. The APK produced by a successful job is attached to the workflow as `Noma-debug-apk`.

## Architecture

```text
app/src/main/java/dev/juuri/noma/
  MainActivity.kt                  Activity and dependencies
  domain/
    Note.kt                        Immutable note entity
    NotesRepository.kt             Storage contract and loading states
    NotesService.kt                Create/update/search/delete logic
  data/
    NotesOpenHelper.kt             Versioned SQLite schema and queries
    SqliteNotesRepository.kt       Serialized off-main-thread persistence
  ui/
    NotesViewModel.kt              UI state, sequential write queue, undo events
    NomaApp.kt                     List, filters, editor and dialogs
    theme/NomaTheme.kt             Semantic light/dark M3 theme
```

The note data is stored in the app-private database `noma-notes.db`, survives process restarts and participates in Android's normal encrypted cloud/device-transfer backup mechanism where supported. There is no remote sync.

The UI uses Material 3 component and design tokens from the stable Compose Material3 artifact. Expressive styling here means deliberate variation in shapes, spacing, typography, and movement rather than an experimental component dependency.

## Verification and constraints

- `app/src/test` includes JVM tests for creation, updating, searching by body/title, pin ordering, deletion/restore and note previews.
- `app/src/androidTest` includes on-device SQLite persistence and v1→v2 migration tests; a device or emulator is required to execute them.
- A GitHub Actions workflow builds, tests, lints and publishes the debug APK as a short-lived artifact on successful validation.
- SQLite version 2 adds archive state through a non-destructive v1→v2 migration that preserves every existing note. Future schema upgrades require explicit migrations.
- The repository initially contained no project or Gradle wrapper binary. CI uses an installed Gradle 8.13 distribution to avoid committing an unverified binary. Android Studio and a local Gradle 8.13 installation are supported.

### Privacy

No INTERNET permission, analytics SDK or account access. Notes remain on-device unless the user enables Android system backup. Android does not automatically end-to-end encrypt every backup destination; disable device backups for sensitive notes.
