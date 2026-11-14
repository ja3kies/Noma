# Noma — Gates binaires de qualité

Ces règles s'appliquent à toute priorité P0, P1 ou P2, en particulier à la branche `feature/noma-replica-notes-rebirth`.

## Uniquement deux statuts

- **VALIDÉ** : le comportement a été implémenté, le build et les tests concernés sont passés, et la vérification propre à la priorité est documentée.
- **FAILED** : au moins une correction, preuve, compilation, instrumentation, vérification visuelle ou mesure requise manque. Mentionner le blocage exact.

Le statut « partiel », « presque fini », « supposé correct » et « validé visuellement » sans contrôle réel sont interdits.

Dès qu'une priorité est FAILED, la tâche suivante consiste à éliminer ses défauts bloquants, à réexécuter les tests nécessaires, puis à recalculer son statut. Aucun statut ne devient VALIDÉ uniquement parce que le code compile. Il est impossible de déclarer tous les gates VALIDÉ sans appareil quand le rendu, l'IME ou les widgets doivent être contrôlés sur le lanceur réel.

## Ordre d'exécution

1. **P0** : stockage, sauvegarde et migration, rôles des quatre thèmes, contrastes, navigation et clavier, import/export. Unit tests, Android test compilation et build. Tests instrumentés dès qu'un appareil est disponible. Si le rendu est déterminant, captures courantes et contrôle.
2. **P1** : bibliothèque, éditeur, apparence, UI du widget et comportement du widget sur écran d'accueil. Contrôler les états compacts/étendus, clair/sombre, Gboard, grands caractères, les lancements sur Smart Launcher et Pixel Launcher et les fonds d'écran contrastés. Un widget RemoteViews ne peut pas être déclaré équivalent au moteur de réfraction du lanceur.
3. **P2** : transitions de navigation et de clavier, ressorts interruptibles, jank/latence, accessibilité avec TalkBack. Les dossiers, sous-dossiers, checklists structurées et corbeille restent un chantier de stockage distinct ; les déclarer VALIDÉ exige des migrations et des tests de bout en bout.

**Dépendance stricte :** ne commencer le gate suivant qu'après correction de toutes les régressions constatées au gate précédent. Si un P0 apparaît pendant P1 ou P2, revenir à P0.

## Commandes reproductibles

```bash
cd ~/projects/Noma
git switch feature/noma-replica-notes-rebirth
git pull --ff-only origin feature/noma-replica-notes-rebirth
bash scripts/build-termux-noma.sh --full
adb devices
```

L'échec ou l'absence de `adb devices` ne doit jamais être rapporté comme « tests Android PASS » : la **compilation** des tests et leur **exécution instrumentée** sont distinctes. Sans accès à un appareil, conserver FAILED pour les gates qui réclament des vérifications visuelles, interactions réelles, mesure de frame time ou intégration launcher. Ne jamais inventer les captures ou les performances.

## Rapport attendu par priorité

| Gate | Statut | Preuves | Défaut bloquant / correction restante |
| --- | --- | --- | --- |
| P0 | VALIDÉ / FAILED | tests, build, thème, données | cause mesurable ou aucune |
| P1 | VALIDÉ / FAILED | tests UI, captures, widget installé | cause mesurable ou aucune |
| P2 | VALIDÉ / FAILED | navigation, clavier, jank, accessibilité | cause mesurable ou aucune |

Pour une correction, indiquer le fichier, le comportement avant/après, le test et le commit. Le commit et le push ne sont **pas** des preuves de validation. Ne jamais merger `main` automatiquement.

## Journal de clôture — novembre 2026

Dernière base revue avant ce journal : `ee206c1`. Le prochain commit de qualité ajoute l'instrumentation de preuve sans convertir artificiellement un statut FAILED en VALIDÉ.

### Corrections intégrées sur la branche

- P0 : épinglage de cartes obsolètes, archivage/désarchivage concurrent, annulation de suppression sans écraser un import plus récent, notifications UI bornées sans bloquer la file d'écritures, tests de régression des commandes concurrentes.
- P0 : recherche/remplacement Unicode, plages non chevauchantes et recherche de l'occurrence sélectionnée ; 1 000 cas aléatoires ajoutés au projet. Une vérification Kotlin autonome de l'algorithme a réussi 6 118 788 assertions sur la copie de travail, **sans prétendre exécuter Gradle**.
- P1 : couleurs système Material You sur API 31+ pour les widgets, étiquettes contraintes aux dimensions des RemoteViews, contrastes clair/sombre, rafraîchissement après mise à jour APK et sauvegarde robuste des préférences.
- P2 : sortie anticipée de la recherche de correspondances, ressorts interruptibles et indication de sauvegarde différée afin d'éviter les déplacements de l'éditeur lors des frappes rapides.

### Blocages mesurés et critères restants

| Gate | Statut | Validation restant à obtenir |
| --- | --- | --- |
| P0 | **FAILED** | Compilation JVM/Android à jour, tests instrumentés réellement exécutés, migration SQLite, erreurs et reprise sur appareil |
| P1 | **FAILED** | Tests de widgets installés, capture visuelle Material You/Frosted/Liquid Glass sur lanceurs réels, grands caractères et états contrastés |
| P2 | **FAILED** | Mesures de frames et latence sur appareil, transitions avec Gboard et Back, parcours TalkBack et réduction des animations |

Plusieurs exécutions GitHub Actions, dont `43597293750`, se sont terminées en environ 3–4 secondes, sans étapes de job exposées par l'API. Cela **ne démontre pas** une défaillance de compilation. Un blocage de runner/compte ou de facturation est plausible, mais sa cause ne doit être considérée comme prouvée que si une annotation GitHub ou un diagnostic de support la confirme.

### Collecte reproductible de preuves sur Pixel

```bash
cd ~/projects/Noma
git switch feature/noma-replica-notes-rebirth
git pull --ff-only origin feature/noma-replica-notes-rebirth
bash scripts/build-termux-noma.sh --device
```

Le mode `--device` refuse de déclarer l'instrumentation exécutée sans fichier de résultats XML, conserve le SHA de l'APK et crée un rapport daté `~/noma-device-validation-*/report.md`. Les statistiques `gfxinfo` n'équivalent pas à une mesure du 95e percentile de frame pacing. Ne pas considérer le passage des tests comme une preuve de rendu Smart Launcher, d'accessibilité TalkBack ou de fluidité.
