# Handoff - Streaming Hub TV

## Stato Corrente
Completati e verificati con successo tutti i miglioramenti richiesti per Smart TV:
1. **Video Player Key Handling & Lifecycle (`ON_PAUSE`)**: Intercettazione tasto BACK in fase tunneling per chiudere i controlli OSD senza de-focalizzare la UI; seek rapido $\pm 10$s con DPAD Sinistra/Destra e badge HUD temporaneo (2s); supporto tasti hardware multimediali (`MEDIA_PLAY_PAUSE`, `PLAY`, `PAUSE`, `STOP`, `HEADSETHOOK`). Aggiunto `LifecycleEventObserver` per interrompere il playback e sincronizzare il progresso di visione quando l'app va in `ON_PAUSE` o `ON_STOP` (es. tasto Home, spegnimento TV, cambio app).
2. **Screensaver OLED & MediaSession**: Integrato `androidx.media3.session.MediaSession` con ExoPlayer per segnalare la riproduzione al sistema operativo e HDMI-CEC. Gestito `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` reattivo su Activity Window per prevenire lo screensaver OLED durante il playback.
3. **Sezioni Home (Tutti, Film, Serie TV, Anime)**: Aggiunta barra di navigazione con sezioni dedicate in `HomeScreen` e filtro catalogo con in-memory cache in `HomeViewModel`.
4. **Fluidità UI & Zero Layout Shift**: Rimosso il trigger continuo di `heroItem` al focus delle card (eliminando scatti e riposizionamenti) e stabilizzato lo spessore del bordo in `TvFocusModifier.kt`.
5. **Thumbnail Episodi Serie TV**: Corretta `formatImageUrl` per dirigere i path relativi TMDB a `image.tmdb.org/t/p/w500` anziché al backend locale, con supporto a `still_path`/`thumbnail_url` e fallback grafico.

## File Rilevanti
- `app/src/main/kotlin/it/streaminghub/tv/player/VideoPlayerManager.kt`: Creazione/rilascio `MediaSession` e metodo `pausePlayback()`.
- `app/src/main/kotlin/it/streaminghub/tv/ui/player/PlayerScreen.kt`: Intercettazione BACK, seek rapido $\pm 10$s, hardware keys, observer lifecycle `ON_PAUSE`/`ON_STOP` e flag `KEEP_SCREEN_ON`.
- `app/src/main/kotlin/it/streaminghub/tv/ui/player/PlayerViewModel.kt`: Metodo `pausePlayback()` e hook di progresso.
- `app/src/main/kotlin/it/streaminghub/tv/data/model/Dtos.kt`: Riconoscimento path TMDB in `formatImageUrl` ed estensione campi still/thumbnail su `TvEpisodeDto`.
- `app/src/main/kotlin/it/streaminghub/tv/ui/components/TvEpisodeCard.kt`: Binding `effectivePosterUrl` con fallback gradient.
- `app/src/main/kotlin/it/streaminghub/tv/ui/components/TvFocusModifier.kt`: Spessore bordo fisso per azzerare layout shift.
- `app/src/main/kotlin/it/streaminghub/tv/ui/home/HomeScreen.kt`: Tabs sezioni nella top bar con navigazione D-pad.
- `app/src/main/kotlin/it/streaminghub/tv/ui/home/HomeViewModel.kt`: Enum `HomeSection`, cache sezioni e disattivazione sovrascrittura continua di `heroItem`.
- `docs/plans/2026-10-16_improve_player_navigation_sections_and_ui.md`: Piano di lavoro dettagliato completato.

## Prossimi Passi (Next Steps)
- Test su Smart TV / Chromecast con Google TV fisica per validare il comportamento del telecomando e screensaver in ambiente reale.
- Eventuale commit git delle modifiche tracciate (`git add` e commit convenzionale).

## Comandi Utili
- **Compilazione Kotlin**: `./gradlew compileDebugKotlin --no-daemon`
- **Build APK Debug**: `./gradlew assembleDebug --no-daemon`
- **Installazione su TV via ADB**: `adb connect <IP_TV>:5555 && adb install -r app/build/outputs/apk/debug/app-debug.apk`
