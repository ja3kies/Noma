# P1 organization & writing — reviewed implementation

Branch: `feature/noma-p1-organization` stacked on `feature/noma-p1-capture`.
No automatic merge; source notes and metadata are preserved through schema changes.

## Checklist — 42 distinct actions

1. Add ordered tags to the Note entity.
2. Cap tag length to 32 characters.
3. Cap count at 20 tags per note.
4. Trim and collapse whitespace in new tags.
5. Normalize pasted #tag prefixes.
6. Reject duplicate tags case-insensitively.
7. Remove tags case-insensitively.
8. Preserve timestamps/pins/archive when mutating tags.
9. Enumerate tags for current active/archived section.
10. De-duplicate and sort the tag catalog.
11. Add tag-aware note search.
12. Add optional tag filter to the domain service.
13. Add tag filter StateFlow to the ViewModel.
14. Clear invalid tag scope when switching Archive view.
15. Create notes directly within a filtered tag.
16. Clear tag filter on external share capture.
17. Expose add/remove tag controls in the editor.
18. Handle Done IME to create tags.
19. Supply an explicit Add Tag button.
20. Show removable tag chips.
21. Include tag previews on note cards.
22. Expose tag filter among existing controls without hiding sort.
23. Update empty-results state for active tag filters.
24. Localize tag labels and accessibility descriptions.
25. Add SQLite v3 column for durable tag lists.
26. Migrate v2 to v3 without dropping user tables.
27. Migrate v1 to v3 through both migration steps.
28. Preserve original note data through the migration.
29. Store tags in both individual upserts and batch restores.
30. Read tagged notes from SQLite.
31. Include tags in JSON backup exports.
32. Accept older JSON v1 backups without tags.
33. Enforce backup tag limits.
34. Test tag rules and domain search.
35. Add SQLite v1/v2 migration instrumentation tests.
36. Add Compose tag editor instrumentation coverage.
37. Parse portable Markdown checkbox lines.
38. Add/check/uncheck individual tasks without rewriting unrelated lines.
39. Preserve body characters and Unicode in checklist toggles.
40. Display checklist completion progress on card/editor.
41. Provide cursor-aware Markdown bold/heading/bullet/checklist commands.
42. Test formatting and interactive checklists with JUnit and Compose.

## Gates

CI tasks: JVM tests, compileDebugAndroidTestKotlin, lintDebug, assembleDebug.
A passing CI build does not substitute for device tests: verify keyboard selection/composition,
large fonts, tag scrolling, checkbox TalkBack, tablet sizing, v1/v2 migrations on test databases,
backup with tags, process restart, rapid typing, and undo. The instrumented database is isolated
from real user content.

## Rollback

Revert this branch to the P1 capture commit. Do not uninstall an older APK or delete
the SQLite database; note metadata is preserved during v2→v3 migration.
