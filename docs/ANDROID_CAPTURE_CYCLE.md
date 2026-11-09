# Noma — Android capture delivery cycle
Date: 2026-11-09. Branch: `feature/noma-android-capture`.
Base: `19d9064` from the isolated UX branch. **No merge to main.**

## Acceptance checklist: 33 implementation and verification actions
Each item refers to code and/or a committed regression scenario; an authored test is **not evidence that it has passed**.

| # | Concrete action | Evidence |
|---|---|---|
| 1 | Receive external `ACTION_SEND` using Android MIME `text/plain` | `AndroidManifest.xml` |
| 2 | Restrict exported share target to DEFAULT category and plain text | `AndroidManifest.xml` |
| 3 | Reject unsupported actions and rich media types | `AndroidCaptureIntents.kt`, instrumentation |
| 4 | Read `EXTRA_TEXT` only; no silent URI/file/network reads | `AndroidCaptureIntents.kt` |
| 5 | Accept optional `EXTRA_SUBJECT` as plain text title | `AndroidCaptureIntents.kt` |
| 6 | Bound subject to 200 characters | parser, instrumentation |
| 7 | Bound incoming body to 100,000 characters | parser and `NotesService` |
| 8 | Reject blank or missing body | parser, service unit test |
| 9 | Strip UTF-8 BOM on incoming text | parser, instrumentation |
| 10 | Handle hostile malformed intents without crashing Activity | `MainActivity.acceptCaptureIntent` |
| 11 | Support Android cold start with text share | `MainActivity.onCreate` |
| 12 | Support warm Activity re-entry via `onNewIntent` | `MainActivity.onNewIntent` |
| 13 | Prevent repeated processing of a restored Android intent | `setIntent(Intent(ACTION_MAIN))` |
| 14 | Preview sender-controlled title and text before saving | `NomaApp` confirmation |
| 15 | Let the user cancel shared text without a write | `NomaApp`, UI test |
| 16 | Explicitly confirm before saving content | `NotesViewModel.confirmSharedText` |
| 17 | Persist a fully initialized note in a single repository save | `NotesService.captureText` |
| 18 | Prevent double-tap duplicate captures | `capturingShare`, JVM test |
| 19 | Keep shared content available when a storage write fails | `NotesViewModel`, JVM test |
| 20 | Block replacement of an unsaved active editor | `NotesViewModel`, JVM test |
| 21 | Handle consecutive shares in a FIFO queue | `NotesViewModel`, JVM test |
| 22 | Guard queue growth against unbounded memory usage | queue size bound |
| 23 | Preserve pending share preview after Activity state restoration | `MainActivity.onSaveInstanceState` |
| 24 | Open saved shared note in the editor | `NotesViewModel`, Compose test |
| 25 | Add dynamic long-press launcher shortcut | `MainActivity.registerQuickCaptureShortcut` |
| 26 | Build shortcut intents with the runtime package, including preview variant | `Intent(this, MainActivity::class.java)` |
| 27 | Defer a new-note shortcut while the active draft is saving | `requestQuickNote`, JVM test |
| 28 | Coalesce repeated quick-note taps while awaiting a save | `requestQuickNote`, JVM test |
| 29 | Keep a queued quick-note request on a transient write failure | `retrySave`, JVM test |
| 30 | Add home-screen AppWidgetProvider with a one-tap explicit PendingIntent | `NomaQuickCaptureWidget.kt` |
| 31 | Register the widget manifest metadata and provide a 3x1 initial layout | `AndroidManifest.xml`, widget XML |
| 32 | Provide accessible large widget touch area and distinct light/dark styling | widget layout, colors |
| 33 | Localize all exposed capture prompts, shortcut and widget labels in French and English | `values/strings.xml`, `values-fr/strings.xml` |

## Tests and build gates
- [ ] GitHub CI `testDebugUnitTest` passes at **this branch's final SHA**.
- [ ] GitHub CI `lintDebug` passes, zero errors.
- [ ] GitHub CI `compileDebugAndroidTestKotlin` and `assembleDebug` pass.
- [ ] `connectedDebugAndroidTest` completes successfully with capture parser, widget registration, share consent and nonwrite cancel tests.
- [ ] Manual real-device test: from Android share sheet, copy website URL into Noma, verify dialog -> save -> note visible after force close/reopen.
- [ ] Manual launcher long-press shortcut: direct opening into focused blank note.
- [ ] Manual widget 3x1, light/dark, resize and tap behavior.
- [ ] Manual disk-failure and interruption tests with disposable data; no silent data loss.
- [ ] Preview APK coexistence and signing upgrade tested before installing above any existing preview.

## Safety / limitations
- No accounts, background service, persistent notification, network access, storage broad permission, or schema migrations added.
- Confirmation precedes persistence; cancelled sharing must not write content.
- Pending shares in the foreground queue are in process memory; only the currently visible pending share is stored in the Activity saved state. A device reboot, force-stop or process death under some conditions may discard secondary queued shares. **Do not claim a durable external inbox** until persisted drafts and idempotency are implemented.
- A very large incoming share is rejected rather than truncated silently. The sender must manually shorten it.
- A widget is an optional entry point, not a replacement for the in-app editor.
- Android debug CI signing is ephemeral and APKs cannot safely replace existing preview installations with a different signature. Never uninstall an app containing notes.
- Feature branch remains isolated and unmerged; rollback is rebuilding the baseline commit `19d9064` with a safe signing identity and preserving the database.
