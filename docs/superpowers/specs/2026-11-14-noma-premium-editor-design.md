# Noma editor — premium native writing (14 November 2026)

## Intent
Rebuild **only the editor surface** into a calm, expressive Android-native writing experience. Keep existing Noma/Material 3 botanical theme, all persisted data, autosave, drafts, retry, search/replace, export, back handling, two-pane layout, and FR/EN accessibility. No redesign of library, migrations or destructive actions.

## Research and design choices
- **Apple Notes**: the note title is the clear editorial focal point; metadata and actions recede. See https://support.apple.com/guide/iphone/create-and-format-notes-iph2dbd3a2a9/ios.
- **Bear editor**: editing tools stay contextual and compact rather than taking over the document; see https://bear.app/faq/how-to-use-bears-editor/.
- **iA Writer**: calm single-column text and minimal chrome keep attention on writing; see https://ia.net/writer/how-to/write-with-focus.
- **Material 3 Compose**: stable IME insets, semantic color roles, minimum 48dp tap targets, keyboard-friendly actions. See https://developer.android.com/develop/ui/compose/components.

Alternatives considered: (a) a full rich-text editor duplicates unsupported functionality and risks storage compatibility; (b) an immersive borderless editor hides essential error/retry controls. Selected: refine the **existing functional editor** via the following three independent changes, using native Compose surfaces and existing state.

## Exactly three product changes
1. **Editorial canvas:** de-duplicate the top-bar title; constrain reading column to 640dp; keep read-only last-edited metadata in the writing hierarchy while IME opens/closes (avoid a jump in body position). Preserve multiline title, focus, text composition and actual note data.
2. **Compact floating tool dock + Hugeicons:** replace full-width flat bottom toolbar with one centered rounded contextual surface, retaining search/replace, bullet toggle, export and hide-keyboard. Use verified **free** Hugeicons Stroke Rounded icons for editor top controls and dock; no proprietary Pro assets, keep MIT copyright/permission notice. Preserve each existing 48dp button and testTag; editor remains usable above IME, in dark mode and on tablets.
3. **Actionable autosave state:** collapse the duplicate retry action and failure message into one sticky, error-colored compact region directly beneath the top app bar. Failed status + explicit recovery guidance + Retry share one surface so the command stays available with long content and keyboard open. Keep normal saved/saving behavior and delayed feedback; never discard unsaved text or delay error visibility.

## Quality contract & adversarial checks
Test empty/long notes, long title and multi-line text, IME on/off, 360–412dp screens, 840dp two pane, themes including dark, 1.5–2.0 font, save success/slow/failure/retry, TalkBack semantics, rapid taps, animation interruption, Back, export, find/replace, selected text and process restore. Reuse theme and motion tokens, no extra dependency. A redesign is not complete on successful compilation alone: verify rendered emulator screenshots, inspect crowding/contrast/touch targets, exercise actions and follow up on any failed tests.

## Isolation
Branch: `feature/noma-editor-premium-3-corrections`; base: `feature/noma-replica-notes-rebirth` (PR #15). Do not merge, release or modify stable package data. CI must succeed for `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug` and `:app:connectedDebugAndroidTest`. Snapshot evidence from emulator is required for visual signoff; a Pixel 9 manual check remains separate.
