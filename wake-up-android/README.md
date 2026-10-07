# Wake Up! 2 — Android

Application Kotlin pour Android 8.0 et versions ultérieures, identifiant
`com.asta669.wakeup`, version `2.0` (`versionCode 2`). La cible de validation
matérielle est un Redmi Note 13 sous Android 15 / HyperOS 2.

## Préparer son réveil

1. Ouvrez **Associer mon QR code**. Scannez un QR code existant, ou générez-en un
   puis utilisez **Imprimer / enregistrer en PDF**. Conservez le document avant
   de confirmer l'association et accrochez-le dans la cuisine.
2. Autorisez la caméra. La reconnaissance du code fonctionne hors ligne.
3. Ouvrez **Vérifier les autorisations** depuis l'accueil. Accordez les alarmes
   exactes, les notifications et l'affichage plein écran. Revenez dans l'application
   après chaque réglage ; l'état réel des autorisations est affiché.
4. Choisissez votre heure en touchant la grande horloge, puis **Activer le réveil**.
5. Essayez d'abord une alarme proche sur votre téléphone, notamment après avoir
   balayé l'application hors des applications récentes et verrouillé l'écran.

Sur Redmi / HyperOS, vérifiez également les réglages propres au fabricant pour
l'affichage sur l'écran verrouillé, le démarrage en arrière-plan et la batterie.
Leur intitulé varie selon la version. Le code utilise un réveil système exact,
un service indépendant de l'écran principal et une notification d'alarme avec
accès direct au scanner. Si Android refuse l'ouverture plein écran, touchez la
notification. Une application ordinaire ne peut pas garantir ce comportement
contre toutes les restrictions du fabricant, un arrêt forcé ou un téléphone éteint.

## Arrêter l'alarme

Scannez le QR code **associé**. Les autres codes sont refusés. Les défis de calcul
et le report ont été retirés. L'alarme quotidienne reste programmée après l'arrêt.
La caméra peut être éclairée depuis l'écran du réveil.

Si le code ou la caméra est inutilisable, maintenez **Secours**, puis confirmez
l'arrêt. Ce secours reste accessible, y compris après une mise à jour depuis une
ancienne version qui n'avait pas encore de code associé.

Le mode **Tester le réveil** est distinct du réveil programmé. Il dispose d'un
bouton d'arrêt et s'arrête automatiquement après une minute.

## Sonneries

Trois boucles originales sont proposées : **Pulsation**, **Balise** et **Ascension**.
Le script `tools/generate_alarm_sounds.py` permet de les reproduire sans échantillon
externe. Le volume système des alarmes est réglé au maximum dès le démarrage.
L'écran traite les touches de volume et le service rétablit le maximum pendant
la sonnerie. Android et les politiques du téléphone conservent la priorité sur
certaines commandes système : ce maintien doit être essayé sur l'appareil réel.

Après l'arrêt, l'application restaure le volume d'alarme qu'elle avait mémorisé.

## School ON / OFF

**School ON** mémorise les volumes ordinaires et le mode d'interruption avant de
les modifier. **School OFF** restaure les réglages, même après une fermeture de
l'application. Appuyer plusieurs fois sur ON n'écrase pas la sauvegarde initiale.

- Les médias, sonneries d'appel et notifications sont mis en silence selon les
  permissions accordées ; les alarmes restent prioritaires.
- L'accès à **Ne pas déranger** permet le filtre « alarmes seules ».
- Si une permission est absente ou révoquée, l'écran indique l'application
  partielle ou la restauration restant à terminer.
- Le bouton **Mode avion** ouvre le réglage Android et l'écran affiche son état
  réel. Son interrupteur doit être actionné par l'utilisateur : une application
  Android standard ne peut pas l'activer ou le désactiver silencieusement.

## Jarvis et données

Jarvis peut lire un briefing après un scan réussi. Renseignez votre nom et votre
routine et autorisez le calendrier si vous souhaitez inclure vos rendez-vous.
Une voix française doit être disponible dans les réglages de synthèse vocale Android.

Sans clé Gemini utilisable, le briefing est construit localement. Si vous activez
Jarvis avec une clé, les informations du briefing sont envoyées à l'API Google
Gemini ; les conditions et quotas dépendent de votre compte.

Le contenu du calendrier est transmis comme donnée distincte des instructions
système. Jarvis produit seulement du texte et ne dispose d'aucun outil pour agir
sur le téléphone. Cela réduit l'impact des instructions trompeuses contenues dans
un événement, sans constituer une garantie absolue sur le texte généré.

Les QR codes sont traités comme des identifiants opaques : aucun lien ou commande
n'est ouvert. Seule une empreinte SHA-256 du code associé est conservée. Les
préférences, dont la clé Gemini éventuelle, restent privées à l'application et
sont exclues des sauvegardes configurées. Aucun secret ne doit être ajouté au dépôt.

## Compiler et vérifier

Pré-requis : Java 17, SDK `platforms;android-34`, `build-tools;34.0.0` et
`platform-tools`. Définissez `ANDROID_HOME` ou renseignez `sdk.dir` dans le fichier
local `local.properties` ignoré par Git. Le wrapper Gradle 8.7 est inclus avec le
SHA-256 attendu de sa distribution.

```bash
cd wake-up-android
./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon --max-workers=2
```

Sous Windows, utilisez `gradlew.bat`. L'APK se trouve dans
`app/build/outputs/apk/debug/app-debug.apk`. Les rapports sont dans
`app/build/reports/` ; les aperçus des écrans sont rendus par les tests Android
Robolectric dans `app/build/reports/ui-preview/`.

Les tests couvrent les horaires, les sessions d'alarme, le volume, les QR codes,
la restauration School, les préférences et l'ouverture des écrans. Robolectric
simule Android et ne valide pas les restrictions HyperOS, le haut-parleur réel,
la caméra physique ni le comportement sur écran verrouillé d'un appareil réel.

## Installer l'APK

Dans GitHub **Actions → Build Wake Up! APK**, choisissez une exécution réussie,
puis téléchargez l'archive **WakeUp-debug-apk** contenant `WakeUp.apk`. Un lancement
manuel du workflow sur `main` peut aussi publier ce fichier dans **Releases**.

Un APK de débogage est signé avec une clé de débogage. Si la version déjà installée
sur le téléphone utilise une autre signature, Android refusera la mise à jour.
Ne désinstallez pas sans avoir relevé vos réglages : une désinstallation efface
les données de l'application. Une distribution durable nécessite une signature
conservée d'une version à l'autre.

## Vérifications à terminer sur Redmi

Tester les permissions refusées puis accordées, l'alarme après balayage de
l'application, l'écran verrouillé, les touches de volume, le scan du bon et d'un
mauvais code, le secours, les trois sons, le redémarrage du téléphone et les
réglages School avant/après. Ces essais matériels restent distincts des tests
automatisés et de la compilation.
