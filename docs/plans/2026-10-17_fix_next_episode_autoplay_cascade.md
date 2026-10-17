# Piano di Lavoro: Fix Cascata Autoplay Episodio Successivo & Aggiornamenti Resume/Backstack

Data: 2026-10-17
Task: Fix del bug di skip a cascata dell'episodio successivo, refresh automatico dopo riproduzione e reset app al resume da Home TV

## Obiettivo
1. Eliminare la transizione prematura a cascata al secondo episodio successivo e l'apparizione spuria del relativo banner quando l'utente conferma la visione dell'episodio successivo.
2. Aggiornare automaticamente la schermata che ha avviato la riproduzione (Home con riga "Continua a guardare" e prossimo episodio, Scheda Dettagli con progressione, stato watched e prossimo episodio) al ritorno dal Player.
3. Al resume dalla Home della TV (Leanback Launcher o inattività prolungata), ripristinare l'app allo stato iniziale (Home / Profili), allineandosi al comportamento standard delle app TV (Netflix, Prime Video).

## Causa Radice & Analisi Architetturale
1. **Autoplay Cascade**:
   - `playNextEpisode()` non azzerava `PlayerUiState` né fermava ExoPlayer prima della risoluzione asincrona del nuovo stream.
   - `loadSkipSegmentsAndNextEpisode()` prefetchava immediatamente il terzo episodio; trovando `isEnded == true` dal vecchio video, `positionObserver` scatenava il countdown di 12s per il terzo episodio.
   - `countdownJob` non veniva cancellato quando `showNextPrompt` tornava a `false`.
2. **Refresh al ritorno da riproduzione**:
   - `DetailsScreen` e `HomeScreen` caricavano i dati solo alla prima composizione / init dei rispettivi ViewModel. Mancava l'ascolto del ciclo di vita `ON_RESUME` per ricaricare la progressione e la shelf "Continua a guardare" dopo il `popBackStack` dal player.
   - `reportProgress()` nel player non garantiva il completamento con `NonCancellable` in caso di deallocazione immediata.
3. **Reset al resume da Home TV**:
   - Al rilancio dal launcher di Android TV (`Intent.CATEGORY_LEANBACK_LAUNCHER`), `MainActivity` non intercettava `onNewIntent` per resettare il backstack di Compose alla root/Home.
   - Mancava la gestione del timeout di standby (15 minuti).

## Scope & Non-Goals
- **In Scope**:
  - `VideoPlayerManager`: metodo `resetForNextMedia()`.
  - `PlayerViewModel`: gestione sicura dei Job (`resolveJob`, `metadataJob`, `countdownJob`), reset stato e `NonCancellable` in `reportProgress()`.
  - `DetailsViewModel` & `DetailsScreen`: metodo `refreshProgressAndDetails()` e trigger `ON_RESUME`.
  - `HomeViewModel` & `HomeScreen`: metodo `refreshContinueWatchingAndUserData()` e trigger `ON_RESUME`.
  - `MainActivity` & `AndroidManifest.xml`: `singleTask`, intercettazione `onNewIntent` con `CATEGORY_LEANBACK_LAUNCHER`, timeout 15 minuti su `onResume` e reset della navigazione a `NavRoutes.Home.route`.
- **Non-Goals**:
  - Riscrittura totale della navigazione o modifiche all'interfaccia utente grafica.

## Task Checklist
- [x] 1. Estendere `VideoPlayerManager` con `resetForNextMedia()`.
- [x] 2. Aggiornare `PlayerViewModel`:
  - [x] 2.1 Memorizzare e cancellare i Job asincroni precedenti.
  - [x] 2.2 Reset immediato in `playNextEpisode()`.
  - [x] 2.3 Cancellazione di `countdownJob` quando `showNextPrompt` diventa falso.
  - [x] 2.4 Condizionare `showNextPrompt` alla validità del video attuale.
  - [ ] 2.5 Aggiungere `NonCancellable` a `reportProgress()`.
- [ ] 3. Refresh pagina al ritorno dalla riproduzione:
  - [ ] 3.1 Aggiungere `refreshProgressAndDetails()` a `DetailsViewModel` e trigger in `DetailsScreen` su `ON_RESUME`.
  - [ ] 3.2 Aggiungere `refreshContinueWatchingAndUserData()` a `HomeViewModel` e trigger in `HomeScreen` su `ON_RESUME`.
- [ ] 4. Reset stato iniziale al resume da Home TV:
  - [ ] 4.1 Configurare `singleTask` in `AndroidManifest.xml`.
  - [ ] 4.2 In `MainActivity`, gestire `onNewIntent` per `LEANBACK_LAUNCHER` e timeout inattività su `onResume`, pilotando il reset a `NavRoutes.Home`.
- [ ] 5. Verifica della compilazione con `./gradlew assembleDebug --no-daemon`.
- [ ] 6. Aggiornare `docs/HANDOFF.md`.

## Strategia di Verifica
- Compilazione Gradle con successo (`./gradlew assembleDebug --no-daemon`).
- Verifica log e conformità del codice.
