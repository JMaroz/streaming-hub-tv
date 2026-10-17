# Stabilizzazione Focus Pattern Card TV e Documentazione Alternative

## Obiettivo
Stabilizzare l'interfaccia 10-foot su Android TV eliminando gli spostamenti percepiti (layout jitter/shift) durante la navigazione con D-pad. Viene adottata la **Scelta B** (micro-zoom `1.04f`, elevazione `zIndex(1f)` e isolamento dei margini verticali nel carosello), documentando formalmente la **Scelta A** (inversa `0.9f` -> `1.0f`) e l'opzione **Zero-Scale** per consentire un cambio immediato di configurazione in qualsiasi momento.

## Scope & Non-Goals
- **In Scope**:
  - `TvFocusModifier.kt`: parametri configurabili `unfocusedScale: Float = 1.0f`, `focusedScale: Float = 1.04f`, inserimento di `.zIndex(if (isFocused) 1f else 0f)` per evitare clipping e collisioni con i fratelli.
  - `TvPosterCard.kt`: distanziatore di sicurezza tra poster e titolo (`Spacer(8.dp)`) per evitare sovrapposizioni visive nel delta di crescita verticale.
  - `TvShelfRow.kt`: `contentPadding` verticale per `LazyRow` pre-allocando l'area di rendering dell'animazione.
  - `docs/adr/001-tv-card-focus-pattern.md`: documentazione architetturale con il trade-off, la motivazione di esclusione della Scelta A come default, e la guida one-liner per attivare la Scelta A o la Scelta Zero-Scale.
- **Non-Goals**:
  - Nessuna modifica alle chiamate di rete o alla gestione dello stato del `HomeViewModel`.
  - Nessuna libreria esterna aggiunta.

## Task Checklist
- [x] Analisi delle opzioni A e B e ricerca delle best practice industriali (Netflix, tvOS, Material 3 TV).
- [x] Creazione ADR in `docs/adr/001-tv-card-focus-pattern.md` con guida per switch rapido.
- [x] Aggiornamento `TvFocusModifier.kt` con parametri `unfocusedScale`/`focusedScale` e `zIndex`.
- [x] Aggiustamento margini e distanziatori in `TvPosterCard.kt` e `TvShelfRow.kt`.
- [x] Verifica empirica di compilazione con `./gradlew compileDebugKotlin`.
- [x] Aggiornamento `docs/HANDOFF.md`.

## Strategia di Verifica
1. Eseguire `./gradlew compileDebugKotlin` verificando l'assenza di errori di compilazione e regressioni sui contratti dei componenti Compose.
2. Ispezione visiva del codice per garantire che `tvFocusable` rimanga retrocompatibile con tutti i componenti che già lo invocano.
