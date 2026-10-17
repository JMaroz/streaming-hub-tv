# Handoff - Streaming Hub TV

## Stato Corrente
Risolti il bug di avanzamento a cascata dell'episodio successivo nelle serie TV e implementati gli aggiornamenti al resume:
1. **Fix Autoplay Cascade Serie TV**:
   - In `VideoPlayerManager`, aggiunto `resetForNextMedia()` che ferma immediatamente ExoPlayer (`stop()`, `clearMediaItems()`) e azzera `_uiState` (`currentPositionMs = 0`, `durationMs = 0`, `isEnded = false`, `isBuffering = true`).
   - In `PlayerViewModel`, tracciamento e cancellazione atomica dei job asincroni (`resolveJob`, `metadataJob`, `countdownJob`).
   - `startPositionObserver` condiziona `showNextPrompt` alla validità del flusso e cancella tempestivamente `countdownJob` non appena il prompt si nasconde.
   - `reportProgress` racchiuso in `withContext(NonCancellable)` per garantire il salvataggio dei progressi prima dell'uscita.
2. **Refresh Automatico al Ritorno dalla Riproduzione**:
   - `DetailsScreen` e `DetailsViewModel`: al lifecycle `ON_RESUME` ricaricano la progressione e l'episodio di ripresa aggiornato (`refreshProgressAndDetails()`).
   - `HomeScreen` e `HomeViewModel`: al lifecycle `ON_RESUME` aggiornano la riga "Continua a guardare" e i preferiti (`refreshContinueWatchingAndUserData()`).
3. **Reset allo Stato Iniziale al Resume dalla Home TV**:
   - In `AndroidManifest.xml`, configurato `launchMode="singleTask"`.
   - In `MainActivity`, intercettato `onNewIntent` per `Intent.CATEGORY_LEANBACK_LAUNCHER` e introdotto controllo di timeout inattività (15 min) in `onResume`: la navigazione ripristina lo stato iniziale (`NavRoutes.Home` con profilo attivo o `NavRoutes.Profiles`), rilasciando eventuali player in sospeso.
4. **Build & Validazione**: Compilato con successo sia APK Debug che Release (`./gradlew assembleRelease --no-daemon`).

## File Rilevanti
- `app/src/main/kotlin/it/streaminghub/tv/player/VideoPlayerManager.kt`: Aggiunto `resetForNextMedia()`.
- `app/src/main/kotlin/it/streaminghub/tv/ui/player/PlayerViewModel.kt`: Gestione job, cancellazione timer e reset pulito.
- `app/src/main/kotlin/it/streaminghub/tv/ui/details/DetailsViewModel.kt` & `DetailsScreen.kt`: Aggiornamento progressione al resume.
- `app/src/main/kotlin/it/streaminghub/tv/ui/home/HomeViewModel.kt` & `HomeScreen.kt`: Aggiornamento "Continua a guardare" al resume.
- `app/src/main/kotlin/it/streaminghub/tv/MainActivity.kt` & `AndroidManifest.xml`: Reset a root/Home su resume da launcher TV.
- `docs/plans/2026-10-17_fix_next_episode_autoplay_cascade.md`: Piano di lavoro dettagliato.

## Prossimi Passi (Next Steps)
- Test su Smart TV Philips con telecomando fisico per validare la sequenza di transizione dell'episodio successivo e il ritorno dalla Home TV.
- Commit Git convenzionale (`fix(player): resolve autoplay cascade, refresh on return and reset on TV home resume`).

## Comandi Utili
- **Build APK Release**: `./gradlew assembleRelease --no-daemon`
- **Connessione ADB TV**: `adb connect 192.168.1.49:5555`
- **Installazione Release**: `adb -s 192.168.1.49:5555 install -r app/build/outputs/apk/release/app-release.apk`
- **Avvio Applicazione**: `adb -s 192.168.1.49:5555 shell am start -n it.streaminghub.tv/.MainActivity`
