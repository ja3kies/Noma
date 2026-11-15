# PR #16 — editor audit follow-up

Base: `b4474da69984d6f0de7166d7790e7b604fd2e883`. The preceding CI passed 150 Android tests. That does not validate the additional regressions below.

## Scope and decisions

Preserve the three existing editor improvements (editorial canvas, Hugeicons dock, sticky save recovery). Refine their demonstrated weaknesses; do not redesign the library, add rich text, migrate storage, change dependencies, or merge main. Reuse MIT Hugeicons and Compose primitives. The app's MainActivity already updates system-bar appearance; the blank light status bar in the old ComponentActivity test screenshots is not evidence of a production system-bar bug.

## 1. Keep the active writing line visible

- Evidence: snapshot 03 clips the active line when the sticky error reduces the viewport; snapshot 05 does not demonstrate cursor visibility after font enlargement.
- Root-cause hypothesis: the body grows unbounded in the document scroll container. A resize can leave the existing cursor outside the new viewport without changing selection or focus.
- Regression: long note, real IME, known selection; compare the cursor rectangle from actual TextLayoutResult against the clipped viewport. Repeat after an actual failed write and after font scale 2.0.
- Target: retain the same document/selection and request visibility for the actual cursor rectangle when focused layout/viewport changes. Do not force scrolling on every scroll gesture or act when the field is unfocused.
- Native reference: https://developer.android.com/reference/kotlin/androidx/compose/foundation/relocation/BringIntoViewRequester

## 2. Stabilize the editor tool dock

- Evidence: export disappears with the IME; search and bullet tools shift positions. The keyboard icon does not express collapse.
- Regression: compare horizontal tool centers before/after opening the real IME; export remains reachable while writing.
- Target: stable export/search/bullet/collapse slots, minimum 48dp targets, localized native tooltips, clear directional Hugeicon for collapse, restrained width and shadow. Disable collapse when there is no IME rather than moving other actions.
- Preserve current search, bullet, export and keyboard behavior. Test narrow screens and large text; do not shrink touch targets.
- Native reference: https://developer.android.com/develop/ui/compose/components/tooltip

## 3. Make failed-save recovery coherent

- Evidence: status and guidance each declare a live region. Portable per-note export is disabled even though it already serializes the selected current Note rather than reading the persisted database.
- Regression: exactly one live announcement in the failure banner, Retry remains independently actionable, repeated failure retains text; failed draft can open TXT/Markdown export.
- Target: one parent live region for status/guidance with separate actionable Retry. Keep descendant test tags and accessibility actions available. Permit exporting the current note snapshot during failure without marking it saved or relaxing collection-backup consistency guards.
- Keep repository, autosave ordering, Back/confirmation, and full-library export protections unchanged.
- Native reference: https://developer.android.com/develop/ui/compose/accessibility/semantics

## Verification

1. Commit regressions first; execute on unchanged production code and record actual failures.
2. Apply minimal fixes, compile instrumentation, run JVM/lint/APK checks and the full Android suite.
3. Review regenerated light, IME, error, dark, and large-font screenshots, adding error + large-font coverage. Use the same synthetic document and selection for comparable states.
4. Preserve FR/EN strings, focus/selection, original document text, 360–412dp phone and two-pane behavior. Distinguish automated semantics checks from an actual TalkBack session.
5. Physical Pixel 9 testing, actual TalkBack speech, and process restart cannot be claimed without an available device. Leave PR draft and unmerged.

## Observed RED baseline

Commit `37de4661a99cf844668942e1a99ee1462e9c5d57`, run https://github.com/ja3kies/Noma/actions/runs/43715657909: JVM/lint/build succeeded; 155 Android tests ran with six failures. All five new regressions failed on unchanged production code. Both cursor tests already failed when selecting a distant line, before the subsequent resize: native relocation of the unbounded field was insufficient even for selection changes. Search moved from x=540px to x=304px; the error had two live regions; draft export was disabled.

An existing library geometry test also failed because it sampled layout after only the model query changed, without opening/waiting for the real IME. Its test now opens the actual keyboard and waits for the required settled geometry, preserving the assertion and library production code.
