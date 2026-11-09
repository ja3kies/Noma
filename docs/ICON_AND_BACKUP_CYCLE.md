# Noma — Icon replacement and next delivery improvements
Date: 2026-11-09  
Branch: `feature/noma-widget-styles-and-resilience`  
Pre-icon baseline: `c545a5f` (CI green); stable `main` untouched

## Brand asset provenance

User supplied `Noma_Icone_Vectorielle_Android.zip` with the olive-to-lime diagonal pen, ivory background, and handwritten flourish.

Integrated into Noma:
- `drawable/noma_launcher_foreground.xml` — **byte-identical** to the ZIP's VectorDrawable, Git blob `2ef5114b560a84097cb8e6075408c684825819f8`.
- `drawable/noma_launcher_monochrome.xml` — **byte-identical** to the ZIP's monochrome VectorDrawable, Git blob `9d0f12fe240cacabc8a625fabf434ea8a22e29c6`.
- `drawable/noma_launcher_background.xml` — same ivory gradient and native shape.
- `mipmap-anydpi-v26/noma_launcher.xml` and `noma_launcher_round.xml` — adaptive configurations with background, foreground and Android 13 thematic monochrome.
- `AndroidManifest.xml` — both normal and round launcher icons reference `@mipmap`.
- Dynamic new-note launcher shortcut uses the same `@mipmap/noma_launcher`.
- All three styled widgets plus the compatibility widget now use the matching transparent vector artwork.
- Legacy `ic_noma_launcher.xml` is a composite alias to the new brand, not the old artwork.
- `design/branding/noma_icon.svg` and `noma_monochrome.svg` retain editable supplied design sources.

**Android minSdk = 26**: Android 8+ supports adaptive icons. The ZIP's PNG densities for pre-26 fallback are therefore deliberately **not included** in the APK. The adaptive vector avoids resolution loss and provides masking/theme support.

## Source and release checks — 29 individually verifiable actions

1. Inspect uploaded ZIP file list.
2. Inspect README to confirm adaptive safety area and source role.
3. Render/inspect the supplied preview raster for visual approval.
4. Review foreground XML geometry.
5. Review monochrome XML geometry.
6. Review adaptive icon declarations.
7. Confirm minSdk 26 prior to dropping redundant PNGs.
8. Add the native adaptive foreground XML.
9. Add the native gradient background XML.
10. Add the native monochrome XML.
11. Add the primary adaptive icon XML.
12. Add the round adaptive icon XML.
13. Change manifest primary icon to the adaptive resource.
14. Change manifest round icon to the adaptive resource.
15. Change dynamic shortcut icon to the new resource.
16. Change Material You widget art to the new vector.
17. Change Frosted Green widget art to the new vector.
18. Change Liquid Glass widget art to the new vector.
19. Change legacy widget layout art to the new vector.
20. Change compatibility drawable to new brand.
21. Correct/verify exact supplied Bézier path in foreground.
22. Correct/verify exact supplied Bézier path in monochrome.
23. Preserve full-color SVG source under design/.
24. Preserve monochrome SVG source under design/.
25. Add instrumentation assertions for manifest adaptive/round icon IDs.
26. Add instrumentation assertions for vector foreground, background and monochrome availability.
27. Add instrumentation for all three widget vector icons.
28. Add instrumentation for compatibility icon alias.
29. Preserve existing app package/database and prevent destructive installation.

## Separate functional advance: verified backup and import performance

- Exporting JSON now writes and then attempts to read the selected document.
- Matching exact byte length and SHA-256 yields `VERIFIED`, otherwise corrupted/truncated readbacks fail.
- A storage provider denying readback yields **UNVERIFIED_PROVIDER** and a prominent user-facing message, not an unsupported success claim.
- Blank, oversized or absent backup destinations are rejected before a successful response.
- Regression JVM tests cover identical bytes, corruption, truncation, missing destinations, too-large input and unreadable providers.
- JSON import collision preview is now O(n + m), not nested O(n × m) list scans, via a pure helper and tests including 5,000 existing + 5,000 incoming notes.
- None of this changes SQLite schema, note content, archived/pinned flags, or backup format version.

## CI acceptance gate

- [ ] Latest **commit** passes `testDebugUnitTest`.
- [ ] Latest commit passes `compileDebugAndroidTestKotlin`.
- [ ] Latest commit passes `lintDebug`.
- [ ] Latest commit produces a verified `assembleDebug` artifact.
- [ ] Latest commit executes `connectedDebugAndroidTest` on an Android emulator.
- [ ] On Pixel 9 verify adaptive icon in launcher grid, long-press shortcut, rounded mask and themed monochrome.
- [ ] Verify widgets in all three appearances and light/dark wallpapers.
- [ ] Export at least one disposable JSON archive to Android Files, reopen it and check import into isolated test installation.
- [ ] Verify no replacement installation would remove any existing personal notes.

Never claim device validation merely from successful CI. The APK is a debug-design preview with a signature that can vary between runs. No release, package rename or stable merge is approved.

## Rollback

If brand rendering or a regression fails, use a separate branch from the known-good `c545a5f` or `77c9a2b`; keep the affected branch intact for investigation. Restore only Git revisions, never delete SQLite data or uninstall a copy containing notes. Previous paths are available in Git history.
