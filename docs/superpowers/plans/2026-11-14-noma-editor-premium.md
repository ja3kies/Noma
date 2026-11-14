# Noma Premium Editor — three verified improvements

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:executing-plans, superpowers:test-driven-development and superpowers:verification-before-completion; every task ends in a CI gate.

**Goal:** Three independently testable Compose editor UI improvements without changing local data or flows.
**Architecture:** Existing `NoteEditorScreen` remains state owner. Change only editor composables, editor-focused spacing tokens, MIT-licensed drawable resources, and editor-focused instrumentation tests. No new dependencies.
**Tech Stack:** Kotlin, Compose Material 3, Gradle, Android 35 emulator.
**Spec:** `docs/superpowers/specs/2026-11-14-noma-premium-editor-design.md`

## Global constraints
Keep autosave/retry and draft data intact, existing localized copy and testTags, 48dp interactive targets, 360dp phone + 840dp two-pane layout, IME adjustResize, FR/EN, light/dark, standard animation scale, `main` unmodified. Source Hugeicons only from `hugeicons/hugeicons/icons/*.svg` MIT free icons; preserve upstream LICENSE.md. Do not claim Pixel testing.

## Review focus
- Visible/hidden keyboard changes insets but must not reorder title, metadata and body.
- Small / large font screens must not clip the editor toolbar or Retry.
- A failed write must keep text intact, show actionable Retry and guidance, including after repeated failures.
- Rapid typing must not cause focus loss or a continuously shifting autosave notice.
- Accessibility labels and back actions must remain reachable in both themes.

## Task 1: Editorial canvas
**Files:** `app/src/main/java/dev/juuri/noma/ui/NomaApp.kt`, `ui/theme/NomaTheme.kt`; test `app/src/androidTest/java/dev/juuri/noma/ui/NomaUiTest.kt`.
**Interface:** existing `NoteEditorScreen(note, saveState, viewModel, snackbar, expanded)`; keep text fields testTags and persistence callbacks unchanged.
- [ ] Add regression test for last-edited metadata remaining in editor while IME visible and for no duplicated title heading in expanded editor.
- [ ] Run connected UI tests; expect RED for metadata condition.
- [ ] Keep metadata in layout regardless of keyboard; use a narrow editorial column and neutral lightweight top label.
- [ ] Rerun connected UI tests and baseline; expect GREEN.
- [ ] Commit only Task 1 code and tests.

## Task 2: Hugeicons pill dock
**Files:** `app/src/main/java/dev/juuri/noma/ui/NomaApp.kt`; new `app/src/main/res/drawable/noma_huge_*.xml` and `third_party/hugeicons/LICENSE.md`; test `NomaUiTest.kt`.
**Interface:** retain `noma_editor_toolbar` and editor action tags, add `noma_editor_dock`.
- [ ] Add test asserting dock tag and width < 90% editor viewport; confirm actions are still tappable.
- [ ] Run test; expect RED because no dock exists.
- [ ] Port only verified free Hugeicons path data to resource vectors, 24x24dp stroke 1.5, tint via MaterialTheme; embed MIT notice.
- [ ] Replace flat full-width editor toolbar with centered 320dp-maximum rounded Surface and existing actions; retain IME and nav inset handling.
- [ ] Rerun unit + instrumentation; confirm no action regression.
- [ ] Commit only Task 2.

## Task 3: Sticky recovery surface
**Files:** `app/src/main/java/dev/juuri/noma/ui/NomaApp.kt`; test `NomaUiTest.kt`.
**Interface:** preserve `noma_editor_save_bar`, `noma_retry_save`, `noma_save_failure_guidance`; no save data model changes.
- [ ] Add regression test that Retry is inside the sticky save-status surface and remains displayed with failed writes.
- [ ] Run test; expect RED because Retry currently sits in TopAppBar.
- [ ] Place status, guidance and Retry together in a responsive tinted surface, leaving top bar for navigation and actions. No duplicated retry controls.
- [ ] Rerun all tests on Android 35 emulator; test multiple failed retries and recovery, IME & long content.
- [ ] Commit only Task 3.

## Evidence and adversarial review
- [ ] Capture screenshots on emulator for normal note, IME, long note, dark theme and failed-save state, using a fixture note and no personal data; publish alongside Android test reports.
- [ ] Inspect screenshots at same emulator resolution and test font-scale, dark theme and TalkBack semantics where available.
- [ ] User scenario: create, type, bullets, find and replace, hide keyboard, save retry, back; fail if text moves, vanishes, or actions are unreachable.
- [ ] Competitor audit: rank title hierarchy, contrast, whitespace, alignment, tap areas and animation coherence; fix all serious issues before signoff.
- [ ] Verify green JVM tests, Android instrumented tests, lint, APK and artifact SHA on the exact final SHA.
- [ ] Leave draft PR unmerged, record screenshot limitations and what Pixel testing remains.
