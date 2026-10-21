# RAVAN AI (Stage 2)

Project shell: Gradle setup, manifest, Material 3 red-neon dark theme, Hilt, splash screen, and placeholder home.

## Build options

**Option A (recommended): GitHub Actions.** Push this folder to a GitHub repo. The workflow in `.github/workflows/build-apk.yml` builds the debug APK. Download it from the Actions tab (Artifacts).

**Option B: Android Studio on a PC.** Open the `RavanAI` folder, let Gradle sync, then Build > Build APK.

**Option C: Termux.** Good for editing and git. Building the APK inside Termux is hard because the Android SDK build tools (aapt2) are not built for Termux. Use Option A or B to build.

## Install
Transfer the APK to the phone, allow installs from unknown sources, and open it.

## Stage plan
Stage 3: domain models, command router, local intents.
Stage 4: voice input and output (SpeechRecognizer, TTS).
Later stages add device actions, Gemini, storage, and settings.
