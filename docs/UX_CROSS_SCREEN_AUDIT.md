# Noma — audit UX transversal, cycle récupération et navigation

**Date :** 2026-11-09  
**Branche :** `feature/noma-ux-recovery-navigation`  
**Base :** `3f563b3` (chantier de vérification des sauvegardes).  
**Statut :** corrections implémentées ; la clôture dépend de la CI du dernier commit et des observations sur un véritable appareil. Aucun changement de `main`.

## Priorités fondées sur une inspection du code

### 1. Accueil, recherche et navigation — P0/P1

| Problème constaté | Correction | Vérification |
|---|---|---|
| La création restait proposée alors que SQLite ne pouvait pas charger les notes | Masquer la FAB quand le dépôt est en chargement ou erreur | Test Compose : aucun bouton dans ces états |
| Recherche/tri/filtres modifiables alors qu'aucun résultat n'était disponible | Désactivation des contrôles pendant les erreurs et le chargement | Test Compose sur recherche désactivée |
| Extrait de recherche conservé en mémoire quand le titre changeait | Clé `remember` basée sur titre, corps et requête | Contrôle statique et test de recherche existant |
| « Épingler » proposé sur les notes archivées, sans conséquence visible dans les épinglées | Action supprimée des menus d'archive | Test Compose |
| Le menu `…` annonçait seulement « Actions » pour toutes les notes | Libellé TalkBack comportant le titre de la note | Test Compose |
| Le pictogramme d'épinglage décoratif annonçait « Épingler » à tort | Retirer son libellé vocal, tout en conservant l'action réelle dans le menu | Contrôle de sémantique |
| La carte sélectionnée sur écran large n'était pas explicitement marquée sélectionnée | `semantics.selected` | Contrôle de sémantique |

### 2. Éditeur et fiabilité — P0

| Problème | Correction | Vérification |
|---|---|---|
| Les transitions d'état `Sauvegarde… / Enregistré / Échec` n'étaient pas annoncées | Région d'annonce TalkBack polie | Contrôle de sémantique ; test manuel de débit vocal requis |
| Les tests du launcher exigeaient un seul identifiant d'icône | Accepter la ressource adaptative normale ou ronde sélectionnée par Android | CI instrumentation ; correction de la panne observée |
| La sélection d'un autre document avant confirmation d'écriture était déjà bloquée | Protection conservée | Tests JVM précédents conservés |

### 3. Importation, exportation et récupération — P0

| Problème | Correction | Vérification |
|---|---|---|
| La vérification d'une sauvegarde utilisait le message « Import échoué » | Message spécifique FR/EN | Vérification des ressources |
| Un résultat de vérification pouvait rester visible lors d'une nouvelle tentative | Effacer le résumé avant la lecture | Contrôle de l'état |
| La confirmation d'import pouvait rester activée si la base échouait après lecture du fichier | Désactiver la confirmation hors état `Ready` | Test d'état |
| Une confirmation d'import fermait l'aperçu avant le résultat de SQLite | `importNotesAwait`, acheminé dans la file d'opérations | Tests JVM de confirmation |
| L'échec SQLite obligeait à choisir de nouveau le fichier | Conserver le dialogue et les notes validées pour réessayer | Tests JVM de défaillance/réessai ; test écran à réaliser |
| Un appui répété pouvait démarrer un import avant la fin du premier | Etat `busy` et validation indisponible durant l'import | Contrôle de la logique Compose |
| L'import ne présentait que le total et les collisions | Afficher nouvelles notes, collisions, épinglées et archivées | Contrôle de ressources ; tests visuels requis |
| Le message de progression n'existait pas | « Importation en cours… » / « Importing… » | Contrôle des chaînes |
| Le formulaire ne pouvait pas différencier erreur d'import et erreur système | Exceptions d'import propagées au dialogue, sans snackbar en doublon | Tests JVM |
| Les snapshots d'export sont ordonnés après les écritures | Comportement antérieur conservé | Tests JVM précédents conservés |

### 4. Partage Android — P1

| Problème | Correction | Vérification |
|---|---|---|
| Un long partage n'affichait que 500 caractères, sans préciser la coupure | Indiquer explicitement la longueur de l'aperçu et que le texte entier sera conservé | Test Compose avec 750 caractères |
| Les actions destinées aux notes n'avaient pas d'identité vocale distincte | Désigner le titre dans l'accessibilité | Test Compose |

## 30 contrôles/actionnements traçables

1. Vérification de la compilation du cycle précédent.
2. Identification de l'échec réel du test Android d'icône ronde.
3. Correction de l'assertion d'icône adaptative.
4. Masquage de la création sous `Loading`.
5. Masquage de la création sous `Error`.
6. Désactivation de la recherche sous `Loading`.
7. Désactivation de la recherche sous `Error`.
8. Désactivation du filtre archives sous `Loading/Error`.
9. Désactivation du filtre épinglées sous `Loading/Error`.
10. Désactivation du tri sous `Loading/Error`.
11. Test Compose `Loading`.
12. Test Compose `Error`.
13. Correction de la clé de prévisualisation titre/corps.
14. Suppression de l'épinglage non pertinent depuis les archives.
15. Test Compose du menu archive.
16. Libellé TalkBack par note.
17. Test Compose du libellé vocal.
18. Marquage de la note sélectionnée.
19. Suppression de l'étiquette vocale d'ornement.
20. Annonce polie du statut de sauvegarde.
21. Distinction des messages d'erreur de vérification et d'import.
22. Réinitialisation du résultat avant une nouvelle vérification.
23. Blocage de confirmation si le dépôt n'est pas prêt.
24. Détail des notes nouvelles et existantes avant import.
25. Détail des notes épinglées et archivées avant import.
26. Attente de l'accusé de réception SQLite.
27. Préservation du dialogue en cas d'échec.
28. Désactivation du double appui lors de l'import.
29. Progression localisée en français et en anglais.
30. Tests JVM de confirmation différée et de réessai.
31. Texte annonçant la troncature de l'aperçu de partage.
32. Test Compose confirmant l'enregistrement des 750 caractères.
33. Alignement des tests Compose sur les vraies chaînes « Try again » et « Restore note ».
34. Préservation de la compatibilité SQLite et des formats d'archive existants.

Ces actions **ne constituent pas une preuve de réussite des tests**. Elles doivent être revalidées à la même révision.

## Critères de sortie avant de passer aux fonctionnalités supplémentaires

- [ ] CI du **dernier commit** : `testDebugUnitTest`, `lintDebug`, `compileDebugAndroidTestKotlin` et `assembleDebug` réussis.
- [ ] CI du **dernier commit** : `connectedDebugAndroidTest` exécuté et réussi sur émulateur.
- [ ] Sur Pixel 9 : parcours accueil/éditeur/recherche/archives, clavier ouvert et fermé.
- [ ] Sur Pixel 9 : TalkBack sur recherche, liste, actions, état de sauvegarde et partage.
- [ ] Sur Pixel 9 : taille des caractères 200 %, petit format et paysage, sans débordement ni cible inaccessible.
- [ ] Sur Pixel 9 : sauvegarde JSON, tentative d'import dans un espace de test, interruption, refus de permission, puis reprise.
- [ ] Mesurer recherche et navigation avec 1 000–10 000 notes et des textes longs ; aucun chiffre de performance n'est affirmé sans mesure.
- [ ] Vérifier les trois styles du widget et l'icône sur plusieurs fonds d'écran et modes clair/sombre.

**À ne pas annoncer comme terminé :** tous les détails UX sur appareils physiques, la performance sur grandes bibliothèques, l'accessibilité exhaustive et les scénarios de panne fournisseur non reproduits. L'objectif est d'éliminer les défauts confirmés puis d'obtenir une validation manuelle des parcours critiques, pas de déclarer arbitrairement « zéro défaut ».

## Sécurité et rollback

Aucune mutation de schéma SQLite, aucun effacement de notes, aucune fusion dans `main`, aucune suppression de branche ni réinstallation. Le rollback consiste à reconstruire une branche à partir du parent `3f563b3` tout en préservant l'installation et la base de données. Ne jamais remplacer une installation contenant des notes avec une signature de debug incompatible.
