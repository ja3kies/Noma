# Noma — UI stages 2–6 · implementation and verification matrix

**Repository:** ja3kies/Noma  
**Branch:** `feature/noma-ui-stages2-6-15-actions`  
**Parent:** Stage 1, `a632c11861c61d0f39d1a0af85bf43b9b78bdc35`.  
**Scope:** presentation and tests; no production database/auth/network code changed.

The entries below describe individual implemented changes. An action is not marked *verified* merely because it was committed. Each stage must have >=15 modifications and all CI gates must pass before it is complete.

References: [Apple Lists](https://developer.apple.com/design/human-interface-guidelines/lists-and-tables), [Apple Menus](https://developer.apple.com/design/human-interface-guidelines/menus), [Android Compose Lazy Lists](https://developer.android.com/develop/ui/compose/lists), [Android Compose Animation](https://developer.android.com/develop/ui/compose/animation/quick-guide), [Android Adaptive Layout](https://developer.android.com/develop/ui/compose/layouts/adaptive).


## Stage 2 — Notes list, card hierarchy and grouping (19 actions)

- [x] **2.01** Show a contextual icon beside each section heading
- [x] **2.02** Show pinned-note counts in pinned section
- [x] **2.03** Show regular/filtered-note counts in secondary section
- [x] **2.04** Select Archive/Search/Recent heading icons by context
- [x] **2.05** Give count badges an accessibility description
- [x] **2.06** Increase section-title weight while preserving typography hierarchy
- [x] **2.07** Use a semantic tonal container for count chips
- [x] **2.08** Animate selected card background with theme colors
- [x] **2.09** Animate card border color when selection changes
- [x] **2.10** Animate selected card corner radius with a no-bounce spring
- [x] **2.11** Animate card elevation on selection
- [x] **2.12** Make note cards at least 96 dp high
- [x] **2.13** Increase card content padding to improve legibility
- [x] **2.14** Emphasize selected note titles without bolding all notes
- [x] **2.15** Apply 21 sp preview line-height
- [x] **2.16** Use a compact pinned badge with semantic color
- [x] **2.17** Distinguish archived cards with a dedicated badge
- [x] **2.18** Keep card menu controls at least 48 dp
- [x] **2.19** Reduce duplicate list bottom whitespace while keeping the FAB visible

**Verification:** code review complete; execution status is pending GitHub Actions for this branch.

## Stage 3 — Editor, typography and error recovery (21 actions)

- [x] **3.01** Show active note title in tablet editor toolbar
- [x] **3.02** Ellipsize long selected titles in editor toolbar
- [x] **3.03** Increase editor back-button target to 48 dp
- [x] **3.04** Increase editor pin target to 48 dp
- [x] **3.05** Announce pin/unpin state to accessibility services
- [x] **3.06** Increase editor overflow menu target to 48 dp
- [x] **3.07** Cap document reading measure at 700 dp
- [x] **3.08** Remember document scroll state per note ID
- [x] **3.09** Increase document bottom padding for IME comfort
- [x] **3.10** Use stronger title font weight
- [x] **3.11** Use semantic onSurface title color
- [x] **3.12** Use semantic onSurface body text color
- [x] **3.13** Increase body line-height to 27 sp
- [x] **3.14** Increase minimum writing area height to 360 dp
- [x] **3.15** Match placeholder rhythm with body line-height
- [x] **3.16** Ensure Retry offers a 48 dp target
- [x] **3.17** Animate save-status bar background to error container
- [x] **3.18** Keep failed-save guidance above scrollable document
- [x] **3.19** Animate error guidance in and out without bouncing
- [x] **3.20** Keep pending-exit status fixed above document
- [x] **3.21** Remove duplicate warnings from the scrolling content

**Verification:** code review complete; execution status is pending GitHub Actions for this branch.

## Stage 4 — Menus, sharing dialogs and destructive actions (23 actions)

- [x] **4.01** Give the pin action a stable test target
- [x] **4.02** Offer a 48 dp pin menu row
- [x] **4.03** Use selected pin tint when a note is pinned
- [x] **4.04** Add a visual check for an already-pinned item
- [x] **4.05** Give Archive/Restore a stable test target
- [x] **4.06** Offer a 48 dp archive menu row
- [x] **4.07** Use secondary icon tint on archive action
- [x] **4.08** Give Share a stable menu target
- [x] **4.09** Offer a 48 dp Share menu row
- [x] **4.10** Use consistent icon tint for Share
- [x] **4.11** Give Delete a 48 dp action row
- [x] **4.12** Wrap incoming share text in a separate tonal preview
- [x] **4.13** Keep share text preview readable at 23 sp line-height
- [x] **4.14** Add an explicit share preview test tag
- [x] **4.15** Explain storage-not-ready inside the share dialog
- [x] **4.16** Announce share validation errors politely
- [x] **4.17** Announce unsaved-current-note guidance politely
- [x] **4.18** Show a progress indicator while saving shared text
- [x] **4.19** Localize the saving label into French and English
- [x] **4.20** Give share confirmation a 48 dp target
- [x] **4.21** Give share dismissal a 48 dp target
- [x] **4.22** Give delete confirmation a 48 dp target
- [x] **4.23** Give delete cancellation a 48 dp target

**Verification:** code review complete; execution status is pending GitHub Actions for this branch.

## Stage 5 — Appearance choice and widget presentation (25 actions)

- [x] **5.01** Apply matching extra-large shape to appearance dialog
- [x] **5.02** Allow 430 dp constrained appearance choices
- [x] **5.03** Space appearance choice surfaces for clear grouping
- [x] **5.04** Animate selected appearance container color
- [x] **5.05** Animate appearance border highlight
- [x] **5.06** Preview System mode with a light/dark split
- [x] **5.07** Preview warm paper Light mode
- [x] **5.08** Preview soft deep Dark mode
- [x] **5.09** Use 76 dp appearance option touch targets
- [x] **5.10** Outline preview swatches to separate from selected surfaces
- [x] **5.11** Use a stronger type weight for selected appearance
- [x] **5.12** Provide localized appearance descriptions for each mode
- [x] **5.13** Keep appearance dialog close at 48 dp
- [x] **5.14** Apply matching extra-large shape to widget dialog
- [x] **5.15** Allow 440 dp constrained widget choices
- [x] **5.16** Animate selected widget container background
- [x] **5.17** Animate selected widget border
- [x] **5.18** Use 76 dp widget option touch targets
- [x] **5.19** Enlarge actual widget preview to 64 by 42 dp
- [x] **5.20** Place a note glyph in every widget preview
- [x] **5.21** Choose contrasting glyph color on light glass previews
- [x] **5.22** Show localized descriptions of each widget style
- [x] **5.23** Use selected widget text weight for hierarchy
- [x] **5.24** Retain native radio semantics on each choice
- [x] **5.25** Keep widget dialog close at 48 dp

**Verification:** code review complete; execution status is pending GitHub Actions for this branch.

## Stage 6 — Tablet, wide window and finishing (17 actions)

- [x] **6.01** Extract the 840 dp two-pane breakpoint into a tested helper
- [x] **6.02** Use different pane gutters on medium and large widths
- [x] **6.03** Use responsive outer margins for split-pane layout
- [x] **6.04** Adapt pane corner radii at the 1200 dp threshold
- [x] **6.05** Animate list pane width during window resizing
- [x] **6.06** Wrap tablet list in a separated rounded surface
- [x] **6.07** Use semantic surfaceContainerLow for the tablet list
- [x] **6.08** Give tablet editor a matching independent rounded surface
- [x] **6.09** Ensure editor Scaffold respects the tablet pane surface
- [x] **6.10** Keep narrow/phone layout unchanged below the breakpoint
- [x] **6.11** Keep blank tablet editor content centered
- [x] **6.12** Provide a tonal icon badge for the empty editor pane
- [x] **6.13** Limit empty-state text width to 460 dp
- [x] **6.14** Center the empty-state title for scanning
- [x] **6.15** Center supporting text and use secondary typography
- [x] **6.16** Give the empty tablet pane a stable test identifier
- [x] **6.17** Test breakpoint plus spacing, margin and radius values

**Verification:** code review complete; execution status is pending GitHub Actions for this branch.

## Regression and acceptance gates

- [x] 5 stage implementation commits and targeted tests prepared in isolated Git commit chain
- [x] Existing card, editor, menu and tablet functionality retained
- [x] New Android UI tests for pinned/archived badges, section counts, retry, menu commands, sharing and theme previews
- [x] New JVM breakpoint, spacing and rounded pane tests
- [ ] `testDebugUnitTest` PASS
- [ ] `compileDebugAndroidTestKotlin` PASS
- [ ] `lintDebug` PASS
- [ ] `assembleDebug` PASS
- [ ] `connectedDebugAndroidTest` PASS on API35 emulator
- [ ] On-device review on Pixel 9 in light/dark themes and split-pane tablet mode
- [ ] User permission for merge (no automatic merge)

**Rollback:** retain parent stage 1 and PR #5 untouched; abandon the new branch if any acceptance gate fails.
