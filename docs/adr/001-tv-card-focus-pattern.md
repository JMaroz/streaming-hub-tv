# ADR 001: Focus Feedback Pattern per Card ed Elementi TV

## Context
Nelle interfacce 10-foot (Android TV) la navigazione avviene tramite telecomando D-pad. Inizialmente, il modifier `tvFocusable` applicava uno zoom al focus pari a `focusedScale = 1.08f` partendo da `1.0f`.
Questo comportamento causava:
1. **Spostamento e jitter visivo percepito** durante la navigazione orizzontale rapida.
2. **Sovrapposizione con il testo sottostante**: con una card alta `215dp`, l'espansione a `1.08f` genera `+8.6dp` di crescita verso il basso, superando lo spaziatore di `6dp` e sormontando il titolo.
3. **Collisioni orizzontali/verticali**: l'assenza di un `zIndex` elevato sulla card a fuoco faceva sì che i bordi espansi finissero disegnati sotto la card successiva o venissero tagliati dai confini del `LazyRow`.

Sono state valutate due alternative:
- **Scelta A (Pattern Inverso)**: tutte le card a riposo scalate a `0.9f`, riportate a `1.0f` solo quando hanno il focus.
- **Scelta B (Micro-Zoom con Elevazione)**: scala controllata a `1.04f`, elevazione `zIndex(1f)` e isolamento dei margini verticali nel carosello.
- **Scelta Zero-Scale**: scala costante a `1.0f` (nessun ridimensionamento), focus segnalato unicamente da bordo ad alto contrasto e alone luminoso.

## Decision
Abbiamo adottato la **Scelta B** come default, predisponendo `tvFocusable` con parametri separati `unfocusedScale` e `focusedScale` per consentire il passaggio istantaneo alla **Scelta A** o a **Zero-Scale**.

### Perché la Scelta A non è il default
- **Variazione relativa maggiore**: il passaggio da `0.9f` a `1.0f` comporta un delta dimensionale di $1.0 / 0.9 = +11.1\%$, contro il $+4.0\%$ della Scelta B e il $+8.0\%$ della situazione iniziale.
- **Effetto fisarmonica**: durante lo scorrimento veloce, la card precedente collassa a 0.9 e quella nuova cresce a 1.0, generando un moto relativo continuo.
- **Perdita di densità e qualità**: a riposo l'intera fila di card appare rimpicciolita e distanziata artificialmente rispetto allo spazio allocato dalla griglia.

## Quick Switch Guide: Come passare alla Scelta A o a Zero-Scale
Il file `TvFocusModifier.kt` espone i parametri `unfocusedScale` e `focusedScale`.

### 1. Per passare alla Scelta A (Pattern Inverso 0.9 -> 1.0):
In `app/src/main/kotlin/it/streaminghub/tv/ui/components/TvFocusModifier.kt`:
```kotlin
fun Modifier.tvFocusable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    focusedScale: Float = 1.0f,      // <-- Card a fuoco torna a dimensione normale
    unfocusedScale: Float = 0.9f,    // <-- Card a riposo ridotta
    ...
)
```

### 2. Per passare alla Scelta Zero-Scale (Nessun movimento, solo Bordo/Glow):
In `app/src/main/kotlin/it/streaminghub/tv/ui/components/TvFocusModifier.kt`:
```kotlin
fun Modifier.tvFocusable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    focusedScale: Float = 1.0f,      // <-- Nessun ridimensionamento
    unfocusedScale: Float = 1.0f,    // <-- Nessun ridimensionamento
    ...
)
```

## Consequences
- **Vantaggi di Scelta B**:
  - Il delta di crescita scende da $+8.6\text{dp}$ a $+4.3\text{dp}$, completamente assorbito dallo `Spacer(8.dp)` in `TvPosterCard`.
  - Con `.zIndex(if (isFocused) 1f else 0f)`, l'elemento focalizzato fluttua sempre sopra i vicini senza generare artefatti grafici o tagli.
  - Il `contentPadding` verticale del `LazyRow` assicura che il carosello mantenga sempre una bounding box stabile.
- **Flessibilità**:
  - Cambiare strategia tra B, A o Zero-Scale richiede ora la modifica di soli 2 valori di default in `TvFocusModifier.kt`, senza toccare nessun altro file o componente dell'applicazione.
