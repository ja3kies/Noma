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

## État courant après 91d11fc

La navigation tactile, la fidélité visuelle Smart Launcher, TalkBack, la capture réelle de widgets, les temps de frame, les tests instrumentés et la compilation de cette nouvelle révision restent à contrôler sur appareil. Tant que ces preuves manquent, les gates concernés sont **FAILED**, et non « partiels ».
