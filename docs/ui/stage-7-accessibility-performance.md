# Noma — Étape 7 : accessibilité et fluidité (21 actions)

Branche dédiée : `feature/noma-ui-stage7-accessibility-performance` à partir de `c19103d5b916a0bdec525078163d00f403006b3e`. La PR UI #6 et le code de données restent inchangés.

## Anomalie initiale

CI [#338](https://github.com/ja3kies/Noma/actions/runs/42809248493) : compilation, lint, JVM et APK PASS, **1 échec sur 113 tests instrumentés** (`emptyNotebookOffersSingleActionEvenWithActiveSearchFilter`). La CI [#337](https://github.com/ja3kies/Noma/actions/runs/42808419727) était entièrement verte pour le même commit. Hypothèse traitée : un callback tardif de composition IME peut restaurer le champ après un effacement qui libère le focus en premier. La modification inverse cet ordre ; la couverture répétée tente de reproduire le problème. Ne pas revendiquer la cause racine prouvée avant tests instrumentés.

## Objectifs implémentés

| Identifiant | Amélioration | Emplacement |
|---|---|---|
| S7-01 | Prevent stale IME composition restoring a cleared search by resetting filters before dropping focus | `NotesListScreen bottom action` |
| S7-02 | Let the bottom pill grow for larger fonts rather than clipping it at 58 dp | `BottomSearchPill TextField` |
| S7-03 | Expose the Noma home title as a TalkBack heading | `NotesListScreen header` |
| S7-04 | Ellipsize the home title on small windows and with accessibility font scaling | `NotesListScreen header` |
| S7-05 | Expose storage error title as an accessible heading | `Error state` |
| S7-06 | Center the storage error title for narrow-width and large-font layouts | `Error state` |
| S7-07 | Give storage retry a 48 dp minimum touch target | `Error retry button` |
| S7-08 | Give retry a stable instrumentation identifier | `noma_storage_retry` |
| S7-09 | Expose empty-library and empty-search titles as accessibility headings | `Empty state` |
| S7-10 | Center the empty-state heading rather than leaving multiline text left-aligned | `Empty state` |
| S7-11 | Center the empty-state explanation for readable multiline layout | `Empty state` |
| S7-12 | Declare section heading contentType in pinned LazyColumn slots | `LazyColumn` |
| S7-13 | Declare section heading contentType in regular LazyColumn slots | `LazyColumn` |
| S7-14 | Declare note_card contentType for pinned cards while preserving stable note ID keys | `LazyColumn` |
| S7-15 | Declare note_card contentType for regular cards while preserving stable note ID keys | `LazyColumn` |
| S7-16 | Expose pinned and recent section rows as navigable accessibility headings | `SectionHeader` |
| S7-17 | Label title editing separately from body text for screen readers | `NoteEditorScreen title` |
| S7-18 | Label note body independently with a new French/English resource | `NoteEditorScreen body` |
| S7-19 | Memoize DateFormat rendering by updatedAt rather than recomputing on UI animations | `NoteCard` |
| S7-20 | Announce sticky failed-save guidance politely, without stealing focus | `Editor save bar` |
| S7-21 | Announce pending-exit state politely while data is being saved | `Editor save bar` |

Les actions précédentes 1 à 6 ne sont pas comptées une seconde fois.

## Tests ajoutés

Cinq tests instrumentés pour vérifier : annonces des titres de section, description distincte des champs d'édition, titre accessible dans la bibliothèque vide, récupération du stockage et **trois cycles successifs** de recherche / réinitialisation avec clavier.

## Validation

- [x] Modifications distinctes dans le code Compose et les ressources FR/EN
- [x] Inspection des ancres de remplacement, du diff et des noms de ressources
- [ ] JVM et lint de la branche
- [ ] APK compilé à la CI
- [ ] Instrumentation sur API 35, y compris le scénario intermittent
- [ ] Essai réel sur Pixel, avec police agrandie, TalkBack et clavier
- [ ] Accord utilisateur avant toute fusion

## Principes et références visuelles

- [Apple — Search fields (barre de recherche inférieure)](https://developer.apple.com/design/human-interface-guidelines/search-fields)
- [Android — Compose performance](https://developer.android.com/develop/ui/compose/performance/bestpractices)
- [Android — Compose Accessibility](https://developer.android.com/develop/ui/compose/accessibility/semantics)
- [Android — Font scaling](https://developer.android.com/develop/ui/compose/text/user-input)

Rollback : laisser la PR #6 en brouillon et revenir à son head `c19103d5`. Aucune modification persistante irréversible.
