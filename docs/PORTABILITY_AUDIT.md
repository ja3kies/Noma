# Noma — P0 portability implementation and acceptance audit

This file records verifiable source changes, not a claim that Android instrumentation has executed. Do **not** mark release-ready until the same revision passes CI and real-device checks.

## 30 independent implementation actions

| # | Action and implementation evidence |
|---|---|
| 1 | Versioned archive format marker in `NotesPortableFormat` |
| 2 | Reject unknown archive format |
| 3 | Reject unknown future archive version |
| 4 | Stable note ordering in JSON export |
| 5 | Preserve exact note UUID/id |
| 6 | Preserve empty and nonempty note title |
| 7 | Preserve multiline bodies |
| 8 | Preserve pinned status |
| 9 | Preserve archived status |
| 10 | Preserve creation timestamps |
| 11 | Preserve edit timestamps |
| 12 | Strict UTF-8 decoding, no silent replacement |
| 13 | Bounded archive bytes |
| 14 | Maximum 10,000 notes per archive |
| 15 | Reject duplicate IDs within an archive |
| 16 | Reject blank or excessively long IDs |
| 17 | Validate timestamp order and nonnegative times |
| 18 | Enforce title/body size limits |
| 19 | Sanitize output filenames |
| 20 | Android SAF document export of all notes as JSON |
| 21 | Android SAF document import of all notes as JSON |
| 22 | Android SAF TXT import retaining raw content |
| 23 | Android SAF Markdown import retaining raw content |
| 24 | Per-note TXT export in editor |
| 25 | Per-note Markdown export in editor |
| 26 | Confirm import before writing and preview duplicate count |
| 27 | Skip existing note IDs without modifying existing content |
| 28 | SQLite all-or-nothing transaction for bulk import |
| 29 | Localized import/export UI in French and English |
| 30 | JVM and instrumentation regression tests covering round trips, malformed input, duplicate IDs, rollback and existing-note preservation |

## Safety and recovery

- The JSON archive is **plaintext**, not encrypted. Anyone with file access can read private notes.
- Export a JSON archive before updating or migrating Noma. Store it in a private location and verify that it can be read.
- Canceling SAF selection performs no repository writes; importing requires explicit confirmation.
- An imported ID already on-device is skipped. A different note with the same ID is **not** merged.
- Production SQLite import is transactional; an SQL failure should roll back every insertion from that operation.
- TXT and Markdown import as a *new* note with raw file content in the body and a sanitized filename-based title. These formats cannot preserve timestamps, pinning, archive state or IDs. Use JSON for lossless Noma-to-Noma migration.
- The import source and destination are accessed only through user-selected SAF URIs. No broad storage permission is needed.
- A failed export may leave an empty or partial document at the user-selected destination. Discard it and export again; do not treat an incomplete document as a backup.
- Future migrations must retain JSON v1 read compatibility and include sample fixtures and on-device migration tests.
- Rollback: keep the previous installed app and its database unchanged; revert this feature branch to baseline `7b986b8` only on the development branch. Preview debug signing is ephemeral. Do not uninstall any installed app holding notes.

## Release acceptance

- [ ] Latest commit: `assembleDebug`, `testDebugUnitTest`, `compileDebugAndroidTestKotlin` and `lintDebug` all green.
- [ ] Instrumented JSON codec, SQL migration and import rollback tests executed on emulator/device, not merely compiled.
- [ ] Import TXT, Markdown and JSON from the Android Files picker.
- [ ] Export TXT/Markdown note and JSON archive; verify bytes, accented French and Unicode.
- [ ] Test cancelled SAF picker, denied provider, missing file, partial stream and out-of-space error.
- [ ] Test long notes, empty notes, 320dp phone, TalkBack and system font scaling.
- [ ] Reopen app and confirm imported content survives process death.
- [ ] Verify a pre-existing note is never replaced by an imported colliding ID.
- [ ] Save a real-world backup, restore into a **separate test installation**, compare IDs and content.
- [ ] Confirm privacy disclosure and stable production signing/upgrade strategy.

The 30-action implementation threshold does not by itself satisfy the release gate; test evidence and an upgrade-safe APK are separate requirements.
