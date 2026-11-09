# Noma — delivery program

All feature branches start at the last green design-preview commit. Do not merge a branch merely because it compiles. Each cycle needs **at least 25 separately verifiable implementation, integration, test, documentation, or security actions**; no arbitrary edits to reach the count. Each branch requires multiple cohesive commits, a reproducible build, JVM tests, compiled and executed Android instrumentation where available, lint, a data-preservation audit, and a rollback path. A passing CI build is necessary but not sufficient for device approval.

| Priority | Branch | End-to-end acceptance |
|---|---|---|
| P0 Portability | `feature/noma-portability` | Import/export TXT, Markdown and versioned JSON; Android SAF picker; encoding and escaping; duplicates; conflicts; cancel; atomicity; round trips; privacy |
| P0 Backup | `feature/noma-backup-recovery` | Versioned full backup, restore preview, integrity validation, encrypted optional backup, partial failure, explicit confirmation, rollback |
| P0 Reliability | `feature/noma-reliability` | Process death, rotation, rapid typing, write failures, disk full, migration, upgrade signing, keyboard, accessibility, instrumentation and regression |
| P1 Capture | `feature/noma-quick-capture` | Widget, shortcut, Android ACTION_SEND/ACTION_PROCESS_TEXT, background lifecycle, duplicate handling, no data loss |
| P1 Formatting | `feature/noma-writing-formatting` | Checklists, headings, emphasis, plain text fallback, accessible editing, stable serialization, import/export |
| P1 Organization | `feature/noma-organization` | Folders/tags, migrations, filters, archive/pin semantics, rename/delete safety, empty states |
| P2 Sync | `feature/noma-private-sync` | Opt-in provider, identity, authenticated encryption, offline queue, conflicts, key lifecycle, device removal, recovery, explicit privacy documentation |
| P2 Search | `feature/noma-advanced-search` | Ranked title/body results, filters, highlights, diacritics, large-library performance, indexing and test fixtures |

## Integration dependencies
- Portability and backup must precede any schema expansion and sync rollout.
- Formatting, organization and search must preserve import/export and backup compatibility.
- Quick capture must reuse the single durable write pipeline and be covered by lifecycle tests.
- Sync must never silently upload notes or replace local data; user consent and threat model are gates.
- Never publish, force-push, delete user data, merge to main, or rotate signing keys without approval.
- Every stage should document actual evidence, unresolved limitations, and a tested rollback procedure.

## Current cycle: P0 portability — 25-action gate
1. Version the portable data schema.
2. Define stable UTF-8 output.
3. Preserve note IDs in lossless backups.
4. Preserve creation timestamps.
5. Preserve modification timestamps.
6. Preserve pin state.
7. Preserve archive state.
8. Preserve multiline bodies.
9. Preserve empty titles.
10. Preserve Unicode.
11. Encode JSON control characters.
12. Detect invalid JSON.
13. Reject unknown incompatible versions.
14. Limit pathological input size.
15. Support Markdown export.
16. Escape Markdown filenames.
17. Support TXT export.
18. Avoid overwriting existing notes silently.
19. Define duplicate-ID conflict policy.
20. Use Android Storage Access Framework.
21. Handle canceled picker without writes.
22. Surface read/write failures.
23. Test round-trip fidelity.
24. Test adversarial inputs and collisions.
25. Document privacy and restore/rollback.
No completion claim until each action has implementation evidence and tests.
