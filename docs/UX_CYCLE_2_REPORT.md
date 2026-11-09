# Noma — Cycle UX : capture, recherche et accessibilité

## Base et sécurité
- Branche : `feature/noma-ux-search-clarity`; base précédente CI verte `4fcdbf5`.
- Aucun changement de schéma SQLite, de signature, d'identifiant de package, de suppression ou de synchronisation.
- `main` n'est pas modifiée. Retour arrière : conserver l'APK et les notes existantes; remettre la branche au commit `4fcdbf5` uniquement dans une nouvelle branche de correctif si une régression apparaît. Ne jamais désinstaller une installation contenant des notes.
- Statut de livraison : **ne pas qualifier « validé » avant tests JVM, compilation, lint ET instrumentation sur la tête de branche**.

## 30 actions d'implémentation et de vérification attribuables au cycle

| # | Action / preuve de code |
|---|---|
| 01 | Conserver la compatibilité avec les anciennes notes et le schéma SQLite v2 |
| 02 | Effacer une requête active après création réellement réussie |
| 03 | Sortir de la vue archives après création réussie |
| 04 | Retirer le filtre « épinglées seulement » après création réussie |
| 05 | Préserver la requête lorsqu'un enregistrement échoue |
| 06 | Préserver les filtres lorsqu'un enregistrement échoue |
| 07 | Remettre `creating` à faux après échec |
| 08 | Test JVM : création depuis une recherche |
| 09 | Test JVM : création depuis les archives |
| 10 | Test JVM : création échouée et récupération |
| 11 | Test Compose : nouvelle note immédiatement retrouvable après retour |
| 12 | Éviter la concaténation titre+corps pour le filtrage |
| 13 | Éviter la normalisation du corps quand le titre satisfait tous les mots |
| 14 | Limiter les extraits des notes longues à 160 caractères plus ellipses |
| 15 | Centrer l'extrait sur la première correspondance |
| 16 | Tenir compte de la correspondance des points de code Unicode dans les extraits |
| 17 | Test JVM : correspondance à la fin d'une ligne très longue |
| 18 | Test JVM : caractères combinants et emojis |
| 19 | Test JVM : extrait court inchangé |
| 20 | Test JVM : texte vide avec correspondance dans le titre |
| 21 | Prioriser les mots non déjà présents dans le titre |
| 22 | Choisir une ligne qui satisfait le plus de mots pertinents |
| 23 | Test JVM : mots titre/corps |
| 24 | Test JVM : score des lignes et fallback |
| 25 | Reconnaître `œ/oe`, `æ/ae`, `ß/ss`, `ø/o`, `ł/l` |
| 26 | Test JVM : orthographe sur clavier sans ligatures |
| 27 | Ajouter une description TalkBack à la recherche |
| 28 | Annoncer poliment les variations du nombre de résultats |
| 29 | Test Compose : description accessible du champ de recherche |
| 30 | Tri alphabétique harmonisé avec la normalisation, plus test des accents |

Chaque action est inspectable dans les commits de la branche et/ou les tests. Ce décompte n'est pas une preuve que l'ensemble des tests est réussi.

## Protocole de validation
1. Vérifier la CI **du dernier commit**, `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `lintDebug`, `assembleDebug`.
2. Vérifier le job `connectedDebugAndroidTest` de la même exécution GitHub.
3. Sous Android, rechercher un terme accentué sans accent, lancer la capture, revenir à la liste, tester « rien trouvé », rechercher dans les archives.
4. Activer TalkBack et des tailles de caractères étendues : focus recherche, annonces, commandes de tri, éditeur et clavier.
5. Sur un appareil avec de vraies notes, exporter une archive JSON avant toute installation; ne pas tester les migrations avec les données personnelles.
6. Évaluer les performances avec 1 000, 5 000 et 10 000 notes, ainsi que les grands corps de texte (tests à réaliser). Ne pas supposer la fluidité en l'absence de mesure.

## Limites connues
- La recherche est actuellement recalculée lors de la saisie; aucune mesure de latence sur appareil à grand volume n'a été effectuée.
- Le surlignage typographique des mots n'est pas encore implémenté.
- Un export JSON n'est pas chiffré; l'avertissement de confidentialité de la branche P0 demeure.
- Une compilation en CI avec instrumentation n'est pas équivalente à une validation complète de l'expérience utilisateur sur Pixel 9.
