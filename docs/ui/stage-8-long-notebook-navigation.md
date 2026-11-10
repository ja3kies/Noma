# Noma · Stage 8 — Long notebook navigation and purposeful motion

**Branch:** `feature/noma-ui-stage8-long-notebooks`, isolated from stage 7. CI runs separately; merging remains strictly blocked until both stages pass all gates.

## Implemented work (20 items)

| # | Action | Code |
|---|---|---
| 08.01 | Introduce an explicit Back to top action for long note lists | `noma_scroll_to_top` |
| 08.02 | Use native LazyListState.animateScrollToItem for continuous scroll-to-top motion | `listScrollScope.launch` |
| 08.03 | Keep search, sorting and stored notes intact while navigating to the top | `scroll callback` |
| 08.04 | Show jump action only beyond four visible list items | `isDeepInList` |
| 08.05 | Hide jump action until the notes store is Ready | `bottomBar visibility` |
| 08.06 | Hide jump action when the current filtered result set is empty | `bottomBar visibility` |
| 08.07 | Hide jump action when Android's IME is visible | `imeVisible` |
| 08.08 | Gate list scroll recomposition with remember/derivedStateOf | `isDeepInList` |
| 08.09 | Fade and expand the jump action smoothly into the bottom toolbar | `AnimatedVisibility enter` |
| 08.10 | Fade and shrink the jump action out without leaving a blank toolbar gap | `AnimatedVisibility exit` |
| 08.11 | Use a 48dp minimum target for the jump action | `scroll button` |
| 08.12 | Localize its label in English and French and hide its decorative icon from TalkBack | `scroll_to_top` |
| 08.13 | Use semantic secondary-container colors instead of fixed accent values | `jump action background` |
| 08.14 | Place jump action at end of a capped-width row above bottom search | `bottomBar` |
| 08.15 | Collapse the home tagline when the notebook has been scrolled deeply | `header AnimatedVisibility` |
| 08.16 | Restore the tagline automatically when returning to the top | `header visibility` |
| 08.17 | Animate the header dimensions with a no-bounce spring | `header animateContentSize` |
| 08.18 | Memoize note partition into pinned and regular groups | `remember(visible, archivedOnly)` |
| 08.19 | Use a proper sorting icon for the alphabetical section | `SectionHeader icon` |
| 08.20 | Localize section count semantics rather than joining English-style strings | `section_count_description` |

These items are stage 8 refinements; none is a repeat of stages 1–7.

## Automated regressions

Three new Compose UI tests cover:
1. Jump from long notebook to the top without changing notes or the bottom search pill.
2. Collapse/restore the tagline when scrolling and returning.
3. Preserve title-based sort order through the jump.

## Acceptance gates

- [x] Stage 8 changes prepared on a separate branch, not merged into the earlier PR
- [x] New Android tests and French/English strings staged
- [ ] Stage 7 push and PR CI completely PASS
- [ ] Stage 8 JVM compilation, lint, debug APK and emulator UI tests PASS
- [ ] Pixel 9 verification of IME, landscape, long lists and motion settings
- [ ] User approves merge of stage 7 and stage 8

## References

- [Android — Compose lazy lists](https://developer.android.com/develop/ui/compose/lists)
- [Android — Compose performance best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices)
- [Android — Compose animation](https://developer.android.com/develop/ui/compose/animation/quick-guide)
- [Apple — Search in a bottom toolbar](https://developer.apple.com/design/human-interface-guidelines/search-fields)

No repository/database code, package identity, file data, or authentication changed. To roll back, retain the stage 7 branch and drop the staged stage 8 commit.
