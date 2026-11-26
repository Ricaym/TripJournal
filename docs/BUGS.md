# Bugs vérifiés

## 1. Compatibilité KSP et AGP

- **Reproduction :** lancer la compilation sur la base de `main` avec AGP 9.5.1 et KSP 2.0.21-1.0.28.
- **Impact :** Gradle échoue pendant la configuration des sources Kotlin, avant la compilation de l’application.
- **Développeur concerné :** El-bak, auteur du correctif de compatibilité dans la PR #1.
- **État :** les options de compatibilité proposées dans la PR sont reprises dans cette branche; la compilation reste à confirmer par Gradle.

## 2. Modification d’un lieu inexistant qui crée un nouveau lieu

- **Reproduction :** ouvrir `PlaceFormViewModel` en mode édition avec un identifiant absent de la base, puis enregistrer le formulaire.
- **Impact :** le chargement laisse `_place` à `null`; `save()` construit alors un `Place` avec l’identifiant `0`, et le dépôt l’insère comme un nouveau lieu au lieu de signaler que le lieu à modifier est introuvable.
- **Développeur concerné :** El-bak, auteur de `PlaceFormViewModel` dans la PR #1.
- **Correction suggérée :** désactiver l’enregistrement ou émettre un événement « lieu introuvable » lorsque le chargement d’un lieu demandé en édition échoue.
