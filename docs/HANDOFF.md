# Handoff - Streaming Hub TV

## Stato Corrente
- Completata con successo l'implementazione del nuovo design fluido e del Carousel ad Accordion ispirato al benchmark `free-my-tv`:
  1. **`TvAmbientBackdrop`**: Sfondo cinematografico a pieno schermo con crossfade morbido e gradiente verticale continuo verso lo scuro profondo.
  2. **`TvAccordionCarousel`**: Hero Carousel a fisarmonica ad alte performance per Android TV. La card a fuoco si allarga fluidamente da 160dp a 580dp con animazione FastOutSlowIn, rivelando backdrop orizzontale in alta risoluzione, badge, titolo, rating e sinossi. Click OK diretto alla scheda Dettagli (zero trappole D-pad).
  3. **`FluidTvTopBar`**: Header flottante in glassmorphism (stile tvOS) con capsule container, pillola di selezione animata per le categorie e pulsanti di azione rapida circolari ad alto contrasto.
  4. **Ottimizzazione Fluidità & Debounce**: In `HomeViewModel.kt`, il cambio di backdrop fotografico è regolato da un debounce di 350ms, eliminando ricomposizioni a cascata e saturazione di banda/GPU durante lo scorrimento veloce con il D-pad.
  5. **Verifica Empirica**: Compilazione Kotlin e build dell'APK debug (`./gradlew assembleDebug`) completate con esito positivo (`BUILD SUCCESSFUL in 11s`).

## File Rilevanti
- `app/src/main/kotlin/it/streaminghub/tv/ui/components/TvAmbientBackdrop.kt`: Sfondo ambientale dinamico con crossfade e gradienti cinema.
- `app/src/main/kotlin/it/streaminghub/tv/ui/components/TvAccordionCarousel.kt`: Carousel orizzontale ad accordion con card espandibile e scorrimento sincronizzato.
- `app/src/main/kotlin/it/streaminghub/tv/ui/components/FluidTvTopBar.kt`: Header in glassmorphism con tab animate a pillola e pulsanti circolari.
- `app/src/main/kotlin/it/streaminghub/tv/ui/home/HomeScreen.kt`: Schermata Home integrata con flusso unificato e layout fluido.
- `app/src/main/kotlin/it/streaminghub/tv/ui/home/HomeViewModel.kt`: Gestione debounced del backdrop (350ms) e caricamento dei titoli del carousel.
- `docs/plans/2026-11-22_tv_ui_fluidity_and_carousel.md`: Piano di lavoro completato e verificato.

## Prossimi Passi (Next Steps)
- Eseguire il deploy o test dell'APK di debug (`app/build/outputs/apk/debug/app-debug.apk`) su dispositivo Android TV reale o emulatore per testare il feeling del telecomando fisico.
- (Opzionale) Estendere l'effetto ambient backdrop con leggero tint adattivo se desiderato.

## Comandi Utili
- **Build APK Debug Locale**: `./gradlew assembleDebug`
- **Installazione su TV via ADB**: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- **Logcat TV per verifica frame**: `adb logcat -s StreamingHub`
