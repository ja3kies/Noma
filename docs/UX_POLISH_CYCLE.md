# Noma — cycle UX : accueil, édition, navigation et accessibilité

Date : 2026-11-09  
Branche : `feature/noma-ux-polish-next` (issue de `e1dbdb3`, icône proportionnée réintégrée).  
Aucun changement de `main`, du schéma SQLite, du format d'archive ou de la signature de l'application.

## Problèmes confirmés et corrections

| Zone | Défaut observé dans le code | Correction apportée |
| --- | --- | --- |
| Carnet vide | Un bouton central et une FAB « Nouvelle note » en doublon | Une seule action principale dans l'état vide ; FAB maintenue quand des notes existent |
| Liste triée | En-tête « Recent / Récentes » même en tri A–Z | En-tête alphabétique distinct |
| Recherche | En-tête « Recent » malgré une requête active | Section « Search results / Résultats de recherche » |
| Navigation après recherche | Le focus/IME pouvait rester actif lors de l'ouverture d'une note | Effacement du focus et fermeture du clavier au clic sur une carte |
| Éditeur | Retour système capturé même quand le clavier était ouvert | Premier retour laissé au clavier ; deuxième retour pour quitter en conservant la logique de sauvegarde |
| Apparence | Options cliquables sans véritable rôle radio groupé | `selectableGroup` + `selectable(role = RadioButton)` |
| Accessibilité large texte | Préférences de thème/widget sans défilement interne explicite | Contenu déroulant avec limite de hauteur |
| Suppression | Dialogue générique ne précisant pas la note concernée | Nom de note inclus dans la confirmation, limité à 80 caractères |
| Menu éditeur | Épinglage présenté à la fois dans la barre et le menu | Action unique dans la barre ; suppression du doublon |
| État « sauvegarde en cours » | Menu permettait d'essayer suppression et archivage qui étaient ensuite refusés | Actions destructives désactivées jusqu'à l'acquittement du disque |
| Capture via widget | Après abandon explicite d'un brouillon échoué, une ancienne capture différée restait en attente | Annulation de la demande différée lors de `closeNote` |
| Partage sortant | Exception si aucun destinataire Android n'acceptait le partage | Message localisé et comportement récupérable |
| Création lente | Double-clic ignoré sans aucun retour visuel | État « Création de la note… » et indicateur de progression ; bouton vide désactivé |

## Vérifications et tâches réalisées (32 points précis)

1. Conserver l'icône adaptative XML corrigée dans l'historique de la branche.
2. Conserver le SVG couleur et monochrome dans `design/branding`.
3. Éviter la duplication de FAB dans un carnet vide.
4. Préserver la FAB quand le carnet est déjà rempli.
5. Préserver le CTA central accessible d'un carnet vide.
6. Distinguer en-tête de résultats de recherche.
7. Distinguer tri alphabétique et tri récent.
8. Traduire les en-têtes de sections en anglais.
9. Traduire les en-têtes de sections en français.
10. Laisser Android masquer l'IME avant d'exécuter la navigation arrière.
11. Conserver l'attente de sauvegarde lors du véritable retour de l'éditeur.
12. Masquer le clavier de recherche avant l'ouverture d'une carte.
13. Effacer le focus de recherche avant l'ouverture d'une carte.
14. Utiliser un groupe d'options radio accessible pour le thème.
15. Utiliser un groupe d'options radio accessible pour le style du widget.
16. Rendre les réglages d'apparence défilants quand la police est agrandie.
17. Rendre les réglages du widget défilants quand la police est agrandie.
18. Ajouter un test instrumenté de l'état vide sans double CTA.
19. Ajouter un test instrumenté de la FAB du carnet rempli.
20. Ajouter un test instrumenté du titre de section en tri A–Z et recherche.
21. Ajouter le titre de la note à la confirmation de suppression.
22. Localiser la confirmation de suppression en anglais.
23. Localiser la confirmation de suppression en français.
24. Supprimer le contrôle d'épinglage redondant du menu éditeur.
25. Empêcher les actions destructives tant que l'éditeur n'est pas sauvegardé.
26. Tester l'annulation de suppression nommée sans perte de note.
27. Tester le menu éditeur sans épinglage répété.
28. Annuler une ancienne capture widget différée si un brouillon est abandonné.
29. Tester le non-déclenchement d'une capture fantôme après échec d'écriture.
30. Gérer le partage Android indisponible sans faire tomber Noma.
31. Localiser ce message de partage en deux langues.
32. Montrer une progression lors d'une création lente et vérifier que les appuis répétés sont bloqués avec un test instrumenté.

**Ces tests existent dans Git : ils ne sont pas considérés comme réussis sans vérification CI du SHA final.** Les captures sur appareil ne sont pas remplacées par les maquettes ni par la compilation.

## Contrôles avant de passer à d'autres fonctionnalités

- [ ] Dernier SHA : JVM, compilation debug, lint, compilation androidTest.
- [ ] Dernier SHA : tests instrumentés sur émulateur, sans faux positif.
- [ ] Pixel 9 : icône adaptative, monochrome et widget sur écran d'accueil.
- [ ] Pixel 9 : clavier ouvert dans l'éditeur, retour système une puis deux fois.
- [ ] Pixel 9 : TalkBack et grand texte à 200 % dans les deux sélecteurs.
- [ ] Pixel 9 : supprimer / annuler sur notes nommées.
- [ ] Pixel 9 : appui rapide répété « Nouvelle note » sur stockage lent.
- [ ] Pixel 9 : petit écran et paysage, sans débordement.
- [ ] Mesures de latence pour 1 000, 5 000 et 10 000 notes, avec et sans recherche.
- [ ] Retester annulation, sauvegarde échouée et reprise sous défaillance SAF/SQLite.

## Risques restant ouverts

- UX réelle sur appareils physiques, notamment animations clavier et grand texte.
- Performances de recherche/tri sur de très grandes bibliothèques (aucune mesure disponible).
- Navigation TalkBack exhaustive et lisibilité sur les fonds d'écran réels.
- Variantes d'apparence des widgets selon les lanceurs OEM.
- Un test CI vert ne démontre pas l'absence de tout défaut ergonomique.

Rollback : construire une branche de secours depuis `e1dbdb3` et conserver intégralement les données et la version stable. **Ne pas désinstaller une installation contenant des notes, ne pas pousser vers `main` et ne pas fusionner sans accord.**
