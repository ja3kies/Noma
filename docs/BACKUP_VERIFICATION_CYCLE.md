# Noma — restauration sûre, inspection des sauvegardes et lecture SAF

## Périmètre
Branche `feature/noma-backup-verification-ui` issue du correctif `25edcddd` (compilation d’icône en attente au moment du branchement). Aucun changement de `main`, du schéma SQLite, des notes existantes, du format JSON v1 ni de la signature applicative.

## Objectifs corrigés et livrables (27 actions vérifiables)

1. Corriger l’appel non pris en charge à `ApplicationInfo.roundIcon` dans le test d’icône.
2. Préserver le test effectif de la ressource d’icône ronde.
3. Ajouter un résumé typé des archives JSON.
4. Conserver un résumé valable pour les archives vides.
5. Compter les notes.
6. Compter les notes épinglées.
7. Compter les notes archivées.
8. Calculer la date de modification la plus récente.
9. Calculer la date de création la plus ancienne.
10. Compter les caractères des corps sans dépassement `Int`.
11. Construire le résumé uniquement après validation complète du schéma JSON v1.
12. Exposer l’action « Vérifier une sauvegarde » dans le menu import/export.
13. Sélectionner le fichier via le Storage Access Framework, sans permission globale.
14. Lire le fichier sans modifier la bibliothèque locale.
15. Afficher un résumé lisible dans une boîte de dialogue.
16. Mentionner explicitement qu’aucune note n’a été importée.
17. Rejeter les versions futures du format d’archive.
18. Rejeter les documents JSON malformés.
19. Traduire les libellés et avertissements en français.
20. Traduire les libellés et avertissements en anglais.
21. Centraliser la lecture de documents SAF dans `NotesDocumentIo`.
22. Refuser toute lecture dépassant 16 Mio.
23. Accepter exactement la limite maximale autorisée.
24. Détecter les fournisseurs de documents qui bloquent indéfiniment sans progression.
25. Réutiliser cette protection pour import JSON, import TXT/Markdown et inspection.
26. Conserver la vérification des sauvegardes disponible lorsque la base locale ne peut pas être lue.
27. Ajouter des tests JVM et Android sur les limites, la corruption, les résumés et l’accès à la récupération.

## Vérifications
- [ ] Dernier SHA : `testDebugUnitTest` vert.
- [ ] Dernier SHA : `lintDebug` vert.
- [ ] Dernier SHA : `compileDebugAndroidTestKotlin` vert.
- [ ] Dernier SHA : `assembleDebug` vert.
- [ ] Dernier SHA : `connectedDebugAndroidTest` vert sur émulateur.
- [ ] Pixel 9 : sélectionner un export de test et afficher le résumé sans importer.
- [ ] Pixel 9 : importer ensuite dans une **installation de test séparée** et comparer titre/corps/ID/date.
- [ ] Pixel 9 : tester annulation du sélecteur, document corrompu, refus d’accès SAF, fond d’écran et taille de police.

Un document « lisible » signifie uniquement que son JSON peut être décodé selon la version supportée. Cela ne prouve **ni** que le contenu correspond à une sauvegarde antérieure **ni** qu’une restauration complète a réellement été effectuée. Le contrôle SHA-256 après écriture reste distinct de cette fonctionnalité.

## Rollback
Revenir sur une branche de secours au commit `25edcddd` sans réinitialiser les données SQLite. Ne jamais désinstaller une application contenant des notes, forcer une fusion ni remplacer une installation existante avec un APK signé différemment. L’utilisateur valide tout déploiement.
