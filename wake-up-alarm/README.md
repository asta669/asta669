# ⏰ Wake Up!

An alarm clock that **won't shut off until you prove you're actually awake.** No more slapping snooze in your sleep — you have to solve a challenge to make the noise stop, so you're forced to get out of bed.

Built as a single, self-contained HTML file. No installs, no accounts, no internet needed after loading. Nothing ever leaves your device.

## ✨ Features

- 🔔 **Loud, escalating alarm** generated in your browser (harsh detuned tone that pulses — no sound files needed)
- 🧠 **"Prove you're awake" challenges** — pick how you dismiss it:
  - **Math** — solve arithmetic problems
  - **Type** — type a phrase exactly
  - **Memory** — memorize and repeat a number
  - **Tap** — tap a button 30 times
- 🎚️ **Difficulty** — 1, 3, or 5 challenges before it stops
- 🔊 **Gradual fade-in volume** so you wake gently, then firmly
- 😴 **Snooze** (5 min) — optional, you can turn it off for tough-love mode
- 📳 **Vibration** on supported phones
- 🌙 **Keeps screen awake** where the browser allows it
- 💾 **Remembers your settings**

## ▶️ How to use

1. Open `index.html` in any browser (double-click it, or host it).
2. Set your wake time, choose a challenge type and difficulty.
3. Tap **Set alarm**. Tap **▶︎ Test alarm now** first to see how it feels.
4. **Keep the tab open.** You can lock your phone, but don't close the browser tab — that's how it keeps the alarm running.

> 📱 **Phone tip:** For the most reliable alarm, keep the tab open and your phone plugged in. Browsers limit background audio, so an always-on tab is the trick. "Set alarm" and "Test" both unlock audio playback (browsers require a tap first).

## 🛠️ Tech

Plain HTML, CSS, and vanilla JavaScript in one file. The alarm sound is synthesized with the **Web Audio API** — two detuned oscillators pulsed by an LFO — so there are no audio assets to download and it works fully offline.

## 💡 Ideas for later

- Repeating weekday alarms
- Barcode/QR "walk to the kitchen and scan" mode
- Multiple alarms
- Installable PWA with a service worker
- Sync a real morning routine / checklist

---
Made for [@asta669](https://github.com/asta669) 🌅
