# ChromaTap Animated Keyboard

A high-fidelity Samsung KeysCafe replica keyboard app for Android with neon RGB tap animations, glow particles, and dynamic lighting effects. Built entirely with Jetpack Compose.

## Features

- 🌈 **RGB Neon Key Animations** — Each key tap triggers cyan/pink/white neon transitions
- ✨ **Glow Particle System** — Animated splash ripples propagate from tap position
- 🎨 **Full Keyboard Layouts** — Letters, Symbols, and Emoji modes
- 📳 **Haptic Feedback** — Vibration on every key press
- 🌑 **Dark AMOLED Theme** — Deep black background, battery-friendly
- 🤖 **Gemini AI Integration** — Firebase AI for smart keyboard features

## Screenshots

> Coming soon

## Tech Stack

- Kotlin + Jetpack Compose
- Firebase AI (Gemini)
- Room Database
- Retrofit + Moshi
- AGP 9.1.1 / Kotlin 2.2.10

## Setup

### Prerequisites
- Android Studio Narwhal or later
- JDK 21
- Gemini API key (free at [ai.google.dev](https://ai.google.dev))

### Run Locally

1. Clone the repo:
   ```bash
   git clone https://github.com/therealrehman/ChromaTapKeyboard.git
   cd ChromaTapKeyboard
   ```

2. Create a `.env` file in the root:
   ```
   GEMINI_API_KEY=your_api_key_here
   ```

3. Open in Android Studio and run on device/emulator.

### Build from Command Line
```bash
./gradlew assembleDebug
```
APK will be at `app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions

Every push to `main` automatically builds a debug APK. Add your `GEMINI_API_KEY` in:
> GitHub repo → Settings → Secrets and variables → Actions → New repository secret

## License

MIT License — see [LICENSE](LICENSE)

---

*Developed by [@therealrehman](https://github.com/therealrehman)*
