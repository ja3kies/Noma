# Noma — Cycle UX 3 : search relevance, highlighting, unsaved-draft safety

Last cycle reference: `55cfc47` (CI failed lint, not approved). This cycle remains on `feature/noma-ux-search-clarity`; no changes to `main`, storage schema, app signing, package identifiers or user data.

## Minimum 25 individually checkable changes and regression checks

1. Replace forbidden `LocalContext.current.getString` access with Compose `stringResource` for search semantics.
2. Retain an explicit, screen-reader-readable search label.
3. Add deterministic exact-title search relevance.
4. Add prefix-title relevance.
5. Add all-query-terms-in-title relevance.
6. Rank partial-title hits ahead of body-only hits.
7. Keep previously selected sort as a tie-breaker.
8. Preserve pinned and recent ordering when the query is empty.
9. Test exact-title vs newer body-only matches.
10. Test relevance across titles and bodies.
11. Test unchanged normal notebook ordering.
12. Test title sorting for equal relevance.
13. Map folded Unicode text positions back to original UTF-16.
14. Support accent-insensitive highlighted spans without modifying note strings.
15. Support ligature-equivalent highlighted spans.
16. Keep emoji and supplementary characters intact in highlighted strings.
17. Merge overlapping highlighted ranges.
18. Bound highlighted matches to prevent unbounded spans per card.
19. Test highlighted accents.
20. Test highlighted ligatures.
21. Test highlighted emoji-safe ranges.
22. Test overlapping span merging and blank queries.
23. Highlight note titles with Material 3 primary token and bold weight.
24. Highlight contextual note excerpts without changing editor text.
25. Reject navigation from a pending or failed editor save.
26. Reject creating a second note when an active draft has not saved.
27. Reject deleting the active note while its last revision is unsaved.
28. Reject archiving the active note while its last revision is unsaved.
29. Protect an in-flight save when the already-selected card is tapped again.
30. Show a translated, non-destructive snackbar explaining why navigation is blocked.
31. Add French and English strings for blocked-navigation guidance.
32. Test switching notes with a deferred write.
33. Test same-note selection during deferred save.
34. Test delete/archive/new-note actions after simulated disk failure.
35. Preserve existing SQLite schema and full import/export compatibility.

These are source-level and test-artifact checks; writing tests is not the same as executing them.

## Release gate
- [ ] `testDebugUnitTest` green on **last** commit.
- [ ] `compileDebugAndroidTestKotlin` green on last commit.
- [ ] `lintDebug` green on last commit.
- [ ] `assembleDebug` green on last commit.
- [ ] `connectedDebugAndroidTest` green on last commit.
- [ ] Long-press/rotation/device re-open check on Android with disposable notes.
- [ ] Verify no copy of real notes is used for instrumentation.
- [ ] Document any remaining UX or accessibility limitations before APK is presented as ready.

## Rollback
The unchanged baseline for this UX branch is `e391d6a`; the last emulator-validated UX checkpoint is `4fcdbf5`. Revert only within a development branch and rebuild. Never uninstall an app containing notes or overwrite the installed signing key. No change is merged to main without approval.

## Limitations
- Accent normalisation, relevance calculation and annotations still need an on-device performance benchmark with thousands of notes; no latency is claimed.
- Highlighting is visual only and should not add duplicate verbal announcements to TalkBack.
- Editing while a save is in flight remains possible; navigating away is deliberately blocked until the latest write is acknowledged.
