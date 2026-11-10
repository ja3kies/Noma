# Noma · UI step 1 — Bottom search & home controls

Base: `a88b43a5872e573a7883a6d16760ce94dec1e3a0` — GitHub Actions #333 passed build, JVM, lint and emulator tests.
Branch: `feature/noma-ui-stage1-search-15`. Only presentation, localization and UI instrumentation are modified.
Data layer, database, repository, APIs and original UX branch remain untouched.

## New, individually reviewable changes

| ID | Change | Code location | Acceptance |
|---|---|---|---|
| S1-01 | Focused search uses an animated primary-colored outline | `BottomSearchPill` | Border changes on focus |
| S1-02 | Focused search surface transitions to semantic surface color | `BottomSearchPill` | Light and dark themes use tokens |
| S1-03 | Spring elevation increases on focus without bounce | `BottomSearchPill` | Focus responds smoothly |
| S1-04 | A separate Cancel action exits search without erasing its query | `BottomSearchPill` | Query remains after Cancel |
| S1-05 | Cancel slides and fades with active focus | `BottomSearchPill` | No sudden disappearing action |
| S1-06 | Clear resets search text while retaining focus | `NotesListScreen.onClear` | A subsequent query needs no re-focus |
| S1-07 | Search placeholder adapts to archived notes | `NotesListScreen` | Archive-only hint is explicit |
| S1-08 | Search placeholder adapts to pinned notes | `NotesListScreen` | Pinned-only hint is explicit |
| S1-09 | Search result count appears next to the bottom pill | `NotesListScreen.bottomBar` | Visible while searching |
| S1-10 | Search result count announces updates politely to accessibility tools | `noma_search_result_count` | Live region present |
| S1-11 | Old header result count is hidden while bottom count is shown | `NotesListScreen` | No duplicate visible summary |
| S1-12 | Focusing search closes an open sort menu | `onSearchFocus` | No overlay conflict |
| S1-13 | Selected Archive chip uses a check icon | Archive FilterChip | Selected state visually explicit |
| S1-14 | Selected Pinned chip uses a check icon | Pinned FilterChip | Selected state visually explicit |
| S1-15 | Archive filter has a minimum 48 dp target | Archive FilterChip | Tap target meets Android guidance |
| S1-16 | Pinned filter has a minimum 48 dp target | Pinned FilterChip | Tap target meets Android guidance |
| S1-17 | Pinned filter has a stable test tag | `noma_pinned_filter` | Regression target discoverable |
| S1-18 | Sorting by title highlights the sort control | `noma_sort_button` | Active mode visible |
| S1-19 | Sort icon tint follows selected Material theme state | `noma_sort_button` | Contrast in light/dark |
| S1-20 | Choosing a filter dismisses the sort popup | FilterChip callbacks | No stale popup |
| S1-21 | New Note FAB contracts while the IME is showing | `ExtendedFloatingActionButton` | Notes retain more visible room |
| S1-22 | Compact FAB retains its spoken action label | `noma_new_note` | Screen reader still identifies create |
| S1-23 | Reset-filters action has at least 48 dp height | `noma_clear_filters` | Accessible when IME is visible |
| S1-24 | Reset-filters action includes a recognizable clear icon | `noma_clear_filters` | Faster identification |

The bottom pill, the keyboard safe-area handling and the editor from earlier steps are **not** counted here as new work.

## Tests added

Nine Android UI tests cover Cancel vs Clear, repeated search, result count placement, empty-count transition, archive scope, pinned scope, query preservation after sorting, FAB accessibility and simultaneous query/filter reset. Existing tests remain enabled.

## Validation gates

- [x] Implemented in isolated Git commits.
- [x] Static inspection of tags, resource keys and primary UI code.
- [ ] JVM tests and lint **PASS for this branch**.
- [ ] APK compilation **PASS for this branch**.
- [ ] All emulator UI tests **PASS for this branch**.
- [ ] Pixel 9 visual review with the physical keyboard/IME and portrait/landscape.
- [ ] Approval to merge; leave original branches unchanged until then.

References:
- Apple HIG Search fields: https://developer.apple.com/design/human-interface-guidelines/search-fields
- Android insets: https://developer.android.com/develop/ui/compose/system/insets-ui
- Android minimum tap targets: https://developer.android.com/develop/ui/compose/accessibility/api-defaults
- Android Compose motion: https://developer.android.com/develop/ui/compose/animation/quick-guide
