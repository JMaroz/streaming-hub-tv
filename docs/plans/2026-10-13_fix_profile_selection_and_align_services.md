# Piano di Lavoro: Risoluzione Selezione Profilo Android TV e Allineamento Servizi Streaming Hub

## Obiettivo
Ripristinare la funzionalità completa dell'app Android TV (`streaming-hub-tv`) risolvendo il problema per cui cliccando su un profilo non accade nulla o l'app si blocca/crasha, ed allineare l'applicazione con tutte le ultime novità introdotte nel backend madre Home Assistant (`streaming-hub-ha` v3.0.2: caroselli dinamici estesi a 23 slider, supporto sorgente anime, filtri parental control raffinati per profili Kids).

## Root Causes Identificate
1. **Remote Control D-Pad Center Key (`KEYCODE_DPAD_CENTER`, 23) non intercettato in `tvFocusable`**:
   - `tvFocusable` applicava sia `.focusable()` che `.clickable()` sulla stessa catena di modificatori. In Jetpack Compose, `.clickable()` è già focusabile. Avere `.focusable()` prima di `.clickable()` crea due `FocusTargetNode`: il primo assorbe il focus, ma `clickable` gestisce `KEYCODE_DPAD_CENTER` solo se il suo nodo interno possiede il focus. Di conseguenza, la pressione del tasto centrale "OK" del telecomando fisico veniva completamente ignorata.
2. **Crash Fatale per Incompatibilità Binaria (`NoSuchMethodError: LazyLayoutPrefetchState.schedulePrefetch`)**:
   - In `app/build.gradle.kts`, era forzata la dipendenza obsoleta `androidx.tv:tv-foundation:1.0.0-alpha11` su un Compose BOM recente (`2026.09.00` con Foundation 1.12.1).
   - Non appena veniva selezionato un profilo e la navigazione raggiungeva `HomeScreen` con il catalogo popolato (23 caroselli tematici), `TvLazyColumn` tentava il prefetch o scroll invocando `schedulePrefetch-0kLqBqw(...)`, inesistente nel runtime moderno. L'app crashava all'istante con `NoSuchMethodError` e il processo veniva terminato da Android, facendo apparire il click come un "non fare nulla" o tornando al launcher.
3. **Recomposition Storm e Reset Navigazione via DataStore**:
   - In `AppPreferences.kt`, i Flow come `serverUrl` non usavano `.distinctUntilChanged()`. Alla scrittura di `setActiveProfile()`, la nuova emissione di DataStore causava la ricomposizione di `MainActivity`, rischiando di resettare il controller o causare instabilità nella navigazione.

## Scope & Non-Goals
- **In Scope**:
  - Correggere `TvFocusModifier.kt` con gestione esplicita degli eventi tasto D-Pad (`KEYCODE_DPAD_CENTER`, `KEYCODE_ENTER`, `KEYCODE_NUMPAD_ENTER`) e rimozione del doppio focus target.
  - Rimuovere la forzatura `force("androidx.tv:tv-foundation:1.0.0-alpha11")` da `app/build.gradle.kts`.
  - Migrare tutti i layout lazy da `androidx.tv.foundation.lazy.*` ai componenti nativi e stabili di Compose Foundation (`LazyColumn`, `LazyRow`, `LazyVerticalGrid`) in:
    - `HomeScreen.kt`
    - `TvShelfRow.kt`
    - `ProfilePickerScreen.kt`
    - `DetailsScreen.kt`
    - `SearchScreen.kt`
    - `PlayerScreen.kt`
  - Aggiungere `.distinctUntilChanged()` ai Flow in `AppPreferences.kt` e consolidare `MainActivity.kt`.
  - Allineare la risoluzione flussi (`PlayerViewModel.kt`) con `preferFhd` dalle preferenze utente.
  - Verificare la corretta visualizzazione e navigazione dei 23 caroselli editoriali con profilo standard e profilo Kids (es. `Leandro`, rating `6+`).
- **Non-Goals (YAGNI)**:
  - Non riscrivere l'architettura di navigazione o sostituire Navigation Compose.
  - Non modificare i contratti API esistenti nel backend `streaming-hub-ha`.
  - Non alterare l'ExoPlayer HLS pipeline già funzionante.

## Task Checklist
- [ ] **Task 1: Fix Input Telecomando & Focus in `TvFocusModifier.kt`**
  - Implementare `.onKeyEvent` per intercettare affidabilmente `ACTION_UP` con `KEYCODE_DPAD_CENTER`, `KEYCODE_ENTER` e `KEYCODE_NUMPAD_ENTER`.
  - Separare i rami `onClick != null` (solo `.clickable`) e `onClick == null` (solo `.focusable`).
- [ ] **Task 2: Migrazione a Standard Compose Lazy Layouts & Rimozione Alpha11**
  - Rimuovere `resolutionStrategy { force("androidx.tv:tv-foundation:1.0.0-alpha11") }` da `app/build.gradle.kts`.
  - Migrare `TvLazyColumn` e `TvLazyRow` in `HomeScreen.kt`, `TvShelfRow.kt`, `ProfilePickerScreen.kt`, `DetailsScreen.kt`, `PlayerScreen.kt` a `LazyColumn` e `LazyRow`.
  - Migrare `TvLazyVerticalGrid` in `SearchScreen.kt` a `LazyVerticalGrid`.
- [ ] **Task 3: Stabilizzazione Flussi Reattivi DataStore & MainActivity**
  - Aggiungere `.distinctUntilChanged()` su `serverUrl`, `activeProfileId`, `activeProfileName`, `activeProfileAvatar`, `preferFhd` in `AppPreferences.kt`.
  - Consolidare `rememberNavController` e `startRoute` in `MainActivity.kt`.
- [ ] **Task 4: Allineamento Preferenze Player & Servizi con HA v3.0.2**
  - In `PlayerViewModel.kt`, passare `preferences.preferFhd` dinamico nella chiamata `resolveStream`.
- [ ] **Task 5: Verifica Empirica su Android TV Reale**
  - Compilare con `./gradlew assembleDebug`.
  - Installare APK su Android TV (`192.168.1.49:5555`).
  - Verificare click telecomando con `input keyevent 23` e navigazione sui profili Andrea e Leandro.
  - Catturare screenshot ed esaminare i logcat per confermare zero crash.
- [ ] **Task 6: Aggiornamento Documentazione Handoff**
  - Sovrascrivere `docs/HANDOFF.md` con l'esito finale sintetico.

## Strategia di Verifica
```bash
# Compilazione ed assemblaggio APK debug
./gradlew assembleDebug

# Installazione su dispositivo Android TV
adb -s 192.168.1.49:5555 install -r app/build/outputs/apk/debug/app-debug.apk

# Test apertura app e click profilo con D-pad center
adb -s 192.168.1.49:5555 shell am start -n it.streaminghub.tv/.MainActivity
adb -s 192.168.1.49:5555 shell input keyevent 23

# Verifica logcat per assenza di NoSuchMethodError e crash
adb -s 192.168.1.49:5555 logcat -d | grep -E "FATAL|AndroidRuntime|it.streaminghub.tv"

# Verifica rendering HomeScreen con screenshot
adb -s 192.168.1.49:5555 shell screencap -p /sdcard/verify.png && adb -s 192.168.1.49:5555 pull /sdcard/verify.png /tmp/verify.png
```
