# Piano di Lavoro: Ottimizzazione Buffer e Seek ExoPlayer (Streaming Hub TV)

## Obiettivo
Eliminare i continui blocchi di buffering e l'overlay di caricamento durante le operazioni di seek forward (+10s) e soprattutto seek backward (-10s) nell'applicazione Android TV `streaming-hub-tv`, configurando un buffer all'indietro dedicato (back-buffer), estendendo la finestra di pre-buffering in avanti e ottimizzando la ripresa post-rebuffer.

## Scope & Non-Goals
- **In Scope**:
  - Configurazione `setBackBuffer` in `DefaultLoadControl` con mantenimento dei keyframe (`retainBackBufferFromKeyframe = true`).
  - Ottimizzazione parametri di buffering `minBufferMs`, `maxBufferMs`, `bufferForPlaybackMs`, `bufferForPlaybackAfterRebufferMs`.
  - Configurazione di `SeekParameters.CLOSEST_SYNC` su `ExoPlayer` per evitare loop di decodifica intempestivi su frame intermedi durante i salti telecomando.
  - Verifica di compilazione Kotlin con `./gradlew compileDebugKotlin`.
- **Non-Goals**:
  - Nessuna modifica alle API di rete o ai protocolli DTO dell'add-on backend.
  - Nessuna riscrittura dell'OSD del player.

## Task Checklist
- [x] Creare piano di lavoro in `docs/plans/2026-10-18_optimize_player_buffer_and_seek.md`
- [x] Aggiornare `VideoPlayerManager.kt` con `setBackBuffer`, parametri di buffer estesi e `SeekParameters.CLOSEST_SYNC`
- [x] Verificare la compilazione Kotlin con `./gradlew compileDebugKotlin`
- [x] Aggiornare `docs/HANDOFF.md` sovrascrivendolo integralmente
- [x] Eseguire commit Git con convenzione Conventional Commits

## Strategia di Verifica
- Eseguire `/Users/andrea/Repository/streaming-hub-tv/gradlew -p /Users/andrea/Repository/streaming-hub-tv compileDebugKotlin` e verificare esito `BUILD SUCCESSFUL`.
- Verificare assenza di warning o regressioni sui tipi e le API Media3.
