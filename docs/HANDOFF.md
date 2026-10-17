# Handoff - Streaming Hub TV

## Stato Corrente
Stabilizzato il pattern di focus per le card ed elementi interattivi su Android TV (risolvendo jitter e spostamento percepito della UI):
1. **Scelta B Implementata**:
   - `TvFocusModifier.kt`: ridotto `focusedScale` da `1.08f` a un sobrio `1.04f` ed elevato il layer di disegno con `.zIndex(if (isFocused) 1f else 0f)` per evitare clipping o collisioni con le card adiacenti.
   - Aggiunto parametro `unfocusedScale: Float = 1.0f` per consentire configurazioni arbitrarie senza toccare i singoli componenti.
2. **Isolamento Spazi e Margini**:
   - `TvPosterCard.kt`: distanziatore tra poster e titolo aumentato a `8.dp` per assorbire completamente la dilatazione verticale di 4.3dp.
   - `TvEpisodeCard.kt`: distanziatore aumentato a `8.dp` per non toccare i testi.
   - `TvShelfRow.kt`: aggiunto `contentPadding = PaddingValues(horizontal = 48.dp, vertical = 6.dp)` al `LazyRow` per pre-allocare l'area di rendering delle card a fuoco.
3. **Documentazione & Switch Rapido a Scelta A o Zero-Scale**:
   - Creato `docs/adr/001-tv-card-focus-pattern.md` con l'analisi architetturale dettagliata e la guida rapida per passare alla Scelta A (`unfocusedScale = 0.9f, focusedScale = 1.0f`) o a Zero-Scale in un'unica riga.
4. **Verifica Empirica**:
   - Compilazione Kotlin completata con successo (`./gradlew compileDebugKotlin` BUILD SUCCESSFUL in 23s).

## File Rilevanti
- `app/src/main/kotlin/it/streaminghub/tv/ui/components/TvFocusModifier.kt`: Supporto a `unfocusedScale`, `focusedScale = 1.04f` e `zIndex(1f)`.
- `app/src/main/kotlin/it/streaminghub/tv/ui/components/TvPosterCard.kt`: Distanziatore `Spacer(8.dp)` poster-titolo.
- `app/src/main/kotlin/it/streaminghub/tv/ui/components/TvEpisodeCard.kt`: Distanziatore `Spacer(8.dp)` thumbnail-titolo.
- `app/src/main/kotlin/it/streaminghub/tv/ui/components/TvShelfRow.kt`: `contentPadding` verticale per isolamento LazyRow.
- `docs/adr/001-tv-card-focus-pattern.md`: ADR con analisi comparativa e guida di switch rapido.
- `docs/plans/2026-10-17_stabilize_tv_card_focus_scale.md`: Piano di lavoro completato e verificato.

## Prossimi Passi (Next Steps)
- Eseguire test su TV fisica (`./gradlew assembleDebug` o `installDebug`) verificando il feeling di scorrimento D-pad.
- Se si preferisce azzerare del tutto qualsiasi ridimensionamento, impostare `focusedScale = 1.0f` in `TvFocusModifier.kt` come indicato nell'ADR 001.

## Comandi Utili
- **Compilazione Kotlin**: `./gradlew compileDebugKotlin`
- **Build APK Release**: `./gradlew assembleRelease --no-daemon`
- **Deploy ADB TV**: `adb -s 192.168.1.49:5555 install -r app/build/outputs/apk/release/app-release.apk`
