# P0 portability — review and acceptance gates

**Branch:** `feature/noma-p0-portability` (stacked on the existing unmerged Noma app branch).
**Boundary:** offline import/export, data integrity, user-controlled backup. No hosted storage, account, or irreversible operations.

## Implementation checklist: 30+ distinct actions

1. Define `noma-notes` backup format identifier.
2. Define a versioned JSON schema (`schemaVersion=1`).
3. Record backup export time.
4. Export every note ID.
5. Preserve note title in backups.
6. Preserve note body in backups.
7. Preserve pinned state.
8. Preserve archived state.
9. Preserve original creation timestamp.
10. Preserve original modification timestamp.
11. Encode Unicode as UTF-8.
12. Reject malformed UTF-8 rather than silently replacing characters.
13. Accept optional UTF-8 BOM.
14. Reject unknown backup format.
15. Reject future/unknown backup schema versions.
16. Reject duplicate note IDs within one backup.
17. Reject negative/fractional timestamps.
18. Enforce a 32 MiB file-size limit before parsing.
19. Enforce 5,000-note limit.
20. Enforce per-field length limits for malicious/corrupt imports.
21. Import Markdown as one note with H1 title recognition.
22. Preserve Markdown body and leading blank lines on round-trip.
23. Import plain TXT with inferred filename title.
24. Export one note as Markdown.
25. Export one note as TXT.
26. Use Android's system OpenDocument picker (no broad storage permission).
27. Use Android's system CreateDocument picker for exports.
28. Wait for queued SQLite writes before producing a backup.
29. Abort backup if latest editor revision failed to save.
30. Restore JSON notes without replacing existing rows.
31. Remap incoming IDs that collide with existing rows.
32. Save imported rows in a single SQLite transaction.
33. Preserve existing database contents on failed batch insert.
34. Refresh UI repository state after import.
35. Provide visible progress while transferring.
36. Provide localized success/error feedback.
37. Show explicit plaintext/unencrypted backup warning.
38. Keep JSON backups fully offline and user-chosen.
39. Document import limitations and how to recover data.
40. Add round-trip and adversarial JVM tests.
41. Add SQLite restore and collision instrumentation tests.

## Validation protocol

- Required CI: `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, `:app:lintDebug`, `:app:assembleDebug`.
- Required device/emulator (NOT implied by a green JVM build): choose and save backup, edit note, restore JSON, verify pinned/archive flags/timestamps, import TXT/MD, export MD/TXT, verify persistence after process restart.
- Adversarial checks: wrong schema, truncated JSON, malformed UTF-8, colliding IDs, extreme size, duplicated IDs, cancelled picker, provider failure, partial writes, storage full.
- Rollback: revert commits on this isolated branch. The original app branch and `main` stay unchanged.

## Security and limitations

The JSON backup is **not encrypted**. Protect it wherever you store it. Restore is **additive**, not a destructive replacement. Re-importing the same file duplicates notes (with new IDs where needed) by design to guarantee no existing note is overwritten. TXT/Markdown exports represent a single note and do not preserve all metadata; JSON is the full-fidelity recovery format. The system backup setting remains independent of manual backups.
