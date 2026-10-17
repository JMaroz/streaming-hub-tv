# Allineamento Icona Brand e Stile in-app con streaming-hub-ha

## Obiettivo
Sostituire la vecchia icona vettoriale generica (`ic_brand_logo.xml`) all'interno dell'app Android TV con l'asset grafico ufficiale del brand (`icon.png` 256x256 PNG) proveniente da `streaming-hub-ha`, allineando la tipologia di rendering (raster a colori originali con angoli arrotondati e bagliore/bordo ciano/viola) e il gradiente del titolo ("Streaming**Hub**" ciano -> viola).

## Scope & Non-Goals
- **In Scope**:
  - Rimozione di `app/src/main/res/drawable/ic_brand_logo.xml` (vecchio mockup vettoriale monocromatico).
  - Importazione dell'icona ufficiale da `streaming-hub-ha/streaming_hub/icon.png` in `app/src/main/res/drawable/ic_brand_logo.png`.
  - Definizione dei token colore brand (`BrandCyan`, `BrandViolet`, `BrandGradient`) in `Color.kt`.
  - Aggiornamento di `HomeScreen.kt`: rendering con `Image`, angoli arrotondati (`RoundedCornerShape(8.dp)`), bordo ciano sfumato e gradiente su "Hub".
  - Aggiornamento di `SetupScreen.kt`: rendering con `Image` e titolo brand con `BrandGradient`.
  - Allineamento di `ProfilePickerScreen.kt`: introduzione dell'icona brand sopra la schermata profilo come su HA web app.
- **Non-Goals** (YAGNI):
  - Modifica del launcher icon (`ic_launcher.png`) o TV banner (`tv_banner.png`) che sono già allineati alla versione ufficiale.
  - Modifiche non correlate alla UI del player o ai flussi di streaming.

## Task Checklist
- [x] Sostituire asset drawable: eliminare `ic_brand_logo.xml` e copiare `icon.png` in `app/src/main/res/drawable/ic_brand_logo.png`.
- [x] Definire token `BrandCyan`, `BrandViolet` e `BrandGradient` in `Color.kt`.
- [x] Aggiornare `HomeScreen.kt` per visualizzare `ic_brand_logo` come `Image` a colori pieni con shape e gradient title.
- [x] Aggiornare `SetupScreen.kt` con la nuova tipologia di icona e gradient title.
- [x] Aggiungere l'icona brand a `ProfilePickerScreen.kt` in linea con la modale HA.
- [x] Compilare ed eseguire build con verifica APK (`./gradlew assembleRelease --no-daemon`).
- [x] Sovrascrivere `docs/HANDOFF.md` con il punto di arrivo.

## Strategia di Verifica
- Verificare che il file `app/src/main/res/drawable/ic_brand_logo.png` sia presente e abbia lo stesso hash md5 di `streaming_hub/icon.png`.
- Eseguire `./gradlew assembleRelease --no-daemon` per confermare l'assenza di errori di risorse duplicate o conflitti grafici.
