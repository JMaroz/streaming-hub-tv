# Handoff - Streaming Hub TV

## Stato Corrente
Ottimizzata la gestione del buffer e delle operazioni di seek (avanti e indietro) in ExoPlayer (`VideoPlayerManager.kt`):
1. **Back-Buffer Attivo (`setBackBuffer`)**:
   - Abilitato `backBufferDurationMs = 40_000` con `retainBackBufferFromKeyframe = true`. I precedenti 40 secondi di riproduzione rimangono in memoria RAM, rendendo il salto all'indietro (-10s / rewind) istantaneo ed eliminando i continui stalli e l'overlay di buffering.
2. **Buffer in Avanti Esteso & Re-buffer Veloce**:
   - `minBufferMs = 30_000` e `maxBufferMs = 90_000` per garantire fino a 90 secondi di pre-caricamento durante la riproduzione continua.
   - `bufferForPlaybackAfterRebufferMs = 2_000` (ridotto da 5.000ms) e `bufferForPlaybackMs = 1_500` (da 2.500ms) per una ripresa immediata della riproduzione in caso di cache miss.
   - `setPrioritizeTimeOverSizeThresholds(true)` per mantenere la soglia temporale anche con stream HLS ad alto bitrate.
3. **Seek Parameters Ottimizzati**:
   - Impostato `SeekParameters.CLOSEST_SYNC` per agganciare istantaneamente il keyframe più vicino evitando cicli di decodifica intempestivi su frame intermedi.
4. **Verifica Empirica**:
   - Compilazione Kotlin completata con successo: `./gradlew compileDebugKotlin` (BUILD SUCCESSFUL in 30s).

## File Rilevanti
- `app/src/main/kotlin/it/streaminghub/tv/player/VideoPlayerManager.kt`: Configurazione `loadControl` (back-buffer 40s, forward buffer 90s) e `SeekParameters.CLOSEST_SYNC`.
- `docs/plans/2026-10-18_optimize_player_buffer_and_seek.md`: Piano di lavoro dettagliato del task.

## Prossimi Passi (Next Steps)
- Effettuare test su Android TV / Google TV reale tramite telecomando D-pad verificando la fluidità di rewind (-10s) e fast-forward (+10s).
- Generare l'APK aggiornato se necessario per il deploy (`./gradlew assembleDebug` o `assembleRelease`).

## Comandi Utili
- **Compilazione Kotlin**: `./gradlew compileDebugKotlin`
- **Build APK Release**: `./gradlew assembleRelease --no-daemon`
- **Installazione ADB TV**: `adb -s 192.168.1.xxx:5555 install -r app/build/outputs/apk/release/app-release.apk`
