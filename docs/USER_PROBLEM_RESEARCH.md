# Noma — étude de problèmes utilisateurs et benchmark UX
Date : 2026-11-09  
Statut : recherche qualitative, **aucune compilation ni validation utilisateur induite**

## Méthode (cinq étapes)

1. **Trouver une demande prouvée** : applications de notes installées et activement utilisées : Google Keep, Obsidian, Apple Notes, Bear, Craft et Simplenote. La présence de ces produits est une preuve de marché, pas une validation de Noma.
2. **Lire les avis négatifs** : distinguer avis officiels, discussions communautaires, aide technique et hypothèses.
3. **Regrouper les plaintes récurrentes** : vitesse de capture, organisation/mise en forme, stabilité, synchronisation/récupération et complexité mobile.
4. **Choisir un problème prioritaire** : capturer, retrouver et préserver une note doit être rapide et sûr, sans configuration requise.
5. **Concevoir une exécution différente** : offline-first et native Android, stockage local vérifiable, capture immédiate, organisation optionnelle, simplicité de premier niveau et portabilité.

## Éléments vérifiables

| Source datée | Signal observé | Niveau de preuve | Opportunité |
|---|---|---|---|
| Google Play Google Keep, avis publics et fiche produit (consulté 2026-11-09) | Des avis critiquent le tri de listes, la réorganisation et les performances sur notes longues. Google décrit la capture rapide, les widgets, les listes, les rappels et la synchronisation comme fonctionnalités déjà proposées. | Avis individuels + description officielle; pas mesure de fréquence | Créer des checklists fiables, tri et grand texte performants; ne pas prétendre inventer les widgets |
| Reddit r/ObsidianMD, 2025-10-15 | Discussion fortement engagée sur les frictions de capture et navigation mobile et le temps avant de commencer à écrire. | Qualitatif, biais de participation | Nouveau document en un geste, saisie immédiatement focalisée, aucune initialisation de plugin |
| Reddit r/NoteTaking, 2026-08-06 | Une personne venant d'iOS recherche une alternative Android; reproches cités : lenteur de Notion, manque de mise en forme dans Keep, comportement de zoom d'autres outils. | Témoignage isolé | Éditeur typographique natif, listes et formatage progressifs |
| Google Help Keep | Aide officielle dédiée aux notes non retrouvées, problèmes de synchronisation et conflits de versions hors connexion. | La documentation décrit des cas à gérer, **pas leur fréquence** | Export vérifié, historique/récupération et résolution de conflit explicite |
| Reddit r/ObsidianMD, 2024-05-15 et 2025-06-19 | Des participants signalent une synchronisation difficile et parfois des inquiétudes sur les notes; certains apprécient malgré tout la portabilité des fichiers. | Témoignages nuancés | Local-first, export simple et synchronisation facultative avec conflit visible |

Sources :
- https://play.google.com/store/apps/details?id=com.google.android.keep
- https://www.reddit.com/r/ObsidianMD/comments/1o74hpk/
- https://www.reddit.com/r/NoteTaking/comments/1vh0mor/
- https://support.google.com/keep/answer/6102239
- https://www.reddit.com/r/ObsidianMD/comments/1cs7pf2
- https://www.reddit.com/r/ObsidianMD/comments/1lex6vi/

Les avis et messages sélectionnés **ne représentent pas statistiquement** tous les utilisateurs. Ne pas inventer de moyenne, nombre d'avis ou note négative.

## Bibliothèque de référence Mobbin

Tentatives directes via Mobbin : « Apple Notes notes list screen », « Bear note editor screen » et « notes app creating and editing a note ». **Accès bloqué** : Mobbin MCP exige un plan payant. Aucun écran ni flow Mobbin n'a donc été inspecté ni validé. Ne pas attribuer des détails précis à un écran non visible.

Recherches à réaliser quand le plan donne accès :
1. Apple Notes — notes list and quick capture; comparer titre, sections, actions de capture, recherche, état vide.
2. Bear — text editor; comparer ligne de lecture, outils contextuels, hiérarchie typographique et absence de bruit.
3. Craft — document list and organization; comparer navigation dossier/document, hiérarchie et actions secondaires.
4. Notion — mobile page creation and search; identifier ce qui ajoute des étapes et ralentit la capture.
5. Todoist — checklist creation and item editing; comparer touches, réordonnancement, case cochée et feedback.
6. Any note app — import/export and error recovery flow; observer le consentement, les informations de conflit et la sortie sûre.

Recherche : https://mobbin.com/  
Mobbin couvre iOS et web; adapter les références à Material 3 Expressive et aux conventions Android, pas de copie fidèle.

## Proposition de produit différenciante

**Positionnement testé comme hypothèse : « La simplicité de Keep, le soin d'un éditeur dédié et la propriété de ses notes. »**

Définition du problème prioritaire : les utilisateurs perdent du temps ou confiance lorsqu'ils doivent naviguer avant d'écrire, chercher à répétition, ou s'inquiéter de la sauvegarde et des exports.

Parcours attendus :
- **Accueil** : dernières notes immédiatement visibles, épinglées séparées, recherche claire, création persistante; actions secondaires dans menus courts.
- **Capture** : depuis icône/widget/partage Android, ouvrir un éditeur focalisé sans étape de configuration ni permission inutile.
- **Édition** : texte en priorité, barre d'outils contextuelle, sauvegarde avec statut **réel**, rejet de fermeture dangereuse après échec, maintien du brouillon.
- **Recherche** : requête multi-mots, accents, extrait et surlignage; retour sans perdre le filtre, état vide expliquant pourquoi il n'y a pas de résultats.
- **Portabilité** : exporter sans compte, expliquer les limites de TXT/Markdown, JSON intégral et sa sensibilité, importer avec aperçu et protection des notes existantes.
- **Organisation** : tags et dossiers optionnels; vue d'accueil simple par défaut; pas d'obligation de ranger une note avant de l'écrire.
- **Sauvegarde** : test de restauration dans un espace isolé, historique de changements avant synchro, conflits montrés plutôt qu'écrasés.

## Tests utilisateurs à mener

Au moins cinq participants par profil (utilisateurs de Keep, d'apps d'écriture avancées, personnes passant d'iOS à Android), sans considérer cinq personnes comme une preuve statistique.

| Scénario | Mesure à collecter | Critère expérimental (à confirmer, non démontré) |
|---|---|---|
| Nouvelle idée depuis écran d'accueil | Temps jusqu'au premier caractère | médiane < 3 s sur appareil cible après ouverture |
| Import de 100 notes | Intégrité titre/corps/date/épinglage, nombre de doublons | 0 note préexistante modifiée; 100% champs JSON conservés |
| Retrouver une note ancienne | Temps, erreurs de recherche, succès | > 90% des tâches accomplies dans l'échantillon |
| Note longue et plusieurs milliers de notes | Latence de saisie, recherche, jank | pas de perte de caractères; budget performance à mesurer |
| Interruption pendant sauvegarde/synchro | Diff et récupération | 0 perte de contenu validé en base |
| Large police/TalkBack, petit écran | Succès de tâche et actions accessibles | 100% des actions critiques utilisables |

Tester avant et après sur les mêmes scénarios, noter les résultats sans arrondir artificiellement, analyser les échecs.

## État technique confirmé / inconnues

Confirmé dans le dépôt : liste/éditeur adaptatifs, recherche titre/corps, notes épinglées/archivées, état de sauvegarde basé sur SQLite, protocole d'import/export, thème Material 3. La CI du commit de départ `e391d6a` a terminé avec succès, y compris un job d'instrumentation (vérifier résultats individuels si un défaut apparaît).

Inconnu : vitesse réelle sur Pixel 9, robustesse sous charge, taux d'abandon, satisfaction et volonté de migrer. Ce document n'autorise aucune affirmation de validation marché ni de fidélité visuelle à Mobbin.

## Priorités UX proposées (sans modifier le périmètre Git existant)

1. **P0 confiance** : restauration prouvée, absence de perte de données, export complet et collisions.
2. **P1 capture** : widget, partage entrant, raccourci et fermeture sécurisée.
3. **P1 écriture** : checklists, formatting léger et accessibilité complète.
4. **P1 récupération** : recherche accents/extraits/filtres et organisation non obligatoire.
5. **P2 synchronisation** : opt-in et conflit géré sans remplacement silencieux.

Le minimum de 25 actions et le seuil de compilation s'appliquent aux cycles d'implémentation, pas à cette recherche. Toute modification fonctionnelle doit aller sur sa branche et conserver ses critères d'acceptation, tests et stratégie de rollback.
