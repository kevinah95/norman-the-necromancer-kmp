# Norman The Necromancer — Kotlin Multiplatform (KMP) & KorGE

Educational recreation in **Kotlin Multiplatform (KMP)** using the 2D game engine **[KorGE](https://docs.korge.org/getting-started/)**, based on the original work by **Dan Prince**.

The game is fully encapsulated within the `:shared` (`commonMain`) shared module and natively distributed to **all platforms**:
- 🤖 **Android** (`:androidApp`)
- 🖥️ **Desktop JVM** (`:desktopApp`)
- 🌐 **Web JavaScript & WebAssembly (WasmJS)** (`:webApp`)
- 🍏 **iOS** (`:iosApp` and native `:shared` framework for Apple Silicon / Simulator)

---

## 🎮 Mobile / Touch Improvements & Optimization

The original TypeScript/HTML5 game was designed for mouse and keyboard (mouse cursor aiming, Spacebar for Resurrect, and numpad/arrow keys for the shop). This KMP version has been optimized for touchscreens and mobile devices while preserving the desktop experience:

1. **Touch Aiming & Shooting**:
   - Touch and drag on the screen to calculate the angular trajectory relative to Norman and orient the crosshair.
   - Release your finger (`onUp`) to cast the projectile.
2. **Dedicated Touch Resurrect Button**:
   - Ergonomic button at the bottom with a dynamic frame, visible cooldown timer in seconds, and a ready indicator icon.
3. **Accessible Touch Shop**:
   - Each ritual and upgrade in the shop can be selected by tapping directly on its row.
   - Interactive touch buttons for **[ BUY ]** and **[ NEXT WAVE ]**.
4. **Pause and Dialogues**:
   - Touch button `||` in the top corner to pause/resume.
   - Tap anywhere on the screen to advance or skip story dialogues.
5. **Virtual Resolution and Scaling**:
   - Fixed internal retro resolution of `400 x 200` pixels (`ScaleMode.SHOW_ALL`), automatically adapting to any phone aspect ratio (16:9, 19.5:9, 21:9, tablets) and desktop windows with crisp, undistorted pixel art.

---

## 🏛️ Project Architecture (`shared/src/commonMain`)

The codebase is organized in a modular and educational structure under the `io.github.kevinah95.norman_the_necromancer` package:

```
shared/src/commonMain/kotlin/io/github/kevinah95/norman_the_necromancer/
├── NormanGame.kt          # Common entry point initializing KorGE and the scene
├── assets/
│   └── GameAtlas.kt       # Loads sprites and fonts with Compose Resources (Res.readBytes) and parses sprites.json
├── core/
│   ├── Tags.kt            # Bitmasks for collisions and entity types (CORPSE, LIVING, SPELL, etc.)
│   ├── Geometry.kt        # 2D Vectors, Rectangles, angles, and math utilities
│   ├── GameObject.kt      # Base entity with physics (x, y, vx, vy, mass, bounce, friction)
│   ├── Behaviour.kt       # Decoupled behavior components (Strategy/Component pattern)
│   ├── Damage.kt          # Damage and death events
│   ├── Ritual.kt          # Passive and active upgrade definitions
│   ├── Tweens.kt          # Movement interpolation system and screenshake
│   └── Game.kt            # Physics loop, collisions, and overall game state
├── entities/
│   └── Entities.kt        # Factories for enemies and allies (Norman, Skeletons, Kings, Monks, etc.)
├── behaviours/
│   └── Behaviours.kt      # Behaviors: March, Attack, Damaging, Bleeding, Enraged, Seeking, etc.
├── rituals/
│   └── Rituals.kt         # The 20 in-game rituals (Bouncing, Rain, Doubleshot, Electrodynamics, etc.)
├── levels/
│   └── Levels.kt          # Wave scripting, enemy progression, minibosses, and final boss
├── shop/
│   └── Shop.kt            # Purchasing logic, procedural generation, and soul pricing
├── fx/
│   ├── ParticleEmitter.kt # Particle physics simulation (gravity, friction, bounce)
│   └── Fx.kt              # Preconfigured visual effects (bones, trails, clouds, dust)
├── audio/
│   └── GameAudio.kt       # Procedural audio synthesis engine and dynamic adaptive soundtrack (Ambient Organ, Bass, Kick, King's Theme)
├── renderer/
│   └── GameRenderer.kt    # Optimized rendering with KorGE BatchBuilder2D and NineSlice
└── scene/
    └── NormanGameScene.kt # Main KorGE scene managing lifecycle and touch/keyboard input events
```

### 🎵 Dynamic Adaptive Soundtrack (`sounds.ts` port)
Dan Prince's original game does not use external audio files (such as .mp3 or .ogg), but rather a real-time procedural synthesizer powered by the Web Audio API. This KMP version recreates all instruments with mathematical fidelity:
- **Ambient Organ:** Periodic Fourier harmonic series `[-0.8, 1, 0.8, 0.8, -0.8, -0.8, -1]` arpeggiated across notes A1 (55Hz) and A0 (27.5Hz), with a 200Hz high-pass filter and cathedral reverb.
- **Procedural Bass:** Sawtooth wave playing an algorithmic bassline in A Harmonic Minor.
- **Sub-Kick:** 808-style sub-bass kick drum with an exponential pitch sweep (150Hz down to 0Hz) and an 80Hz low-pass filter.
- **King's Theme:** 3-voice gothic organ counterpoint (soprano, tenor, and sub-bass pedal) dynamically triggered during the final confrontation against The King (Wave 10 / script level 9).

**Dynamic Progression:**
- **Level 1:** Mysterious ambient organ arpeggio.
- **Level 2:** The rhythmic bassline joins in.
- **Level 3+:** The kick drum enters, completing the 240 BPM beat.
- **Shop:** The kick drum smoothly fades out, creating a relaxed shopping atmosphere.
- **Final Boss (The King):** Standard synthesizers cut out, giving way to the King's gothic organ fugue.
- **Phase 4 of The King:** The kick drum re-enters with intensity for the final close-quarters combat.

### 📦 Cross-Platform Resource Management (`composeResources`)
The game assets (`sprites.png`, `sprites.json`, `font.png`, `font.json`) reside in:
`shared/src/commonMain/composeResources/files/`

They are loaded using the official Compose Multiplatform standard:
```kotlin
val pngBytes = Res.readBytes("files/sprites.png")
bitmap = pngBytes.openAsync().readBitmap() // Decoded by KorGE Image Format
val jsonStr = Res.readBytes("files/sprites.json").decodeToString()
```
This ensures assets are automatically packaged into the Android APK, Desktop JAR, Web bundles, and iOS framework without relying on local filesystem paths.

---

## ⚡ Taskfile Shortcuts

If you have [Task](https://taskfile.dev/) installed, you can manage all platforms with a single command:

```bash
task                  # Display available tasks
task desktop          # Launch the game on Desktop (JVM)
task build-desktop    # Build the Desktop JAR / distribution
task package-desktop  # Package the native app (.dmg on macOS)
task android          # Build, install, and run the game on Android
task build-android    # Build the Android Debug APK
task web-js           # Start the JavaScript Web development server
task build-web-js     # Build the Web production bundle (JS Webpack)
task web-wasm         # Start the WasmJS Web development server
task build-web-wasm   # Build the Web production bundle (WasmJS Webpack)
task ios              # Build the iOS framework for arm64 simulator
task build-ios        # Build iOS frameworks for simulator and device
task test             # Run unit tests on the JVM
task build            # Build all modules (Desktop, Android, Web JS, and Wasm)
task clean            # Clean caches and build directories
```

---

## 🚀 Direct Execution with Gradle

### Desktop (JVM)
```bash
./gradlew :desktopApp:run
```

### Android
Connect an Android phone or start an emulator:
```bash
./gradlew :androidApp:installDebug
```
Or to build the APK:
```bash
./gradlew :androidApp:assembleDebug
```
APK location: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

### Web (JavaScript & WasmJS)
To run the in-browser development server:
```bash
# WasmJS version (recommended for modern browsers)
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# JavaScript version
./gradlew :webApp:jsBrowserDevelopmentRun
```

### iOS
Open the [iosApp/iosApp.xcodeproj](./iosApp/iosApp.xcodeproj) project in Xcode and select your iPhone/iPad simulator or physical device. You can also build the framework directly from the terminal:
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### Unit Tests
```bash
./gradlew :shared:jvmTest
```