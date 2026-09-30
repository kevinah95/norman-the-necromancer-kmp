# Norman The Necromancer — Kotlin Multiplatform (KMP) & KorGE

Recreación didáctica en **Kotlin Multiplatform (KMP)** usando el motor de videojuegos 2D **[KorGE](https://docs.korge.org/getting-started/)**, basado en la obra original de **Dan Prince**.

El juego está completamente encapsulado en el módulo compartido `:shared` (`commonMain`) y distribuido de forma nativa a **todas las plataformas**:
- 🤖 **Android** (`:androidApp`)
- 🖥️ **Desktop JVM** (`:desktopApp`)
- 🌐 **Web JavaScript & WebAssembly (WasmJS)** (`:webApp`)
- 🍏 **iOS** (`:iosApp` y `:shared` framework nativo para Apple Silicon / Simulador)

---

## 🎮 Mejoras y Optimización Mobile / Touch

El juego original en TypeScript/HTML5 estaba diseñado para ratón y teclado (apuntado con cursor de mouse, barra espaciadora para Resurrección y teclado numérico/flechas para la tienda). Esta versión KMP fue optimizada para pantallas táctiles y móviles sin perder la experiencia de escritorio:

1. **Aiming & Shooting Táctil**:
   - Al tocar y arrastrar en la pantalla, se calcula la trayectoria angular respecto a Norman y se proyecta una guía visual de disparo (retícula + puntos de trayectoria).
   - Al levantar el dedo (`onUp`), se lanza el proyectil.
2. **Botón de Resurrección Táctil**:
   - Botón ergonómico en la parte inferior con marco dinámico, tiempo de recarga visible en segundos e icono indicador de listo.
3. **Tienda Táctil Accesible**:
   - Cada ritual y mejora de la tienda se puede seleccionar pulsando directamente sobre su fila.
   - Botones táctiles interactivos para **[ COMPRAR ]** y **[ SIGUIENTE OLEADA ]**.
4. **Pausa y Diálogos**:
   - Botón táctil `||` en la esquina superior para pausar/reanudar.
   - Toque en cualquier parte para avanzar o saltar los diálogos de la historia.
5. **Resolución Virtual y Escalado**:
   - Resolución interna fija de `400 x 200` píxeles retro (`ScaleMode.SHOW_ALL`), adaptándose automáticamente a cualquier relación de aspecto de teléfono (16:9, 19.5:9, 21:9, tablets) y ventanas de escritorio con pixel-art nítido y sin distorsión.

---

## 🏛️ Arquitectura del Proyecto (`shared/src/commonMain`)

El código está organizado de manera modular y didáctica bajo el paquete `io.github.kevinah95.norman_the_necromancer`:

```
shared/src/commonMain/kotlin/io/github/kevinah95/norman_the_necromancer/
├── NormanGame.kt          # Punto de entrada común que inicializa KorGE y la escena
├── assets/
│   └── GameAtlas.kt       # Carga sprites y fuentes con Compose Resources (Res.readBytes) y parsea sprites.json
├── core/
│   ├── Tags.kt            # Bitmasks para colisiones y tipos (CORPSE, LIVING, SPELL, etc.)
│   ├── Geometry.kt        # Vectores 2D, Rectángulos, ángulos y utilidades matemáticas
│   ├── GameObject.kt      # Entidad base con física (x, y, vx, vy, masa, rebote, fricción)
│   ├── Behaviour.kt       # Componentes de comportamiento desacoplados (patrón Strategy/Component)
│   ├── Damage.kt          # Eventos de daño y muerte
│   ├── Ritual.kt          # Definición de mejoras pasivas y activas
│   ├── Tweens.kt          # Sistema de interpolación de movimiento y screenshake
│   └── Game.kt            # Bucle de física, colisiones y estado general de la partida
├── entities/
│   └── Entities.kt        # Fábricas de enemigos y aliados (Norman, Esqueletos, Reyes, Monjes, etc.)
├── behaviours/
│   └── Behaviours.kt      # Comportamientos: March, Attack, Damaging, Bleeding, Enraged, Seeking, etc.
├── rituals/
│   └── Rituals.kt         # Los 20 rituales del juego (Bouncing, Rain, Doubleshot, Electrodynamics, etc.)
├── levels/
│   └── Levels.kt          # Script de oleadas, enemigos progresivos, minibosses y boss final
├── shop/
│   └── Shop.kt            # Lógica de compra, generación procedural y precios con almas
├── fx/
│   ├── ParticleEmitter.kt # Simulación física de partículas (gravedad, fricción, rebote)
│   └── Fx.kt              # Efectos visuales preconfigurados (huesos, estelas, nubes, polvo)
├── audio/
│   └── GameAudio.kt       # Motor de síntesis procedural de audio y banda sonora adaptativa dinámica (Ambient Organ, Bass, Kick, King's Theme)
├── renderer/
│   └── GameRenderer.kt    # Renderizado optimizado con KorGE BatchBuilder2D y NineSlice
└── scene/
    └── NormanGameScene.kt # Escena principal de KorGE con el ciclo de vida y eventos táctiles/teclado
```

### 🎵 Banda Sonora Procedural Adaptativa (`sounds.ts` port)
El juego original de Dan Prince no utiliza archivos de audio externos (como .mp3 o .ogg), sino un sintetizador procedural en tiempo real mediante la Web Audio API. Esta versión KMP recrea con fidelidad matemática todos los instrumentos:
- **Órgano Ambiental:** Serie armónica de Fourier periódica `[-0.8, 1, 0.8, 0.8, -0.8, -0.8, -1]` con arpegio en notas A1 (55Hz) y A0 (27.5Hz), filtro paso alto a 200Hz y reverb de catedral.
- **Bajo Procedural:** Onda diente de sierra que interpreta una línea de bajo algorítmica en La Menor Armónica (A Harmonic Minor).
- **Sub-Kick:** Bombo sub-grave estilo 808 con barrido exponencial de tono (150Hz a 0Hz) y filtro paso bajo a 80Hz.
- **Tema del Rey (King's Theme):** Contrapunto gótico a 3 voces de órgano (soprano, tenor y pedal sub-grave) que se activa dinámicamente en la batalla final contra The King (Nivel 10 / script level 9).

**Progresión Dinámica:**
- **Nivel 1:** Arpegio de órgano ambiental misterioso.
- **Nivel 2:** Se suma el bajo rítmico.
- **Nivel 3+:** Entra el bombo completando la base a 240 BPM.
- **Tienda:** El bombo desaparece suavemente creando un ambiente relajado de compra.
- **Jefe Final (The King):** Los sintetizadores normales se apagan y entra la fuga gótica del Rey.
- **Fase 4 del Rey:** El bombo reingresa con fuerza para el combate final cuerpo a cuerpo.

### 📦 Gestión de Recursos Multiplataforma (`composeResources`)
Los recursos del juego (`sprites.png`, `sprites.json`, `font.png`, `font.json`) residen en:
`shared/src/commonMain/composeResources/files/`

Se cargan a través del estándar oficial de Compose Multiplatform:
```kotlin
val pngBytes = Res.readBytes("files/sprites.png")
bitmap = pngBytes.openAsync().readBitmap() // Decodificado por KorGE Image Format
val jsonStr = Res.readBytes("files/sprites.json").decodeToString()
```
Esto garantiza que los recursos se empaqueten automáticamente en el APK de Android, en el JAR de Desktop, en los bundles Web y en el framework de iOS sin depender de rutas del sistema de archivos local.

---

## ⚡ Atajos con Taskfile

Si tienes [Task](https://taskfile.dev/) instalado, puedes gestionar todas las plataformas con un solo comando:

```bash
task                  # Muestra la lista de tareas disponibles
task desktop          # Lanza el juego en Desktop (JVM)
task build-desktop    # Compila el JAR/ensamble de Desktop
task package-desktop  # Empaqueta la app nativa (.dmg en macOS)
task android          # Compila, instala y lanza el juego en Android
task build-android    # Compila el APK Debug de Android
task web-js           # Inicia el servidor de desarrollo Web en JavaScript
task build-web-js     # Compila el bundle de producción Web (JS Webpack)
task web-wasm         # Inicia el servidor de desarrollo Web en WasmJS
task build-web-wasm   # Compila el bundle de producción Web (WasmJS Webpack)
task ios              # Compila el framework de iOS para simulador arm64
task build-ios        # Compila los frameworks de iOS para simulador y dispositivo
task test             # Ejecuta las pruebas unitarias en la JVM
task build            # Compila todos los módulos (Desktop, Android, Web JS y Wasm)
task clean            # Limpia cachés y carpetas build
```

---

## 🚀 Ejecución directa con Gradle

### Desktop (JVM)
```bash
./gradlew :desktopApp:run
```

### Android
Conecta un teléfono o emulador Android:
```bash
./gradlew :androidApp:installDebug
```
O para compilar el APK:
```bash
./gradlew :androidApp:assembleDebug
```
Ubicación del APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

### Web (JavaScript y WasmJS)
Para ejecutar el servidor de desarrollo en navegador:
```bash
# Versión WasmJS (recomendada para navegadores modernos)
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# Versión JavaScript
./gradlew :webApp:jsBrowserDevelopmentRun
```

### iOS
Abre el proyecto [iosApp/iosApp.xcodeproj](./iosApp/iosApp.xcodeproj) en Xcode y selecciona tu simulador o dispositivo iPhone/iPad. También puedes compilar el framework directamente desde la terminal:
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### Pruebas Unitarias
```bash
./gradlew :shared:jvmTest
```