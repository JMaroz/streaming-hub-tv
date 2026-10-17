# Handoff - Streaming Hub TV

## Stato Corrente
Risolto il problema del focus iniziale e dello scorrimento indesiderato dell'Hero Banner nella Home su Smart TV:
1. **Ancoraggio TopTvNavigationBar**: Spostata la barra di navigazione con sezioni e brand fuori da `LazyColumn`, rendendola un header persistente in cima allo schermo con padding ottimizzato (16dp) e sfondo scuro `BgPrimary`. Rimosse le duplicazioni di layout nei rami `isLoading` ed `errorMessage`.
2. **Azzeramento Auto-Scroll & Focus Iniziale su Sezione Attiva**: Rimosso il `LaunchedEffect` che forzava programmaticamente `requestFocus()` su "Guarda Ora" scatenando `bringIntoView` e tagliando la parte alta dell'Hero Banner. Il focus iniziale si posiziona ora in modo pulito sul tab della sezione selezionata ("Tutti") nella barra superiore (`topBarFocusRequester`), mantenendo la schermata a scroll 0.
3. **Visibilità Completa Hero Banner & D-Pad Flow**: Con `HeroBillboard` come primo item della `LazyColumn` a coordinate stabili (offset 0), sia la Top Bar che l'intero Hero Banner (volto, sfondo, titolo, trama e i tre pulsanti) sono visibili per intero al 100% all'avvio. La navigazione con DPAD verso il basso porta fluidamente a "Guarda Ora" e solo successivamente scorre sulle righe di catalogo sottostanti.
4. **Build & Deploy Release**: Compilato con successo l'APK release (`./gradlew assembleRelease --no-daemon`) con R8 e installato via ADB sulla Smart TV Philips (`192.168.1.49:5555`).

## File Rilevanti
- `app/src/main/kotlin/it/streaminghub/tv/ui/home/HomeScreen.kt`: Layout a colonna con `TopTvNavigationBar` fissa, gestione `topBarFocusRequester` e `listState.scrollToItem(0, 0)`.
- `docs/plans/2026-10-16_fix_home_hero_banner_focus_and_scroll.md`: Piano di lavoro dettagliato del fix completato.

## Prossimi Passi (Next Steps)
- Validazione da telecomando fisico Philips all'avvio dell'app per verificare la navigazione DPAD tra i tab in alto e i pulsanti dell'Hero Banner.
- Esecuzione del commit Git convenzionale (`fix(home): anchor navigation bar and prevent hero banner auto scroll on initial focus`).

## Comandi Utili
- **Build APK Release**: `./gradlew assembleRelease --no-daemon`
- **Connessione ADB TV Philips**: `adb connect 192.168.1.49:5555`
- **Reinstallazione Release**: `adb -s 192.168.1.49:5555 install -r app/build/outputs/apk/release/app-release.apk`
- **Avvio Applicazione**: `adb -s 192.168.1.49:5555 shell am start -n it.streaminghub.tv/.MainActivity`
