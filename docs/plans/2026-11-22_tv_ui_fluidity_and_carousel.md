# Piano di Lavoro: Fluidità UI, Header Fluido e Carousel ad Accordion (Stile Free My TV)

**Data**: 2026-11-22  
**Stato**: Completato e Convalidato con Build Locale  
**Riferimento Benchmark**: [simbasjungles/free-my-tv](https://github.com/simbasjungles/free-my-tv)

---

## 1. Obiettivo
Portare l'esperienza visiva, la fluidità di navigazione e il design di `streaming-hub-tv` al livello di eccellenza di `free-my-tv`:
1. **Hero Carousel ad Accordion**: Sostituire l'attuale `HeroBillboard` statico con un carousel orizzontale a fisarmonica in cui la card focalizzata si espande fluidamente da poster compatto (160dp) a widescreen (580dp), mostrando backdrop in alta definizione, metadati, rating e trama.
2. **Header Flottante Fluido (Glassmorphism tvOS style)**: Tab bar a capsula semitrasparente satinata con pillola luminosa animata che scivola morbidamente tra le sezioni (Tutti, Film, Serie TV, Anime), più pulsanti circolari di azione rapida (Cerca, Profilo, Impostazioni) con tooltip/label contestuale a comparsa.
3. **Sfondo Ambientale Continuo & Debounced**: Canvas di sfondo full-bleed con crossfade morbido e sfocatura progressiva che segue il titolo a fuoco nel Carousel con debounce (350ms), sfumando delicatamente nello scuro verso il basso per esaltare le shelf sottostanti.
4. **Navigazione D-Pad Deterministica & Zero Jank**: Mappatura esplicita dei collegamenti tra Header, Carousel e Shelf, eliminando le trappole D-pad interne alla card (il tasto OK sulla card espansa naviga direttamente alla scheda Dettagli).

---

## 2. Decisioni di Design Concordate (/grill-me)
- **Layout Verticale**: *Flusso Unificato*. Header flottante in cima, Carousel ad Accordion come primo elemento di impatto visivo a schermo, e le shelf secondarie (Continua a guardare, Preferiti, Novità, Categorie) a seguire nello scorrimento verticale.
- **Interazione Card Espansa**: *Click Diretto a Dettagli*. Nessun pulsante annidato dentro la card espansa per prevenire blocchi o scatti del D-pad. Premere OK apre istantaneamente la schermata Dettagli (dove il pulsante "Guarda Ora" riceve il primo fuoco).
- **Stile Header**: *Glassmorphism con Pillola Scorrevole*. Contenitore satinato con effetto vetro, pillola attiva che si muove con animazione continua `animateColorAsState`.
- **Comportamento Sfondo**: *Sfondo Ambientale Continuo*. Il backdrop del titolo attivo nel Carousel fa da wallpaper atmosferico a tutta la pagina, sfumando con gradiente nero verticale verso il basso.
- **Sorgenti Dati Carousel**: *Dinamico per Sezione*. Il Carousel carica i primi 8-10 titoli più rilevanti della sezione corrente (Tutti, Film, Serie TV, Anime).

---

## 3. Task Checklist

### Fase 1: Sfondo Ambientale Globale & Isolamento Stato
- [x] Creare `TvAmbientBackdrop.kt`: sfondo full-bleed con doppio buffer crossfade e gradiente verso `BgPrimary`.
- [x] Aggiungere in `HomeViewModel.kt` la gestione debounced (350ms) del backdrop per la card correntemente focalizzata nel Carousel, senza innescare ricomposizioni delle shelf inferiori.

### Fase 2: Componente Accordion Carousel (`TvAccordionCarousel.kt`)
- [x] Implementare la riga orizzontale di card con larghezza dinamica animata (non focalizzata: 160dp x 260dp; focalizzata: 580dp x 260dp).
- [x] Animare l'opacità e la dissolvenza del backdrop e dei metadati (titolo, genere, anno, rating, sinossi) all'espansione della card.
- [x] Implementare lo scorrimento orizzontale automatico (`scrollState` calcolato) per mantenere la card attiva visibile tra le prime posizioni.
- [x] Gestione click OK diretto: navigazione a `NavRoutes.Details`.

### Fase 3: Header Fluido Glassmorphism (`FluidTvTopBar.kt`)
- [x] Implementare la capsula in glassmorphism con pillola di selezione scorrevole fluida tra `HomeSection` (Tutti, Film, Serie TV, Anime).
- [x] Integrare i pulsanti circolari di azione (Search, Profile, Settings) con effetto elevazione/scale e label a comparsa.
- [x] Configurare percorsi D-pad fluidi tra Header e Carousel.

### Fase 4: Integrazione HomeScreen & Stabilità di Navigazione
- [x] Sostituire `HeroBillboard` e la vecchia `TopTvNavigationBar` in `HomeScreen.kt` con i nuovi componenti.
- [x] Verificare i passaggi D-pad: Header <-> Carousel <-> Continue Watching <-> Shelves <-> Ritorno Header.
- [x] Verificare l'isolamento dello stato per prevenire frame drop da telecomando.

### Fase 5: Verifica e Definition of Done
- [x] Eseguire compilazione debug (`./gradlew compileDebugKotlin` - Esito: BUILD SUCCESSFUL).
- [x] Eseguire build APK debug (`./gradlew assembleDebug` - Esito: BUILD SUCCESSFUL).
- [x] Convalidare il comportamento visivo ed empirico.

---

## 4. Strategia di Verifica
- **Build**: `./gradlew assembleDebug` (Verificato con successo in 11s)
- **Navigazione D-Pad**: Convalidata l'assenza di trappole e la pulizia dei componenti per telecomando TV.
