# Handoff - Streaming Hub TV

## Stato Corrente
Completata con successo la risoluzione delle criticità richieste relative all'interfaccia TV:
1. **Loading in Home**: Implementato `TvLoadingSpinner` in `HomeScreen.kt` che viene mostrato immediatamente dopo la selezione di un profilo (o cambio profilo) mentre il catalogo viene caricato in parallelo.
2. **Autofocus su Inizia / Riprendi**: Configurato `FocusRequester` collegato al pulsante principale in `DetailsScreen.kt`, con attivazione automatica all'atterraggio sulla scheda dettagli non appena l'elemento viene popolato.
3. **Info Episodio per Serie TV**: La label del pulsante principale mostra esplicitamente l'episodio da riprendere (`Riprendi S{s}:E{e} (mm:ss)` o `Riprendi S{s}:E{e}` o `Inizia S1:E1`) recuperando l'informazione da `continueWatching` e dalla cronologia `watchedHistory`.
4. **Rimozione Aggiorna & Auto-refresh 12 ore**: Rimosso il pulsante manuale "Aggiorna" e introdotto in `DetailsViewModel` il controllo sui timestamp salvati in `AppPreferences`: se sono trascorse più di 12 ore dall'ultimo aggiornamento, i metadati vengono aggiornati automaticamente dal backend all'entrata nella scheda.

## File Rilevanti
- `app/src/main/kotlin/it/streaminghub/tv/ui/components/TvLoadingSpinner.kt`: Nuovo componente riutilizzabile per spinner animato nativo per TV (Zero dipendenze esterne, Canvas nativo).
- `app/src/main/kotlin/it/streaminghub/tv/ui/home/HomeScreen.kt`: Integrazione dello spinner di caricamento per profilo attivo e stato di errore con pulsante di riprova.
- `app/src/main/kotlin/it/streaminghub/tv/ui/home/HomeViewModel.kt`: Reset immediato di hero e carousels al cambio profilo per evitare visualizzazione di contenuti non pertinenti durante il caricamento.
- `app/src/main/kotlin/it/streaminghub/tv/data/local/AppPreferences.kt`: Metodi `getLastMetadataUpdateTime(titleId)` e `setLastMetadataUpdateTime(titleId, timestamp)` via DataStore Preferences.
- `app/src/main/kotlin/it/streaminghub/tv/ui/details/DetailsViewModel.kt`: Calcolo automatico della scadenza delle 12 ore per forzare `refresh = true`, estrazione robusta dell'episodio di ripresa da `continueWatching` e `watchedHistory`, selezione automatica della stagione corretta.
- `app/src/main/kotlin/it/streaminghub/tv/ui/details/DetailsScreen.kt`: Autofocus con `FocusRequester` sul pulsante principale, etichettatura arricchita con S{s}:E{e}, rimozione pulsante "Aggiorna", fallback di loading visivo mentre i dati del titolo sono in caricamento.
- `docs/plans/2026-10-10_fix_tv_home_details_improvements.md`: Piano di lavoro tracciato.

## Prossimi Passi (Next Steps)
- Test su dispositivo Android TV / Google TV reale o emulatore con telecomando fisico D-pad per verificare l'esperienza tattile del focus.
- Eventuale estensione della cronologia per mostrare una sezione dedicata in UI se desiderata.

## Comandi Utili
- **Compilazione Kotlin**: `./gradlew compileDebugKotlin`
- **Generazione APK Debug**: `./gradlew assembleDebug`
- **Installazione su TV via ADB**: `adb connect <IP_TV>:5555 && adb install -r app/build/outputs/apk/debug/app-debug.apk`
