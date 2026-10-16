# Piano di Lavoro: Miglioramento Video Player, Sezioni Home, Fluidità UI e Thumbnail Episodi

## Obiettivo
Risolvere le criticità riscontrate nell'esperienza d'uso su Android TV:
1. **Interazione Video Player & Gestione Lifecycle (`ON_PAUSE`)**:
   - **Stop su `ON_PAUSE` / `ON_STOP`**: Quando l'utente preme Home sul telecomando, cambia app o spegne lo schermo/TV, la riproduzione audio e video deve interrompersi immediatamente e il progresso di visione deve essere salvato nel backend.
   - **Tasto BACK**: Se i controlli/progress bar sono visibili, nasconderli immediatamente senza togliere il focus a vuoto o uscire. Se sono nascosti, uscire salvando il progresso.
   - **Frecce Direzionali (DPAD Left/Right)**: Quando i controlli sono nascosti, avanzare/arretrare di 10 secondi mostrando un indicatore rapido temporaneo a scomparsa (2s).
   - **Tasti Hardware Play/Pausa**: Supportare tutti i codici telecomando (`MEDIA_PLAY_PAUSE`, `MEDIA_PLAY`, `MEDIA_PAUSE`, `MEDIA_STOP`, `HEADSETHOOK`) e integrare `MediaSession` di Media3.
2. **Bug Screensaver OLED**:
   - Mantenere attivo lo schermo durante la riproduzione (`FLAG_KEEP_SCREEN_ON`) e rilasciarlo solo in pausa/uscita.
   - Registrare `androidx.media3.session.MediaSession` con ExoPlayer per notificare il sistema operativo TV e HDMI-CEC dello stato di riproduzione, azzerando il timer screensaver alla ripresa.
3. **Sezioni Home (Tutti, Film, Serie TV, Anime)**:
   - Introdurre nella barra di navigazione superiore di `HomeScreen` i tab di categoria come su Home Assistant: `Tutti`, `Film`, `Serie TV`, `Anime`.
   - Popolare le sezioni con i rispettivi cataloghi e memorizzare i dati in cache in `HomeViewModel` per transizioni istantanee.
4. **Fluidità UI & Risoluzione Spostamento Contenuti**:
   - Rimuovere la sovrascrittura continua di `heroItem` in `HomeViewModel.onCardFocused()` che causava ricomposizioni continue e scatti.
   - Stabilizzare dimensioni e bordi delle card in `TvFocusModifier.kt` (spessore costante del bordo) evitando ricalcoli di layout (zero layout shift).
5. **Thumbnail degli Episodi delle Serie TV**:
   - Correggere `formatImageUrl`: impedire che i path relativi TMDB (es. `/abcde.jpg`) vengano erroneamente prefissati con l'URL del server locale invece che con `https://image.tmdb.org/t/p/w500/`.
   - Estendere `TvEpisodeDto` per supportare `still_path`, `still_url`, `thumbnail_url` e `image_url`.
   - Fornire un placeholder grafico curato in `TvEpisodeCard` in assenza di thumbnail.

---

## Scope & Non-Goals
- **In Scope**:
  - `app/src/main/kotlin/it/streaminghub/tv/player/VideoPlayerManager.kt`: Creazione e gestione del ciclo di vita di `MediaSession`, metodo `pausePlayback()`.
  - `app/src/main/kotlin/it/streaminghub/tv/ui/player/PlayerViewModel.kt`: Metodo `pausePlayback()` e hook di salvataggio progresso.
  - `app/src/main/kotlin/it/streaminghub/tv/ui/player/PlayerScreen.kt`:
    - `LifecycleEventObserver` per stoppare playback e salvare progresso su `ON_PAUSE` / `ON_STOP`.
    - Intercettazione tasto BACK con `onPreviewKeyEvent`.
    - Seek rapido con D-Pad Left/Right.
    - Supporto esteso tasti multimediali.
    - `FLAG_KEEP_SCREEN_ON` reattivo su Activity Window.
  - `app/src/main/kotlin/it/streaminghub/tv/data/model/Dtos.kt`: Fix `formatImageUrl` per path TMDB, supporto campi still/thumbnail in `TvEpisodeDto`.
  - `app/src/main/kotlin/it/streaminghub/tv/ui/components/TvEpisodeCard.kt`: Render thumbnail e fallback visuale robusto.
  - `app/src/main/kotlin/it/streaminghub/tv/ui/home/HomeScreen.kt`: Top Navigation Bar con tab Sezioni (`Tutti`, `Film`, `Serie TV`, `Anime`), eliminazione glitch autofocus.
  - `app/src/main/kotlin/it/streaminghub/tv/ui/home/HomeViewModel.kt`: Gestione stato sezioni, eliminazione ricomposizione incontrollata dell'Hero da card focus.
  - `app/src/main/kotlin/it/streaminghub/tv/ui/components/TvFocusModifier.kt`: Stabilizzazione border e clearance per eliminare layout shift.
- **Non-Goals (YAGNI)**:
  - Non riscrivere la pipeline HLS di ExoPlayer (già ottimizzata).
  - Non modificare il protocollo di sincronizzazione cronologia.

---

## Task Checklist
- [x] **Task 1: Correzione URL Immagini e DTO Episodi Serie TV**
  - In `Dtos.kt`, aggiornare `formatImageUrl` per distinguere i prefissi API interni (`/api/`, `/static/`, `/local/`, `/media/`, `/hls/`) dai path TMDB (`/xxx.jpg`).
  - Aggiungere a `TvEpisodeDto` le proprietà `@SerialName("still_path")`, `@SerialName("still_url")`, `@SerialName("thumbnail_url")`, `@SerialName("image_url")` e getter `effectivePosterUrl`.
  - In `TvEpisodeCard.kt`, rifinire il rendering per mostrare la thumbnail formattata o un elegante placeholder con gradiente in caso di assenza.
- [x] **Task 2: Risoluzione Screensaver OLED e Integrazione MediaSession**
  - In `VideoPlayerManager.kt`, istanziare `androidx.media3.session.MediaSession` collegata a `ExoPlayer` e rilasciarla in `release()`.
  - In `PlayerScreen.kt`, aggiungere `DisposableEffect(playerUiState.isPlaying)` per gestire `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` sulla Window dell'Activity.
- [x] **Task 3: Pattern di Usabilità Telecomando e Gestione Lifecycle (`ON_PAUSE`)**
  - In `PlayerScreen.kt`, aggiungere `LifecycleEventObserver` tramite `DisposableEffect` su `LocalLifecycleOwner.current`:
    - Su `ON_PAUSE` e `ON_STOP`: invocare `viewModel.pausePlayback()` e `viewModel.reportProgress()`.
  - Aggiungere `Modifier.onPreviewKeyEvent` sul contenitore principale di `PlayerScreen`:
    - Intercettare `KEYCODE_BACK`: se `showControls` è attivo o c'è un prompt binge, chiudere l'overlay e consumare l'evento (`true`), evitando l'unfocus di Compose.
    - Se `!showControls`:
      - `DPAD_LEFT` -> `viewModel.seekBack(10_000)` e attivazione indicatore seek temporaneo (2s).
      - `DPAD_RIGHT` -> `viewModel.seekForward(10_000)` e attivazione indicatore seek temporaneo (2s).
      - `DPAD_CENTER` / `ENTER` / `DPAD_UP` / `DPAD_DOWN` -> mostrare controlli completi con focus su Play/Pausa.
    - Gestire la serie completa di codici multimediali (`KEYCODE_MEDIA_PLAY_PAUSE`, `KEYCODE_MEDIA_PLAY`, `KEYCODE_MEDIA_PAUSE`, `KEYCODE_MEDIA_STOP`, `KEYCODE_HEADSETHOOK`).
- [x] **Task 4: Risoluzione Scatti e Layout Shifts (Interazioni Fluide)**
  - In `HomeViewModel.kt`, rimuovere l'aggiornamento forzato di `heroItem` da `onCardFocused()`.
  - In `HomeScreen.kt`, stabilizzare l'autofocus legandolo al solo caricamento iniziale e non ai cambi di hero.
  - In `TvFocusModifier.kt`, mantenere dimensioni e spessore bordo costanti variando il solo colore del bordo per azzerare i salti di layout.
- [x] **Task 5: Implementazione Sezioni Home (Tutti, Film, Serie TV, Anime)**
  - In `HomeViewModel.kt`, definire `HomeSection` (`ALL`, `MOVIES`, `SERIES`, `ANIME`) con caricamento e cache dedicati per sezione.
  - In `HomeScreen.kt`, integrare i tab nella barra superiore con navigazione D-Pad e indicatori attivi.
- [x] **Task 6: Compilazione, Verifica e Aggiornamento Handoff**
  - Verificare compilazione Kotlin (`./gradlew compileDebugKotlin --no-daemon`).
  - Verificare build APK (`./gradlew assembleDebug --no-daemon`).
  - Aggiornare `docs/HANDOFF.md` sovrascrivendolo.

---

## Strategia di Verifica
```bash
# Compilazione debug Kotlin
./gradlew compileDebugKotlin --no-daemon

# Assemblaggio APK completo
./gradlew assembleDebug --no-daemon
```
