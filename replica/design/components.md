# Noma Reborn — original Android design primitives

This document specifies an independently developed system. Material 3 semantic colors, system typography by default, 4dp spacing scale, 48dp minimum touch targets, stateful accessibility descriptions, and reduced-motion behavior.

- Library list: adaptive single/two pane; pinned/filtered sections; low-overhead lazy card rendering; bottom search pill stays anchored above insets.
- Editor: accessible header and text field; contextual formatting menu; find/replace modal with explicit confirmation and hit count.
- Folder row (planned): icon badge, localized name, note count, depth guide, overflow actions.
- Checklist row (planned): 48dp checkbox target, nested indent, TalkBack checked state and reorder affordance.
- Locked note (planned): privacy-sanitized preview; authentication surface separated from editor.
- Attachments (planned): local content provider URIs, media lifecycle and copy/retry recovery.
- Recovery (planned): undo/restore, readable warnings, deterministic 30-day retention.

Each component must cover empty/loading/disabled/error/focus states. Do not copy target iconography or branded gradients.