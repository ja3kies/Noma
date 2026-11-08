# P1 capture — quick entry, privacy and native Android integration

**Branch:** `feature/noma-p1-capture`, stacked on P0 portability.

## Checklist (30 independent implementation/control actions)

1. Add a pure inbound-text parser.
2. Reject null input.
3. Reject whitespace-only input.
4. Preserve meaningful body whitespace.
5. Preserve Unicode and international text.
6. Limit inbound body length to 100,000 characters.
7. Limit shared title to 256 characters.
8. Normalize title newlines.
9. Create a single database note from shared title/body.
10. Make note insertion complete before reporting saved.
11. Open the newly captured note in the editor.
12. Expose an Android native `ACTION_SEND text/plain` receiver.
13. Expose a system text-selection `ACTION_PROCESS_TEXT` receiver.
14. Convert external intents into a safe internal explicit activity intent.
15. Avoid reading arbitrary inbound file/URI content.
16. Reuse an existing editor Activity through `CLEAR_TOP|SINGLE_TOP`.
17. Handle incoming intent on cold launch.
18. Handle incoming intent while Noma is running.
19. Clear consumed intents to prevent duplicate note creation on recreation.
20. Add a dedicated app-widget provider.
21. Register widget metadata and responsive sizing.
22. Build a 48dp quick-capture action.
23. Make the entire widget tappable.
24. Give each widget instance a distinct immutable `PendingIntent`.
25. Keep private note text completely off home-screen surfaces.
26. Use light and dark widget resource palettes.
27. Define rounded widget surface and accent action states.
28. Localize widget and share labels in French/English.
29. Provide content descriptions for assisted navigation.
30. Refresh widget views after resize.
31. Rebind widget views after widget restoration.
32. Add dynamic launcher long-press shortcut.
33. Use current application package for the shortcut (debug suffix safe).
34. Add six+ JUnit parser tests and service persistence coverage.
35. Document reproducible device verification and rollback.

## Acceptance

CI must pass JVM tests, Android test source compilation, Android Lint, and debug APK.
Physical device/emulator still required: Android share-sheet capture, text-selection action,
launcher shortcut, widget cold/warm launch, widget resize/restoration, light/dark widget,
selection TalkBack, keyboard focus, double taps, rotation, and process restart.
The widget never requires INTERNET or access to other app content.

Rollback by reverting this branch independently; previous P0 and original app remain unchanged.
