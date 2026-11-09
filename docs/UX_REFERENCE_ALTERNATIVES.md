# Noma — références UI/UX et premier cycle de corrections
Date: 2026-11-09

## Alternatives à Mobbin (vérifiées publiquement)

| Produit | Couverture utile pour Noma | Accès / limites |
|---|---|---|
| [Banani, bibliothèque mobile](https://www.banani.co/references/apps) | Captures d'apps grand public, parcours Things 3 et inspiration de listes; gratuit sans compte pour la collection publique | Couverture limitée; ne pas supposer que l'app recherchée existe |
| [Things 3 — Banani](https://www.banani.co/references/apps/things-3) | Organisation hiérarchique, création d'éléments et navigation avec peu de friction | Référence iOS à transposer en Compose natif |
| [Page Flows](https://pageflows.com/) | Enregistrements de parcours réels sur iOS et Android, utile pour gestes et séquences de capture | Essai limité, puis abonnement |
| [Refero](https://refero.design/) | Écrans et parcours d'apps iOS et web, navigation et éditeur | Accès gratuit limité, contenu complet payant |
| [ScreensDesign](https://screensdesign.com/) | Captures et parcours de produits iOS | Couverture Android absente ou limitée |
| [Dribbble](https://dribbble.com/) | Direction graphique et concepts | Concepts non nécessairement utilisés en production; ne pas traiter comme preuve UX |

**Source de comparaison** (mise à jour octobre 2026) : https://mobbin.com/blog/best-mobbin-alternatives

Mobbin MCP avait renvoyé « requires a paid plan »; aucun écran Mobbin n'a été inspecté. Les pages de Banani ont été examinées via les contenus publics. Aucune étude ne présume de la qualité d'un produit à partir de son seul style visuel.

## Problèmes du produit existant constatables dans le code

- La recherche préexistante exigeait une seule sous-chaîne exacte, sans correspondance lorsque l'utilisateur omet un accent.
- La recherche « rendez vous » ne pouvait pas rapprocher « rendez » dans le titre et « vous » dans le corps.
- Les résultats affichaient toujours la première ligne du corps, même lorsqu'une autre ligne correspondait à la requête.
- L'indicateur du nombre de notes représentait les notes totales du dossier, et non le nombre de résultats.
- Une recherche sans résultat ne proposait pas de remise à zéro claire des filtres; l'utilisateur devait retrouver plusieurs commandes.

## Corrections de ce premier cycle, sans migration de données

1. Normalisation d'une copie du contenu en mémoire pour comparaison, sans réécrire le texte stocké.
2. Recherche insensible aux accents et à la casse.
3. Découpage en mots, blancs multiples acceptés.
4. Termes correspondants dans des champs différents.
5. Obligation de faire correspondre tous les termes.
6. Préservation du classement existant par date et épinglage.
7. Respect du filtre archive.
8. Respect du filtre notes épinglées.
9. Prévisualisation de la ligne pertinente.
10. Fallback sûr lorsqu'un terme se trouve seulement dans le titre.
11. Réinitialisation de recherche et filtres sans modification de note.
12. Conservation de la préférence de tri après réinitialisation.
13. Accès direct à la récupération depuis l'état « aucun résultat ».
14. Compteur des résultats correspondant aux filtres.
15. Gestion singulier/pluriel pour la langue anglaise.
16. Gestion singulier/pluriel pour la langue française.
17. Tests JVM sur les caractères accentués.
18. Tests JVM sur l'ordre des mots.
19. Tests JVM sur les correspondances entre titre et corps.
20. Tests JVM sur le cas de terme absent.
21. Tests JVM sur les prévisualisations.
22. Tests JVM sur les filtres archive et épinglage.
23. Test UI instrumenté sur la remise à zéro.
24. Développement isolé en branche; `main` non modifiée.
25. Vérification CI du commit final avant toute qualification comme build validé.

Attention: les éléments ci-dessus incluent des scénarios de test **écrits, non nécessairement exécutés**. La liste de 25 éléments ne prouve pas à elle seule que la compilation est validée. Seule l'exécution CI du dernier commit et la validation de l'UX sur appareil permettent de clôturer le cycle.

## Contrôles restant nécessaires
- Performance avec 1 000–10 000 notes; coût de la normalisation à chaque recherche.
- Android TalkBack et taille des polices; état vide sous clavier.
- Recherche avec alphabets non latins, ligatures, emojis et caractères combinants.
- Validation de la recherche depuis les widgets et intents de capture du cycle suivant.
- Vérification que l'éditeur n'efface jamais un brouillon après une recherche ou un changement de filtre.

**Rollback:** revenir à `e391d6a` sur cette branche sans modifier la base SQLite; aucune migration de schéma dans ce cycle.
