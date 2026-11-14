# Noma Reborn — Android architecture and phased delivery
Source: `replica/recon.md`, `replica/features.csv`.

## Keep rather than rewrite
Kotlin, Jetpack Compose Material 3, current SQLite repository, Note IDs, offline behavior, share receiver, widgets, export/import, and save state protections. A wholesale rewrite would risk user data and delay parity.

## Clean-room construction
No decompilation of target APK. Recreate public patterns with independently written UI. Brand remains Noma; icons and text remain original. Keep the low-screen bottom search pill already requested.

## Delivery order (independently compilable PRs)
1. P0: editor find/replace with original text preserved and revision-queue safe save; UI and JVM tests.
2. P1: structured checklist block model, migration, keyboard actions, TalkBack tests.
3. P1: folders/subfolders with stable IDs, transfer actions, additive schema, import/export.
4. P1: 30-day recovery and backup compatibility, failure injection.
5. P2: attachments and audio; encrypted note lock only with reviewed cryptography.
6. P2: Markdown/PDF/HTML, tags, cross-links, appearance refinements.
7. P3: sync/Drive, scanning, transcription, math and tables after correctness and privacy audit.

## Migration and release gates
Database migrations must preserve all existing note rows and work from old application versions. Never uninstall to install a new build. Add unit fixtures for previous schema versions. CI gates: JVM tests, lint, debug compilation, API 35 UI instrumentation. Pixel 9 review for accessibility, keyboard, rotation, performance, and back navigation. Real timing measurements required before any 'faster' claim.

## Competitor-inspired quality improvements
Avoid paywall friction, intrusive ads, unsafe account locking, and slow scroll issues reported in public reviews. Noma should aim for straightforward offline-first behavior and explicit recovery states.

## Evidence
Google Play: https://play.google.com/store/apps/details?id=com.inotesapp.inotes
Replica method: https://github.com/Jakeschincariol/replica-skill
