# Piano di Lavoro: Risoluzione Caroselli e Isolamento Anime nella Home

## Obiettivo
Risolvere la mancata visualizzazione dei caroselli editoriali e l'estrema lentezza di caricamento riscontrata con l'ultima versione di Streaming Hub:
1. Rimuovere completamente i contenuti anime dal feed generale di Home e dalle categorie generali (film / serie TV).
2. Confinare i contenuti anime nella Home ad un **singolo carosello dedicato** intitolato **"Ultimi Episodi Anime"** (senza sovrascrivere l'Hero banner).
3. Rendere resiliente e veloce il recupero dei caroselli principali (StreamingCommunity) introducendo auto-aggiornamento su redirect di dominio (es. da `.photos` a `.shiksha`), timeout fail-fast e cache in-memory.

## Root Causes Identificate dal Log
1. **Timeout a catena e fallback a Grid Mode in `engine_reactive`**:
   - `streamingcommunityz.photos` ha subito un cambio dominio con HTTP 301 verso `streamingcommunityz.shiksha` e/o blocchi DNS.
   - `ReactiveStreamClient` non aggiornava `self.base_url` sul redirect e iterava sequenzialmente su 4 URL candidati con timeout di 15s ciascuno.
   - Con entrambe le sorgenti (`reactive` e `anime`) in timeout, `/api/catalog/home` ha risposto con `{"mode": "grid", "carousels": []}`.
   - L'app Android TV (`HomeViewModel.kt`), ricevendo `mode: "grid"`, ha disattivato i caroselli (`isCarouselsMode = false`) mostrando solo le categorie statiche di fallback.
2. **Inquinamento e blocco della Home da parte del motore Anime**:
   - `AnimeSource` esponeva `supported_types = ["tv", "movie", "anime"]`.
   - Ad ogni caricamento della Home o scorrimento di "Ultimi", venivano effettuate chiamate concorrenti a `animeunity.so`, iniettando decine di anime nei feed "film" e "serie".
   - Ogni titolo anime avviava chiamate di arricchimento Cinemeta che fallivano/andavano in timeout, saturando al contempo il proxy immagini `/api/proxy/image` con errori 502/TimeoutError (48 secondi di attesa per una singola richiesta Home!).

## Scope & Non-Goals
- **In Scope**:
  - `streaming-hub-ha`:
    - `streaming_hub/backend/sources/manager.py`: Escludere anime da `get_latest` per `source_filter == "all"` e `media_type in ("all", "movie", "tv")`.
    - `streaming_hub/backend/sources/anime_source.py`: Restringere `supported_types = ["anime"]`. In `get_carousels()`, impostare `hero = None` e restituire un unico carosello con ID `"anime_latest_episodes"` e titolo `"Ultimi Episodi Anime"` (max 15-20 item).
    - `streaming_hub/backend/engine_anime.py`: Cache in-memory (TTL 10 min) e timeout fail-fast a 4s per il carosello anime.
    - `streaming_hub/backend/engine_reactive.py`: Aggiornamento dinamico di `self.base_url` su redirect 301/302, timeout per-candidate ridotto a 5s, cache in-memory caroselli (TTL 5 min).
  - `streaming-hub-tv`:
    - Verifica compatibilità e test render del carosello anime singolo in `HomeScreen.kt` / `HomeViewModel.kt`.
    - Compilazione e test di regressione.
- **Non-Goals (YAGNI)**:
  - Non rimuovere la sezione/tab Anime dedicata (la sorgente anime rimane attiva per ricerche e per la navigazione specifica anime).
  - Non alterare la pipeline HLS di riproduzione (già funzionante via proxy).

## Task Checklist
- [ ] **Task 1: Isolamento Sorgente Anime dal Feed Generale di Home**
  - In `streaming_hub/backend/sources/manager.py`, filtrare `sources` in `get_latest()` in modo che la sorgente `anime` venga interrogata solo se `source_filter == "anime"` o `media_type == "anime"`.
  - In `streaming_hub/backend/sources/anime_source.py`, aggiornare `supported_types = ["anime"]`.
- [ ] **Task 2: Rilegamento Anime a Singolo Carosello "Ultimi Episodi Anime"**
  - In `streaming_hub/backend/sources/anime_source.py`:
    - In `get_carousels()`, restituire `hero = None` e una lista contenente un unico carosello `id: "anime_latest_episodes"`, `title: "Ultimi Episodi Anime"` (max 20 elementi).
  - In `streaming_hub/backend/engine_anime.py`:
    - Aggiungere cache in-memory con TTL 10m per il carosello ultimi episodi.
    - Ridurre timeout chiamata scraping a 4s con fallback silenzioso su lista vuota / cache.
- [ ] **Task 3: Resilienza Dominio e Caroselli in `engine_reactive.py`**
  - In `ReactiveStreamClient._request()`, rilevare se la risposta è un redirect o se l'URL finale differisce dal netloc di `base_url`: in caso positivo, aggiornare `self.base_url`.
  - Ridurre timeout su candidate URLs da 15s a 5s.
  - Aggiungere cache in-memory (TTL 5m) su `get_homepage_carousels()`.
- [ ] **Task 4: Verifica Backend e Android TV**
  - Eseguire suite test backend: `.venv/bin/pytest tests/test_home_carousels.py tests/test_anime_engine.py` e full suite.
  - Eseguire linter: `.venv/bin/ruff check streaming_hub/ tests/`.
  - Verificare compilazione client TV: `./gradlew compileDebugKotlin --no-daemon`.
- [ ] **Task 5: Aggiornamento Documentazione e Handoff**
  - Sovrascrivere `docs/HANDOFF.md`.

## Strategia di Verifica
```bash
# Backend pytest & linter
cd /Users/andrea/Repository/streaming-hub-ha
.venv/bin/pytest tests/test_home_carousels.py tests/test_anime_engine.py -v
.venv/bin/pytest
.venv/bin/ruff check streaming_hub/ tests/

# Test manuale endpoint /api/catalog/home
.venv/bin/python -c "
import asyncio, sys
from streaming_hub.backend.sources.manager import SourceManager
from streaming_hub.backend.sources.reactive_source import ReactiveSource
from streaming_hub.backend.sources.anime_source import AnimeSource

async def test():
    mgr = SourceManager()
    mgr.register_source(ReactiveSource('https://streamingcommunityz.shiksha', enabled=True))
    mgr.register_source(AnimeSource('https://www.animeunity.so', enabled=True))
    hero, carousels = await mgr.get_home_carousels()
    print('Hero:', hero.title if hero else None)
    print('Carousels count:', len(carousels))
    for c in carousels:
        print(' -', c['id'], '|', c['title'], '|', len(c['items']))

asyncio.run(test())
"

# Client TV compilazione
cd /Users/andrea/Repository/streaming-hub-tv
./gradlew compileDebugKotlin --no-daemon
```
