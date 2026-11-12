# Rebirth — validation locale du 12 novembre 2026

Branche : feature/noma-replica-notes-rebirth. Aucun merge ni push CI.

## Sources et limites

- Figma twjFzxB9jjaKoisZlUavKD : session ouverte, lecture refusée par quota Starter. Aucun écran Figma observé pendant ce cycle.
- Trois captures annoncées : absentes des pièces jointes et du dépôt. Aucun avant/après inventé.
- ADB : aucun appareil connecté. Les tests Android compilés ne sont pas des tests instrumentés exécutés.
- Références consultées : [Apple HIG](https://developer.apple.com/design/human-interface-guidelines.md), [Notes](https://support.apple.com/en-nz/118442), [Bear](https://bear.app/faq/typography-options/), [Craft](https://www.craft.do/s/fegziZPWTrBfn0), [Keep](https://support.google.com/keep/answer/6191044), [Obsidian](https://obsidian.md/help/plugins/search), [Simplenote](https://simplenote.com/help/), [Material foundations](https://m3.material.io/foundations/), [Android adaptive](https://developer.android.com/develop/ui/compose/layouts/adaptive), [application de référence](https://play.google.com/store/apps/details?id=com.inotesapp.inotes), [Replica](https://github.com/Jakeschincariol/replica-skill).
- Principes retenus : bibliothèque compacte, filtres explicites, écriture continue, contrôles secondaires contextuels, réglages proportionnés. Aucune reprise d'assets.

## Inventaire initial priorisé

1. Filtres français longs et ligne amputée par le bouton de tri.
2. Cartes avec minimum 100 dp, tuile décorative et badges redondants.
3. Éditeur : quatre actions en haut, statut permanent, minimum 360 dp et marge basse 80 dp sous IME.
4. Apparence : dialogue limité à 430 dp avec grandes options, dégradés ne représentant pas le thème réel.
5. Accent limité aux rôles primary ; plusieurs surfaces restent vertes indépendamment du choix.
6. Recherche existante déjà normalisée et pertinente ; conserver ses protections Unicode.
7. Préserver tous les contrats SQLite, import/export, autosave, sauvegarde échouée et capture.

La cible de 15 changements par étape n'est pas une validation : seuls les comportements effectivement modifiés seront comptés. Les anciens changements documentés dans docs/ui ne sont pas recomptés.
