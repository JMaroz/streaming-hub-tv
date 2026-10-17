# Piano di Lavoro: Risoluzione Focus Iniziale e Visibilità Completa Hero Banner nella Home TV

## Obiettivo
Risolvere il problema per cui, all'avvio dell'app e all'ingresso nella Home di Android TV, il focus si posiziona automaticamente su "Guarda Ora" dell'Hero Banner facendo scorrere la schermata verso il basso, tagliando fuori la barra di navigazione superiore e la parte alta del banner (viso dell'attore, cielo e titoli) e mostrando eccessivamente la sezione sottostante anziché l'Hero Banner completo.

## Root Causes Identificate
1. **TopTvNavigationBar annidata dentro LazyColumn come `item`**:
   - `TopTvNavigationBar` era posizionata come `item` zero dentro `LazyColumn`. Quando qualsiasi elemento successivo (es. i pulsanti di `HeroBillboard`) richiede o riceve il focus, il sistema di scorrimento di Compose tenta di allineare l'elemento target o scrolla l'item 0 fuori dallo schermo verso l'alto.
   - Di conseguenza, la barra di navigazione con il brand, le sezioni (Tutti, Film, Serie TV, Anime) e le icone di ricerca/profilo/impostazioni scompare completamente dalla vista.
2. **FocusRequester forzato con LaunchedEffect su "Guarda Ora"**:
   - In `HomeScreen.kt`, un `LaunchedEffect(uiState.isLoading)` chiamava `heroPlayFocusRequester.requestFocus()` con un delay di 200ms.
   - Questa richiesta programmatica forzata di focus innescava `bringIntoView`, provocando lo scorrimento verso il basso dell'intera schermata per centrare il pulsante "Guarda Ora", tagliando la sommità dell'Hero Banner e nascondendo la top bar.
3. **Mancanza di ancoraggio fisso per l'Header di navigazione**:
   - In un'interfaccia Smart TV, la barra di navigazione orizzontale superiore e l'header delle sezioni devono fungere da ancora visiva fissa (`Column` superiore o sticky header) sopra l'area di scorrimento del catalogo, permettendo all'Hero Banner di essere visibile per intero a scroll zero insieme alla top bar.

## Scope & Non-Goals
- **In Scope**:
  - Spostare `TopTvNavigationBar` fuori dal `LazyColumn`, collocandola in cima al layout in un `Column` dedicato con sfondo scuro coerente `BgPrimary`.
  - Rimuovere la duplicazione di `TopTvNavigationBar` nei rami `isLoading` ed `errorMessage`, unificandola come header persistente.
  - Correggere il focus iniziale: indirizzare il focus iniziale sul tab della sezione attiva nella Top Navigation Bar ("Tutti"), mantenendo lo scroll della `LazyColumn` rigorosamente a `firstVisibleItemScrollOffset = 0` senza scorrimenti indesiderati.
  - Garantire navigazione D-pad fluida e bidirezionale tra Top Navigation Bar, Hero Banner ("Guarda Ora", "Dettagli", "Aggiungi") e le righe del catalogo ("Continua a guardare", "I Tuoi Preferiti", ecc.).
  - Verifica empirica con compilazione APK Release (`./gradlew assembleRelease --no-daemon`) e installazione.
- **Non-Goals (YAGNI)**:
  - Non modificare i modelli dati o il `HomeViewModel`.
  - Non modificare la logica di filtering delle sezioni.
  - Non alterare i componenti `TvShelfRow` o `HeroBillboard`.

## Task Checklist
- [x] **Task 1: Unificazione e ancoraggio di `TopTvNavigationBar` in cima a `HomeScreen`**
  - Posizionata `TopTvNavigationBar` in cima a `HomeScreen` fuori dal `LazyColumn`.
  - Rimosse le definizioni duplicate nei rami di caricamento ed errore.
- [x] **Task 2: Correzione Focus Iniziale e azzeramento scroll forzato**
  - Rimosso il `LaunchedEffect` che forzava `heroPlayFocusRequester.requestFocus()`.
  - Impostato il focus iniziale sul tab della sezione attiva nella Top Navigation Bar (`topBarFocusRequester`).
  - Assicurato `listState.scrollToItem(0, 0)` all'apertura e al cambio sezione.
- [x] **Task 3: Compilazione & Deploy su TV Philips**
  - Compilato con successo APK Release con `./gradlew assembleRelease --no-daemon` (R8 + ProGuard + signing release).
  - Installato con successo via ADB su Smart TV Philips (`Success`).
- [x] **Task 4: Validazione architettura layout TV**
  - Struttura a colonna con barra di navigazione fissa, Hero Billboard a inizio `LazyColumn` a scroll 0 e navigazione D-pad naturale.
- [x] **Task 5: Aggiornamento `docs/HANDOFF.md`**
  - Aggiornato sinteticamente lo stato secondo le regole Ponytail.

## Strategia di Verifica
```bash
./gradlew assembleRelease --no-daemon
adb -s 192.168.1.49:5555 install -r app/build/outputs/apk/release/app-release.apk
adb -s 192.168.1.49:5555 shell am start -n it.streaminghub.tv/.MainActivity
```
