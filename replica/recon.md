# Noma Reborn — public-source reconnaissance (Replica / replica-recon)

Target: Notes – iOS Style Notepad (Android package `com.inotesapp.inotes`), by Baday Production.
Public sources:
- https://play.google.com/store/apps/details?id=com.inotesapp.inotes (description/screenshots, updated Nov 5 2026)
- https://github.com/Jakeschincariol/replica-skill (MIT method, clean-room rules)
- Internal Noma repository: `NomaApp.kt`, `NotesService.kt`, `Note.kt`, `SqliteNotesRepository.kt`, `PortableDocuments.kt`
Evidence levels: CONFIRMED (public listing), INFERRED (navigation not independently observed). No proprietary screenshots or binaries included.

## Product boundary
Noma remains an independently branded native Android application built in Kotlin/Compose with local-first storage. Recreate public **behaviours and interaction patterns**, not the original code, artwork, brand, wording, or private content. Never circumvent IAP, account authorization, DRM, or payment controls.

## Screen inventory
| ID | Screen | Evidence | Core elements | Edge states |
|---|---|---|---|---|
| S01 | Library / notes list | CONFIRMED | search, pinned, sorted cards, new note | empty, thousands of notes, filtered, loading, error |
| S02 | Plain/rich note editor | CONFIRMED | title, body, formatting, links, color | empty, IME, autosave failure, very long note |
| S03 | Checklist editor | CONFIRMED | tick/untick rows, nested sublists | all checked, drag/swipe, accessibility |
| S04 | Folders and nested folders | CONFIRMED | hierarchy, icon/emoji/color, note counts | empty, depth, rename/delete |
| S05 | Note finder / replace | CONFIRMED | search occurrences, previous/next, replace | zero matches, empty query, long text |
| S06 | Note actions | CONFIRMED | pin, share, export, lock, delete | permission, unsaved revision |
| S07 | Locked notes and app gate | CONFIRMED | passcode / biometric | auth failure, timeout, recovery |
| S08 | Voice memo editor | CONFIRMED | record/play/transcribe | mic permission denied, interrupted recording |
| S09 | Document and photo attachments | CONFIRMED | images, PDF, scan | large files, bad URI, no camera |
| S10 | Backup and restore | CONFIRMED | offline backups, Drive sync, recovery | data conflicts, expired authorization |
| S11 | Recently deleted | CONFIRMED | trash, restore, 30-day retention | no items, expiry boundaries |
| S12 | Appearance and accessibility | CONFIRMED | theme, backgrounds, fonts, text sizing | low contrast, large fonts, RTL |
| S13 | Widgets and capture | CONFIRMED | quick note, share intake, home widgets | locked device, permission, process restart |
| S14 | Notes metadata | CONFIRMED | tags, links, tables, calculations | invalid syntax, cyclical note links |
| S15 | Settings and export | CONFIRMED | privacy, backups, Markdown, PDF, HTML | partial failure, no destination |

Noma currently has native S01/S02/S06/S12/S13 and portions of S10, with actual coverage indicated in `features.csv`. Missing reference screens are **not** evidence of pixel layout; do not infer visual measurements from product claims.

## Core user journeys
F01 Create note: S01 → S02 → autosave → S01 (existing Noma; verify on device)
F02 Organize: S01 → S04 folder → S02 → S04 (new)
F03 Checklist: S01 → S02/S03 → tick items → leave and reopen (new)
F04 Find & replace: S02 → S05 → replace once/all → autosave (new)
F05 Recover deleted: S01 → S06 trash → S11 → restore (new)
F06 Secure note: S02 → S06/S07 → unlock/read/edit (new)
F07 Capture audio: S02 → S08 → record/playback → reopen (new)
F08 Import/export: S01 → S15/S10 → choose format → verify recovery (partial existing)

## Repeated UI components
Adaptive scaffold, top navigation, persistent bottom search pill (existing Noma), note card, folder row, section header, toolbar actions, editor block, checklist row, material dialog, confirmation, snackbar recovery, accessible controls, empty state, animated note/item transitions.

## Data model / migration hypotheses
Existing `Note(id,title,body,isPinned,createdAt,updatedAt,isArchived)` must remain readable. Future entities: Folder(id,parentId,name,icon,color,order); NoteFolder(noteId,folderId); Attachment(id,noteId,mime,uri,checksum); ChecklistItem(id,noteId,order,text,checked,indent); NoteSecurity(noteId,encryptedKeyMetadata); DeletedNote(noteId,deletedAt); Tag and NoteTag; NoteLink(sourceId,targetId). All are proposals until implemented. Build additive SQLite migrations with rollback fixtures; no replacement or deletion of existing notes.

## Out of scope
Original application's source, assets, screenshots in distributable APK, branding, licensed fonts, account databases, paid entitlement circumvention, original private APIs, product marketing copy.

## Size and evidence constraints
Large (L/XL) multi-release project; initial vertical slice: notes list and editor, checklists, find/replace, basic folder hierarchy, backup integrity. Validate physical Android UX before parity claims. Public listing alone does not reveal exact tap-by-tap behaviour for every feature.