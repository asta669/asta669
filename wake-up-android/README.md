# Wake Up! pour Android

Application native en Kotlin, compatible avec Android 8.0 et versions ultérieures.
Son identifiant Android est `com.asta669.wakeup`.

## Fonctionnement

- Programmation de l'alarme avec le service système Android `AlarmManager`.
- Son sur le canal audio des alarmes, vibration et écran de résolution des défis.
- Choix de 1, 3 ou 5 défis de calcul pour arrêter l'alarme.
- Bouton de report de cinq minutes.
- Répétition quotidienne et reprogrammation prévue après redémarrage du téléphone.
- Briefing vocal Jarvis facultatif après l'arrêt de l'alarme.

Le son, l'affichage sur l'écran verrouillé et le comportement en veille doivent
être vérifiés sur le téléphone utilisé, avec les permissions appropriées.

## Installer l'application

Un **APK** est le fichier qui installe l'application sur Android.

1. Dans l'onglet **Actions** de GitHub, ouvrez **Build Wake Up! APK** et sélectionnez
   une exécution réussie correspondant à la version souhaitée.
2. Téléchargez l'archive **WakeUp-debug-apk**, puis extrayez `WakeUp.apk`.
3. Copiez cet APK sur le téléphone et ouvrez-le. Autorisez l'installation depuis
   l'application utilisée pour ouvrir le fichier si Android le demande.

Un lancement manuel du workflow sur la branche `main` publie également l'APK
dans **Releases**, sous l'étiquette `latest-apk`, si la compilation et la publication
réussissent. Les autres exécutions fournissent uniquement l'archive téléchargeable
dans **Actions**. Vérifiez le résultat de l'exécution.

Une compilation de débogage est signée automatiquement avec une clé de débogage.
Cette clé peut différer entre deux machines : Android peut refuser une mise à jour
signée avec une autre clé. Une distribution stable nécessite une clé de signature
conservée et une compilation de publication.

## Développer et compiler

Ouvrez ce dossier dans [Android Studio](https://developer.android.com/studio).
Le projet utilise **Java 17**, **Gradle 8.7**, le plugin Android **8.5.2** et
Kotlin **1.9.24**.

Avec le gestionnaire de SDK Android, installez `platform-tools`,
`platforms;android-34` et `build-tools;34.0.0`. Indiquez le SDK avec `ANDROID_HOME`
ou un fichier local `local.properties` contenant `sdk.dir=...` ; ce fichier est
ignoré par Git.

Le dépôt contient la configuration du wrapper Gradle, mais pas son script ni son
JAR. Avec une installation de **Gradle 8.7** et **Java 17**, générez-les puis compilez :

```bash
cd wake-up-android
gradle wrapper --gradle-version 8.7 --distribution-type bin
./gradlew assembleDebug --no-daemon
```

Sous Windows, utilisez `gradlew.bat` pour la seconde commande. L'APK local se trouve
dans `app/build/outputs/apk/debug/app-debug.apk` ; le workflow GitHub le renomme
`WakeUp.apk`. Ses étapes figurent dans
[le workflow Android](../.github/workflows/android.yml).

## Configurer le téléphone

Autorisez les notifications et les alarmes exactes lorsque l'application le
demande. Vérifiez aussi l'autorisation d'affichage plein écran si Android la propose.
Selon le fabricant, les restrictions de batterie ou le démarrage automatique
peuvent demander un réglage supplémentaire.

Programmez d'abord une alarme proche pour vérifier le fonctionnement sur votre
appareil avant de l'utiliser pour un réveil important.

## Configurer Jarvis

Dans les paramètres Jarvis, renseignez votre nom et votre routine, puis activez
le briefing après l'alarme. Autorisez l'accès au calendrier pour inclure les
rendez-vous enregistrés ou synchronisés sur le téléphone. Installez une voix
française de synthèse vocale dans Android et utilisez le bouton de test Jarvis.

Une clé Gemini est facultative et peut être saisie dans l'application pour
générer le texte avec ce service. Dans ce mode, les informations du briefing
sont envoyées à Gemini ; les conditions et quotas dépendent de votre compte
Google AI Studio. Sans clé utilisable ou en cas d'échec de la requête, le code
prévoit un texte de remplacement local.

La clé est enregistrée dans les préférences locales de l'application, pas dans
le fichier APK. Ne l'ajoutez pas aux fichiers du dépôt.

## Vérifier sur appareil

Le dépôt ne fournit actuellement aucune suite de tests Android automatisés.
Une compilation réussie ne valide pas le comportement sur téléphone. Vérifiez :

- programmation, annulation et déclenchement à l'heure prévue ;
- déclenchement avec écran allumé puis verrouillé ;
- rejet des mauvaises réponses et résolution de 1, 3 et 5 défis ;
- report et reprise après cinq minutes ;
- conservation des réglages et reprogrammation après redémarrage ;
- briefing Jarvis avec et sans accès au calendrier ou au réseau.

## Organisation du code

Les sources Kotlin se trouvent dans `app/src/main/java/com/asta669/wakeup/` :

| Fichier | Rôle |
| --- | --- |
| `MainActivity.kt` | Réglage et programmation du réveil |
| `AlarmScheduler.kt` | Programmation auprès du système |
| `AlarmReceiver.kt` | Déclenchement et répétition |
| `AlarmService.kt` | Son, vibration et notification |
| `AlarmActivity.kt` | Défis et report |
| `BootReceiver.kt` | Reprogrammation après redémarrage |
| `Prefs.kt` | Réglages persistants |
| `JarvisActivity.kt` | Lecture du briefing |
| `JarvisSettingsActivity.kt` | Paramètres Jarvis |
| `JarvisBrain.kt` | Génération du texte |
| `CalendarReader.kt` | Lecture des rendez-vous autorisés |

`app/src/main/res/` contient les écrans, icônes, sons et thèmes Android.
`app/src/main/AndroidManifest.xml` déclare les composants et permissions.
