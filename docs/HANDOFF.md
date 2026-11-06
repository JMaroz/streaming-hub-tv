# Handoff - Streaming Hub TV

## Stato Corrente
Completate e consolidate le ottimizzazioni di fluidità visiva e la gestione robusta del ciclo di vita e della sessione (Process Death & Standby) su Smart TV:
1. **Risoluzione Radice del Background / Process Death** (`MainActivity.kt`):
   - **Persistenza su Disco**: Spostato il tracciamento del timestamp da una variabile volatile in RAM a `SharedPreferences` sincrone (`app_session`) e `onSaveInstanceState(outState)` scattando fin da `onPause()`.
   - **Immunità a Process Death (Low Memory Killer)**: Su Smart TV con RAM ridotta (~1.5GB), quando il sistema operativo termina il processo in background, `onCreate` valuta il timestamp persistito: se trascorsi $\ge 5$ minuti, passa `null` a `super.onCreate(null)` azzerando il vecchio backstack (scheda dettaglio/player) e avviando una sessione fresca sulla Home.
   - **Gestione in-memory**: Se il processo rimane vivo in RAM, `onResume` ripristina la Home dopo 5 minuti tramite `resetToInitialStateTrigger`.
2. **Focus Statico & Zero Overhead GPU** (`TvFocusModifier.kt`):
   - Rimossa la shadow dinamica con blur shader (`.shadow(...)`) che saturava la GPU nei cambi di focus.
   - Scala fissata a 1.0x (zero espansione/oscillazione elastica): le locandine restano fisse nello spazio senza spostare o disallineare gli elementi adiacenti.
   - Bordo vivido ad alto contrasto (2.5dp) per un'immediata leggibilità (pattern Netflix/Prime Video).
3. **Reattività al Click & Transizioni NavHost** (`AppNavigation.kt`):
   - Configurato crossfade ultra-rapido a 100ms tra schermate, eliminando lag e freeze percepiti all'apertura dei dettagli/player.
4. **Chiusura Pulita del Player** (`PlayerScreen.kt`):
   - Uscita controllata con salvataggio progressi su evento `ON_STOP` per liberare decodificatori hardware e memoria.
5. **Build Release Verificata**:
   - Compilazione release `./gradlew assembleRelease --no-daemon` completata con successo (`app/build/outputs/apk/release/app-release.apk`, 6.8 MB).

## File Rilevanti
- `app/src/main/kotlin/it/streaminghub/tv/MainActivity.kt`: Gestione persistente della sessione con protezione da Process Death (`SharedPreferences`, `onPause`, `super.onCreate(null)` se scaduta).
- `app/src/main/kotlin/it/streaminghub/tv/ui/components/TvFocusModifier.kt`: Focus statico a 1.0x, rimozione ombre dinamiche e pulizia click.
- `app/src/main/kotlin/it/streaminghub/tv/ui/navigation/AppNavigation.kt`: Transizioni crossfade 100ms su `NavHost`.
- `app/src/main/kotlin/it/streaminghub/tv/ui/player/PlayerScreen.kt`: `safeExit()` automatico con salvataggio progressi su `ON_STOP`.
- `docs/plans/2026-11-06_tv_fluidity_and_lifecycle_optimization.md`: Piano operativo e specifiche di verifica.

## Prossimi Passi (Next Steps)
- Connettere e accendere la TV (`192.168.1.50:5555`) ed eseguire l'installazione dell'APK:
  `adb -s 192.168.1.50:5555 install -r app/build/outputs/apk/release/app-release.apk`
- Verificare sul telecomando la stabilità visiva durante lo scorrimento rapido tra le sezioni e le locandine.
- Validare l'uscita in background e il comportamento di ripristino dopo 5-10 minuti di standby.

## Comandi Utili
- **Build APK Release**: `./gradlew assembleRelease --no-daemon`
- **Connessione ADB TV**: `adb connect 192.168.1.50:5555`
- **Installazione ADB TV**: `adb -s 192.168.1.50:5555 install -r app/build/outputs/apk/release/app-release.apk`
- **Avvio App TV**: `adb -s 192.168.1.50:5555 shell monkey -p it.streaminghub.tv -c android.intent.category.LEANBACK_LAUNCHER 1`
