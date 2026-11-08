# Noma

A calm, offline-first Android notes app. Its visual rhythm is inspired by familiar iOS productivity apps, but the implementation is entirely native Android: **Kotlin, Jetpack Compose and Material 3 / M3 Expressive design roles**.

## Implemented

- Create, edit and delete notes with immediate write-through local persistence.
- Search by title or body, filter pinned notes, and group pinned notes above recent notes.
- Archive and restore notes without deleting them; search the archive and undo archive actions.
- Sort by recent edit or alphabetical title, while preserving pinned sections.
- Note previews and last-edited timestamps.
- Delete confirmation with an Undo snackbar.
- Light, dark, and system theme modes remembered across launches; semantic colors, typography, shapes, spacing, and motion.
- English and French resources, accessible labels, Android Back handling, edge-to-edge content and IME resizing.
- Local private SQLite database (no network access, account or third-party service).
- Explicit loading, empty, no-results and storage error/retry states.
- Adaptive two-pane layout on wide windows and one-pane navigation on phones.
- Editor save status reflects SQLite commits, with retry and a warning before leaving after a failed save.
- User-initiated native Android text sharing.
- Manual offline JSON backups preserving notes, pin/archive flags and timestamps, with versioned schema.
- Non-destructive full-backup restore (ID collisions are remapped), atomic SQLite import and failure feedback.
- UTF-8 TXT / Markdown single-note import and export through Android's system document picker.
- File size/count/field validation protects against accidental oversized or corrupt imports.
- Receive text from Android's native share sheet, with bounded input and direct persistent capture.
- Full backups are unencrypted plain text: an explicit warning precedes export; users choose where to store the file.
- Selecting an existing JSON backup adds its notes without replacing current notes; repeated restores create separate notes rather than deleting or merging originals.

## Build and run

Requirements: Android Studio with JDK 17, Android SDK Platform 36 / Build Tools 36.0.0 and Gradle 8.13. Android Studio can import this directory and sync the Gradle Kotlin DSL project.

CLI with Gradle 8.13 installed:

```bash
gradle :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
gradle :app:installDebug
```

The Android GitHub Actions job installs the required SDK and runs the same validation tasks. The APK produced by a successful job is attached to the workflow as `Noma-debug-apk`.

**Safe previews:** This UI iteration is labeled **Noma Design Preview**, with package ID (`dev.juuri.noma.designpreview`) so it coexists with earlier Noma previews. CI uses ephemeral debug signing keys; successive APKs cannot reliably upgrade the same installation. Never uninstall an installation containing important notes. Production requires stable protected signing and a backup/migration strategy.


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

The UI uses Material 3 component and design tokens from the stable Compose Material3 artifact. See [the Noma design system and UX audit](docs/DESIGN_SYSTEM.md) for decisions, reference principles, accessibility and adaptive breakpoints. Expressive styling here means deliberate variation in shapes, spacing, typography, and movement rather than an experimental component dependency.

## Verification and constraints

- `app/src/test` includes JVM tests for creation, updating, searching by body/title, pin ordering, deletion/restore and note previews.
- `app/src/androidTest` contains on-device Compose flows, SQLite persistence and v1→v2 migration tests; an emulator or device is required. Database tests use a separate test database and never delete real notes.
- A GitHub Actions workflow builds, tests, lints and publishes the debug APK as a short-lived artifact on successful validation.
- SQLite version 2 adds archive state through a non-destructive v1→v2 migration that preserves every existing note. Future schema upgrades require explicit migrations.
- The repository initially contained no project or Gradle wrapper binary. CI uses an installed Gradle 8.13 distribution to avoid committing an unverified binary. Android Studio and a local Gradle 8.13 installation are supported.

### Privacy

No INTERNET permission, analytics SDK or account access. Notes remain on-device unless the user enables Android system backup. Android does not automatically end-to-end encrypt every backup destination; disable device backups for sensitive notes.
