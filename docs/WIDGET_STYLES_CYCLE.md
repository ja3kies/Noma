# Noma — Widget themes + capture resilience: delivery checklist

**Branch:** `feature/noma-widget-styles-and-resilience`  
**Base:** `77c9a2b` — confirmed successful earlier Android capture CI.  
**Status:** development work, not release approval. No merge to `main` or database migration.

This is one grouped development cycle, **not an isolated widget-style compilation**.

## Three selectable widget appearances

| Option | Implementation | Native capabilities and limits |
| --- | --- | --- |
| Material You | Widget-specific Material palette drawable, wallpaper-based system neutral/accent resources on Android 12+, fallback on older Android | Follows host/system dynamic colors rather than Noma's internal reading theme |
| Frosted green | Translucent green multi-stop gradient, bright hairline stroke, muted text | Frosted visual approximation, not realtime blur |
| Liquid Glass | Frost-white and pale-blue gradient, translucent layer and light edge | Glass-like artwork; classic Android RemoteViews **cannot guarantee live wallpaper blur or iOS optical refraction** |

Styles are selected in **Appearance → Widget appearance**. This release stores **one global style preference shared by all Noma widget instances**; per-instance styles are not implemented. It updates installed widgets immediately after a successful preference save. All options retain the same quick-capture behavior.

## At least 30 traceable cycle actions

1. Isolate work in a new development branch.
2. Model Material You as a stable enum option.
3. Model frosted green as a stable enum option.
4. Model Liquid Glass as a stable enum option.
5. Default to Material You for unset settings.
6. Fall back safely from invalid or obsolete stored values.
7. Store style preferences separately from notes.
8. Commit preferences durably before updating the widget host.
9. Provide three separate RemoteViews-compatible XML layouts.
10. Keep one consistent 56dp minimum widget touch region.
11. Give all layouts an accessibility description.
12. Keep labels translatable via shared strings.
13. Implement Material You drawable and pre-Android-12 fallback.
14. Implement Android 12+ system dynamic neutral/accent resources.
15. Implement Material You night fallback palette.
16. Implement Android 12+ Material You night palette.
17. Implement frosted-green translucent gradient.
18. Implement frosted-green contrasting label.
19. Implement distinct frosted-green dark gradient.
20. Implement Liquid Glass translucent layered gradient.
21. Implement Liquid Glass contrasting label.
22. Implement distinct Liquid Glass dark gradient.
23. Set the initial widget layout to Material You.
24. Render the stored style on widget update.
25. Refresh installed widget instances when style changes.
26. Reapply style when widget host resizes an instance.
27. Offer a Widget appearance entry in Appearance.
28. Add an accessible three-option radio-style selector.
29. Show distinct palette preview swatches for each option.
30. Translate settings and disclosures into English and French.
31. Explicitly disclose static vs live-blur limitation.
32. Instrument style parsing and invalid preference fallback.
33. Instrument preferences saved/reloaded for every style.
34. Instrument three distinct XML layouts and touch targets.
35. Verify widget resource label/color lookup in instrumentation.
36. Exercise settings selection through Compose instrumentation.
37. Guard launcher shortcut creation and registration against OEM failures.
38. Give deferred quick-note capture a dedicated user message.
39. Localize deferred capture in both languages.
40. Handle preference-storage failures without app startup crashes.
41. Handle widget host refresh failures without crashing Noma.
42. Keep existing notes, SQLite version, and production package unchanged.

**Important:** A checked-in test is not proof of execution. No build may be called validated until CI results for the final SHA establish `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `lintDebug`, `assembleDebug`, and emulator `connectedDebugAndroidTest` success.

## Manual acceptance on Pixel 9

- [ ] Add the widget with Material You, verify Android dynamic palette and label contrast.
- [ ] Change to Frosted green in Appearance, verify every installed widget updates without removal.
- [ ] Change to Liquid Glass, verify legibility on bright and dark wallpaper.
- [ ] Switch system dark/light, update widgets, verify legibility.
- [ ] Resize the widget: selection persists, tap area remains accessible.
- [ ] Use TalkBack and 200% font size.
- [ ] Tap the widget from a closed app: a new note opens with focus.
- [ ] Tap while editor has an uncommitted save: draft persists and new note opens after saving.
- [ ] Deny shortcut updates on an OEM launcher: Noma still opens.
- [ ] Reboot with test notes: preferences remain, widget opens and has no unintended data access.
- [ ] Verify stable signing before any APK replaces an existing user installation.

## Rollback and safety

The branch is isolated. Revert the development branch to `77c9a2b` if necessary, after preserving a separate backup of test notes. Avoid force-pushing, merging, uninstalling previews with data, or modifying app signing without authorization. A widget never reads private note content. A selected style is **global**, not per-widget. Static translucency is best-effort depending on launcher support.
