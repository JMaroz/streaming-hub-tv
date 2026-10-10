# Piano di Lavoro: Risoluzione Criticità Home e Scheda Dettagli TV

## Obiettivo
Risolvere le problematiche segnalate dall'utente relative all'esperienza TV:
1. Aggiungere un indicatore di loading visivo quando si atterra in Home dopo la selezione del profilo.
2. Portare il focus iniziale sul pulsante principale "Inizia / Riprendi" all'apertura della scheda dettagli.
3. Arricchire l'etichetta del pulsante principale con l'informazione dell'episodio da riprendere (e prossimo episodio) per le serie TV.
4. Rimuovere il pulsante manuale "Aggiorna" e aggiornare automaticamente i metadati all'entrata nella scheda se sono trascorse più di 12 ore dall'ultimo aggiornamento.

## Scope & Non-Goals
- **In Scope**:
  - `HomeScreen.kt` e `HomeViewModel.kt`: gestione dello stato `isLoading` con indicatore animato nativo.
  - `DetailsScreen.kt`: autofocus su pulsante principale tramite `FocusRequester`, rimozione pulsante "Aggiorna", formattazione etichetta episodio per serie TV.
  - `DetailsViewModel.kt`: logica di resume arricchita (stagione, episodio, titolo, fallback da history) e auto-refresh metadati se > 12 ore.
  - `AppPreferences.kt`: memorizzazione e recupero del timestamp di aggiornamento metadati per titolo.
- **Non-Goals (YAGNI)**:
  - Nessuna dipendenza esterna aggiuntiva (uso di Canvas / Jetpack Compose nativo per lo spinner).
  - Nessun refactoring dell'architettura di navigazione o dei componenti non correlati.

## Task Checklist
- [x] Creare il piano in `docs/plans/2026-10-10_fix_tv_home_details_improvements.md`
- [x] Implementare `getLastMetadataUpdateTime` e `setLastMetadataUpdateTime` in `AppPreferences.kt`
- [x] Aggiornare `HomeViewModel.kt` e `HomeScreen.kt` per mostrare un loading chiaro al cambio/selezione profilo
- [x] Aggiornare `DetailsViewModel.kt` per:
  - Identificare correttamente l'episodio da riprendere (anche con progresso <= 10s o da cronologia)
  - Eseguire auto-refresh se trascorsi > 12 ore dall'ultimo aggiornamento
- [x] Aggiornare `DetailsScreen.kt` per:
  - Focalizzare automaticamente il pulsante "Inizia / Riprendi" all'apertura della scheda
  - Mostrare l'etichetta con l'episodio specifico per le serie TV ("Riprendi S{x}:E{y}" / "Inizia S1:E1")
  - Rimuovere il pulsante superfluo "Aggiorna"
- [x] Verificare la compilazione con `./gradlew compileDebugKotlin` e `./gradlew assembleDebug`
- [x] Aggiornare `docs/HANDOFF.md`

## Strategia di Verifica
- Esecuzione di `./gradlew compileDebugKotlin` per validare la correttezza del codice Kotlin e di Compose.
- Verifica del corretto binding dei FocusRequester e dello stato UI reattivo.
