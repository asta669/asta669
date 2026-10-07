# Wake Up! pour Android

Un réveil natif qui vous accompagne jusqu'à la cuisine : associez un QR code,
programmez l'heure, puis scannez le code pour arrêter la sonnerie.

La version 2 ajoute une interface claire aux cartes arrondies, trois sonneries
originales au volume maximal, le contrôle des autorisations d'alarme et un mode
**School ON / OFF**. Le briefing vocal Jarvis reste facultatif.

Le code est dans [`wake-up-android/`](wake-up-android/). Le
[guide Android](wake-up-android/README.md) explique l'installation, les permissions,
le QR code et les vérifications à effectuer sur téléphone.

## Développer

Java 17, Android SDK 34 et Gradle 8.7 (wrapper inclus) :

```bash
cd wake-up-android
./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon --max-workers=2
```

L'APK de test est produit dans `app/build/outputs/apk/debug/app-debug.apk`.
La [configuration GitHub Actions](.github/workflows/android.yml) exécute les mêmes
contrôles. La publication d'une release demande un lancement manuel sur `main`.
