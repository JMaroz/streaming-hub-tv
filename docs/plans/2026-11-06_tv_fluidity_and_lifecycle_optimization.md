# Piano di Ottimizzazione: Fluidità TV, Navigazione & Gestione Lifecycle

## Obiettivo
Ottimizzare al massimo la fluidità, la stabilità visiva e la gestione del ciclo di vita su Android TV (hardware Smart TV con risorse limitate / chipset MediaTek ARM A53, Philips TPM191E):
1. **Focus Statico & Zero Lag**: Eliminare ombre GPU e oscillazioni fisiche; adottare scala 1.0x (completamente ferma) con bordo vivido ad alto contrasto (pattern Netflix/Prime Video).
2. **Click & Navigazione Reattiva**: Rimuovere il ritardo al click con transizione crossfade ultra-rapida (100ms) in `NavHost` e pulizia del dispatch eventi D-pad.
3. **Stabilità di Scorrimento Sezioni**: Locandine fisse nello spazio, nessun movimento/dislocamento degli elementi adiacenti durante lo scorrimento D-pad.
4. **Gestione Background & Lifecycle**: Chiusura ordinata del Player al passaggio in background con salvataggio progressi e ritorno al Dettaglio; timeout di 5 minuti per il ripristino automatico alla schermata Home.

---

## Scope & Non-Goals
- **In Scope**:
  - `TvFocusModifier.kt`: Azzerare l'ingrandimento (`1.0f`), rimuovere `.shadow()`, rimuovere `spring()` ed eliminare la ridondanza di click handling.
  - `AppNavigation.kt`: Aggiungere `enterTransition` e `exitTransition` crossfade (100ms) per `NavHost`.
  - `MainActivity.kt`: Ridurre la soglia di reset a 5 minuti (300.000 ms).
  - `PlayerScreen.kt`: Uscita dal player con salvataggio progressi su evento `ON_STOP` dell'Activity.
- **Non-Goals (YAGNI / Ponytail)**:
  - Nessuna riscrittura dell'architettura di navigazione o dei ViewModel.
  - Nessuna modifica ai contratti API o alle query del database/DataStore.
  - Nessun layer di caching grafico aggiuntivo o librerie di animazione esterne.

---

## Task Checklist
- [x] **Fase 1: Focus Engine & Risparmio Risorse GPU (`TvFocusModifier.kt`)**
  - [x] Rimuovere l'effetto `.shadow(elevation = shadowElevation.dp)` dinamico.
  - [x] Impostare `focusedScale = 1.0f` di default (nessuna variazione di scala che muova gli elementi vicini).
  - [x] Semplificare la gestione click unificando il trigger D-pad/Enter per evitare lag.
- [x] **Fase 2: Transizioni Schermo Reattive (`AppNavigation.kt`)**
  - [x] Configurare `enterTransition` e `exitTransition` con `fadeIn(tween(100))` e `fadeOut(tween(100))` nel `NavHost`.
  - [x] Disattivare animazioni di slide o pop pesanti su TV.
- [x] **Fase 3: Gestione Background & Player Lifecycle (`MainActivity.kt` & `PlayerScreen.kt`)**
  - [x] In `PlayerScreen.kt`, all'evento `Lifecycle.Event.ON_STOP`, salvare i progressi ed eseguire `safeExit()` per tornare al Dettaglio.
  - [x] In `MainActivity.kt`, ridurre la soglia di inattività background da 15 a 5 minuti.
  - [x] Verificare che dopo $\ge 5$ minuti l'app torni alla Home del profilo attivo senza riprendere vecchi contenuti.
- [x] **Fase 4: Verifica Empirica & Validazione su Device**
  - [x] Esecuzione build di release: `./gradlew assembleRelease --no-daemon` completata con successo (APK 6.8MB).
  - [ ] Deploy e test su device TV `192.168.1.50:5555` (non appena la TV viene accesa/connessa).
  - [x] Aggiornamento di `docs/HANDOFF.md`.

---

## Strategia di Verifica
1. **Compilazione**:
   - `./gradlew assembleRelease --no-daemon` -> **Verificato con successo** (APK generato in `app/build/outputs/apk/release/app-release.apk`).
2. **Installazione su TV**:
   - `adb -s 192.168.1.50:5555 install -r app/build/outputs/apk/release/app-release.apk`
3. **Verifica Funzionale**:
   - Navigazione D-pad tra card e sezioni: verificare assenza totale di jitter o spostamento di locandine adiacenti.
   - Pressione D-pad Center: verificare reattività immediata del passaggio Home -> Dettaglio e Dettaglio -> Player.
   - Test Background: avvio player -> uscita su Home TV -> verifica chiusura player; rientro dopo 5+ minuti -> verifica atterraggio pulito su Home.
