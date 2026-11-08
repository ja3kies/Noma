# Noma — Design system and UX audit

**Positioning:** a calm, private, offline-first writing space. Noma draws on iOS clarity and restrained hierarchy while retaining Android-native navigation, sharing, touch semantics and Material 3 components. Never copy Apple UI pixel-for-pixel.

## Reference framework

| Source | Principle adopted in Noma |
|---|---|
| [Material 3 / Expressive](https://m3.material.io/) | Semantic colors, typography, shapes, motion and interaction states |
| [Jetpack Compose](https://developer.android.com/develop/ui/compose) | Composable interface and separation between view, state and persistence |
| [Android adaptive design](https://developer.android.com/develop/adaptive-apps/guides/list-detail) | Responsive list-detail navigation and readable content width |
| [Apple Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines/design-principles/) | Familiar hierarchy, readable writing surface and concise language |
| [Android accessibility](https://developer.android.com/guide/topics/ui/accessibility/apps.html) | Descriptive actions, 48dp+ touch targets and font scaling |
| [WCAG](https://www.w3.org/WAI/standards-guidelines/wcag/) | Contrast, understandable actions and visible system status |
| [Nielsen Norman Group heuristics](https://www.nngroup.com/articles/ten-usability-heuristics/) | Feedback, undo, consistency, error prevention and recovery |

**Visual benchmarks:** Apple Notes (fast capture and familiar list hierarchy), Bear (editor typography), Craft (surface polish), Google Keep (fast access) and Simplenote (minimal friction). This is a study of decisions, not a collection of templates to imitate.

## Noma tokens

All functional Compose screens use `MaterialTheme.colorScheme` and `MaterialTheme.typography` roles, with `NomaSpacing`, `NomaMotion`, and `NomaAppearance` in `ui/theme/NomaTheme.kt`.

- **Color:** warm paper neutrals, ink-blue primary emphasis and restrained amber. Semantic contrast and surfaces differ by light/dark appearance.
- **Typography:** 32sp page titles, 20sp headings, 16sp comfortable writing text and 14sp previews. Size uses sp so system scaling is respected.
- **Shapes:** 8 / 12 / 18 / 24 / 32dp hierarchy. Avoid pill shapes on every surface.
- **Spacing:** 4 / 8 / 12 / 16 / 24dp rhythm.
- **Elevation:** subdued cards and FAB, selected-note outline rather than heavy shadows.
- **Motion:** 160ms quick and 230ms standard easing references. Feedback is subtle, never a blocker for typing.
- **Themes:** explicit System / Light / Dark choices; on-device persistence; status/navigation bar foreground adjusts accordingly.

## Three screen specifications

### 1. Home — notes, pins and recents

Prominent Noma heading, clear search, Archive/Pinned filters, a sort button that does not vanish at smaller widths, stable sections for Pinned / Recent, note cards with title, preview and edited date, and a persistent New note action. The empty notebook includes a text CTA; filtered no-results states explain what happened. Destructive actions have confirmation or undo.

### 2. Editor — focus and honest autosave

New blank notes focus the title and open the keyboard. Next advances to body. The text area grows naturally instead of being fixed at 500dp. The toolbar groups pinning and more actions; archive, delete and share use recognizable iconography with labels. Save status reads **Saving**, **Saved on this device**, or **Not saved** based on actual SQLite completion. Error status exposes Retry and a warning before leaving.

### 3. Search and empty results

Instant title/body search filters the active or archived list and preserves sorting. The search field has a clear action and keyboard Search IME. The no-results state is distinct from an empty notebook. Note items use the first nonempty body line as a title fallback when needed.

## Adaptive contract

- Under **840dp available width**: list and editor occupy the full available window, with Android Back navigation.
- At **840dp and above**: a **400dp list** beside an editor with a **760dp maximum content width**.
- Keyboard insets and edge-to-edge system bars are handled on both layouts.
- Archive and sorting remain usable on small widths through horizontally scrollable filter controls and a persistent sorting icon.
- Dynamic type and TalkBack must be verified on-device at several font scales; code compilation alone is not a visual accessibility audit.

## Issues found in the preceding version

| Finding | Change in this iteration |
|---|---|
| Saving status was a static claim regardless of write success | Revision-aware Saving / Saved / Failed and Retry |
| The body field had fixed height | Expanding, scrolling body |
| Tablet layout was stretched phone UI | Two-pane layout with limited editor line length |
| Theme followed the system only | Saved System / Light / Dark selection |
| Action icon resembled a document rather than a menu | More-options icon and labeled actions |
| First-use empty state had no explicit CTA | New note text action |
| No quick native share | Android sharesheet for plain text |
| Instrumentation reset the production database | Separate instrumentation-only database |
| Preview signing could conflict with previous installations | A distinct Design Preview package name |

## Verification standard

**Automated:** assembleDebug, testDebugUnitTest, compileDebugAndroidTestKotlin, lintDebug. CI artifacts are temporary and use disposable debug signing.

**Device checks still necessary:** small phone, large font/TalkBack, large screen and landscape, keyboard/IME, theme switches, long-note scrolling, text selection, search, archive/restore, delete/undo, disk-full error path, rotation, process restoration, and safe signed upgrades. Do not claim these have passed until actually executed.

## Out of scope for this design integration

No rich text, checklists, image attachments, folders, import/export, secure locking, widget or cloud sync yet. A credible switch from existing note apps will require portability and lifecycle trust first, then selected differentiators based on actual user feedback.
