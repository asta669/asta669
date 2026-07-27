# ⏰ Wake Up! — native Android app

A **real Android alarm app** that forces you out of bed. Unlike a web page, it can
do the things a browser can't:

- ⏰ **Rings at an exact time even when the phone is locked / asleep** (uses the system `AlarmManager.setAlarmClock`, which is exempt from Doze & battery saver).
- 🔊 **Plays on the ALARM audio channel** — so it rings **even in silent / vibrate mode**, and it forces the alarm volume to max so it can't be quietly turned down.
- 📱 **Full-screen alarm over the lock screen**, just like the built-in Clock app.
- 🧠 **You must solve math challenges to turn it off** (1, 3, or 5, your choice). No sleepy snoozing.
- 😴 Optional **5-minute snooze**.
- 🔁 **Repeats daily** and **re-arms itself after a reboot**.
- 📳 Vibrates, holds a wake-lock, works fully offline.
- 🤵 **Jarvis** — an optional spoken morning brief right after you dismiss the alarm.

## 🤵 Jarvis — the spoken morning brief

After you turn off the alarm, Jarvis greets you out loud ("Bonjour Monsieur…"),
reads **today's calendar events**, and encourages your **morning routine**
(footing, reading, writing on Substack…).

- 🧠 **Free AI brain** via Google **Gemini** (free API key, no credit card).
- 📅 Reads **today's Google Calendar** events already synced on your phone — no login, no cost.
- 🔊 Speaks with Android's built-in **French** text-to-speech.
- 🌙 Works **offline too**: with no key (or no network) it speaks a built-in brief from your routine + calendar.

### Set it up (all free)
1. Get a **free Gemini API key**: open **aistudio.google.com/apikey**, sign in with Google, **Create API key**, copy it (starts with `AIza…`).
2. In the app, tap **"🤵 Jarvis — briefing parlé"** → paste the key, set your name and routine, tick **"Activer Jarvis après l'alarme"** and **"Lire mes rendez-vous"** (allow calendar access), then **Enregistrer**.
3. Tap **"▶ Tester Jarvis"** to hear it now.
4. Make sure a **French voice** is installed: Android *Settings → Languages → Text-to-speech → French*.

> The Gemini free tier is far more than enough for one brief a day. Your key is stored only on your phone — don't share the APK if you've entered your key.

## 📥 Get the app on your phone — two ways

### Option A — Let GitHub build the APK for you (no tools needed)
1. Push this folder to GitHub (already done if you're reading this in your repo).
2. On GitHub, open the **Actions** tab → the **“Build Wake Up! APK”** workflow → the latest run.
   *(If it hasn't run, click the workflow and press **Run workflow**.)*
3. Download the **`WakeUp-debug-apk`** artifact at the bottom of the run. Unzip it to get `app-debug.apk`.
4. Copy `app-debug.apk` to your phone and open it. Allow **“Install unknown apps”** when prompted, then install.

### Option B — Build it on your PC with Android Studio
1. Install [Android Studio](https://developer.android.com/studio) (Windows/Linux).
2. **File → Open** and select this `wake-up-android` folder. Let it sync Gradle.
3. Plug in your phone with **USB debugging** on, then press **Run ▶**. The app installs and launches.
   *Or* build an APK from **Build → Build Bundle(s)/APK(s) → Build APK(s)**.

### Command line (advanced)
```bash
cd wake-up-android
gradle wrapper --gradle-version 8.7   # first time only (creates ./gradlew)
./gradlew assembleDebug               # APK -> app/build/outputs/apk/debug/app-debug.apk
```

## 🔐 Permissions to allow (so the alarm is reliable)
On first use the app will ask for these — please allow them:
- **Notifications** — needed to show the full-screen alarm.
- **Alarms & reminders** (exact alarms) — the app opens this setting; toggle it ON and tap *Set alarm* again.
- On some phones (Xiaomi, Samsung, Oppo…), also enable **Autostart** and set the app's battery mode to **Unrestricted / No restrictions**, so the system doesn't kill it before your alarm.

## 🧪 Try it now
Open the app → tap **“▶ Test alarm now”** to preview the full ringing experience immediately (solve the challenge to stop it).

## 🗂️ Project structure
```
wake-up-android/
├─ app/src/main/
│  ├─ AndroidManifest.xml
│  ├─ java/com/asta669/wakeup/
│  │  ├─ MainActivity.kt      # set the alarm time & difficulty
│  │  ├─ AlarmScheduler.kt    # schedules exact alarms via AlarmManager
│  │  ├─ AlarmReceiver.kt     # fires at alarm time, re-arms for next day
│  │  ├─ AlarmService.kt      # foreground service: sound + vibration + full-screen
│  │  ├─ AlarmActivity.kt     # the "solve to dismiss" ringing screen
│  │  ├─ BootReceiver.kt      # re-arms the alarm after a reboot
│  │  └─ Prefs.kt             # saved settings
│  └─ res/                    # layouts, icons, alarm sound, theme
└─ build.gradle, settings.gradle, ...
```

## ⚠️ Note
This is a personal-use **debug** build (unsigned). That's perfectly fine for installing on your
own phone. To publish on the Play Store you'd create a signed release build and a developer account —
ask if you ever want to go there.
