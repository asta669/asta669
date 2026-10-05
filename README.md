# Wake Up! pour Android

Application Android native de réveil, écrite en Kotlin. Pour arrêter l'alarme,
il faut résoudre des défis de calcul. Un briefing vocal Jarvis peut ensuite
présenter la routine du matin et les rendez-vous du jour.

Le projet se trouve dans [`wake-up-android/`](wake-up-android/).
Il nécessite Android 8.0 ou une version ultérieure.

## Installer et utiliser

Consultez le [guide Android](wake-up-android/README.md) pour télécharger un APK,
autoriser les alarmes et les notifications, puis configurer le réveil et Jarvis.
Un APK est le fichier d'installation de l'application sur un téléphone Android.

## Développer

Ouvrez le dossier `wake-up-android` dans Android Studio. La compilation utilise
Java 17, Gradle 8.7 et le SDK Android 34. Les commandes et les vérifications sur
appareil sont détaillées dans le [guide Android](wake-up-android/README.md).

L'automatisation GitHub est décrite dans
[`.github/workflows/android.yml`](.github/workflows/android.yml).

## Organisation

```text
wake-up-android/       Code, ressources et configuration de l'application
.github/workflows/    Construction de l'APK sur GitHub
```
