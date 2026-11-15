# Noma Reborn — concept-to-Compose integration

**Source of visual direction:** original Noma Reborn concept board and the editable [Figma exploration](https://www.figma.com/design/twjFzxB9jjaKoisZlUavKD). The goal is to adapt its hierarchy, calm surfaces and interaction patterns to the real Android application, not reproduce an unrelated product's assets.

## Implemented in this branch

| Area | UI concept | Compose integration |
| --- | --- | --- |
| Semantic theme | botanical / warm paper palette | New light and dark Material 3 roles in `NomaTheme.kt`; all existing surfaces inherit them |
| Appearance | six visible accent swatches | Forest, Sage, Honey, Coral, Ink, Sky; persisted in `MainActivity` and applied live to primary tokens |
| Library | “Mes notes”, light supporting subtitle | Localized EN/FR titles; archive heading remains context-aware |
| Filters | clear selected pill state | “Toutes”/“All notes” resets archive or pinned scope without clearing typed search; pinned chip precedes archive |
| Note cards | compact paper cards and colored markers | Rounded cards, 44dp glyph tile, selected state, preserved pinned/archive semantics and contextual menu |
| Search and create | one unified bottom dock | Always-present search alongside 56dp create FAB; keyboard insets, search cancel and blocked-save behavior retained |
| Editor | writing-first hierarchy | Read-only last-edited metadata below title; body stays continuous, selectable and unchanged in persistence |
| Appearance previews | miniature app surfaces | Light/dark/system thumbnails follow selected accent, with radio selection and accessible contrast |

### Guardrails

- Do **not** fake folders, nested checklists, document scanning, or 30-day deleted items merely because they are in the mockup. Those require separate data models, proper migrations and backed screens.
- No app storage schema or export format was changed in this UI slice.
- Test tags remain stable for existing editor, notes list, export, filters, FAB and search integration.
- Accent palettes are original Noma tokens, not assets or branded colors extracted from another app.
- Use semantic color roles across all components, and keep Android-native system bars, Back and TalkBack.

## Reference implementation decisions

- [Material 3](https://m3.material.io/): chip selection, surface roles, error states and elevation.
- [Android Compose adaptive](https://developer.android.com/develop/ui/compose/layouts/adaptive): retain the tablet split-view and constrained writing width.
- [Apple HIG](https://developer.apple.com/design/human-interface-guidelines/): meaningful hierarchy, restrained chrome, direct writing path; do not clone protected visuals.
- Figma concept: compact, pill-shaped bottom actions, high-contrast selected states, airy list organization.

## Contrast and accessibility

Light selected accent versus white text: > 5.5:1 across all six accent colors.
Dark accent versus dark selected text: > 8.5:1 across all six accent colors.
Main light background versus text: > 13:1. Contrast values are based on declared sRGB tokens; these checks are also encoded as JVM unit tests.

All accent controls have 52dp targets, localized accessible labels, radio-button semantics, and a checked state. The picker wraps to three swatches per row on compact phones and six on wide screens. The old Lilac preference migrates to Ink, so no violet accent remains.

## Test gates / current limitations

Added tests:
- `NomaAccentTest` for six palettes, storage round-trip and text contrast.
- Compose instrumentation for the All filter, dock geometry, editor metadata, and accent selection.
- Existing Android UI and persistence tests must continue to pass.

**A passing APK build has not been verified**. Hosted GitHub Actions are currently blocked at runner allocation by the account's 0-dollar Actions budget. Local Termux is the authorized no-payment route:

```bash
cd ~/projects/Noma
git fetch origin
git switch feature/noma-replica-notes-rebirth
git pull --ff-only origin feature/noma-replica-notes-rebirth
bash scripts/build-termux-noma.sh --full
```

Install the resulting preview APK only after a passing build. Validate on-device: light/dark palettes, every accent, screen-reader states, note creation/search, share/export, large font/IME, editor autosave error recovery, tablet layout and process restart.

## Next UI slices

Once the underlying features exist, implement screens for folders/subfolders, structured checklist editing, recoverable trash, rich note blocks and media attachments. Reuse this design system rather than adding placeholder UI that suggests unfinished features work.
