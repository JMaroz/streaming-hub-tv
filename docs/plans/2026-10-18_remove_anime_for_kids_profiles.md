# Piano di Lavoro: Rimozione Contenuti Anime per Profili Bambino (Max 6+)

## Obiettivo
Garantire che quando è attivo un profilo bambino (parental control con rating massimo 6+, es. "T", "0", "6+", "KIDS", "PEGI 6", "PEGI 7"), i contenuti anime siano completamente rimossi dall'esperienza utente:
1. **Client Android TV (`streaming-hub-tv`)**: Rimuovere la tab "Anime" dalla barra di navigazione superiore (`TopTvNavigationBar`), eliminare caroselli/titoli anime dalla Home, nascondere anime da "Continua a guardare", "Preferiti" e ricerca, e bloccare la visualizzazione dei dettagli.
2. **Backend Home Assistant (`streaming-hub-ha`)**: Integrare in `rating_filter.py` la regola di esclusione totale dei titoli anime per qualsiasi profilo con rating <= 6, garantendo che le API Home (`/api/catalog/home`), catalogo anime (`/api/catalog/anime`), ricerca (`/api/catalog/search`), generi e dettagli (`/api/catalog/details/{id}`) non restituiscano alcun contenuto anime.

---

## Analisi Architetturale & Root Causes

### 1. Backend (`streaming-hub-ha`)
- **Rating Hierarchy**: In `rating_filter.py`, la classificazione associa punteggi numerici all'età minima: `T`/`0` -> `0`, `6+` -> `6`, `12+` -> `12`, `14+` -> `14`, `16+` -> `16`, `18+` -> `18`, `ALL` -> `99`. Un profilo bambino è definito in modo univoco da `get_profile_max_rating(profile) <= 6`.
- **Mancata esclusione degli Anime**: `is_title_allowed_for_profile()` verifica parole chiave per adulti (<18) e generi non adatti ai bambini (horror, thriller, splatter), ma non intercetta i titoli anime (che spesso hanno genere "Animazione", privi di certificazione esplicita o con tag generici). Di conseguenza, alcuni anime possono filtrare nei caroselli della Home o essere accessibili direttamente.
- **Carosello Dedicato Home**: `AnimeSource` produce il carosello `"anime_latest_episodes"`. Se `is_title_allowed_for_profile()` scarta tutti gli anime per profili <= 6, il carosello diventa vuoto e viene già potato automaticamente (`pruned`) da `main.py`.

### 2. Client Android TV (`streaming-hub-tv`)
- **Navigazione Statica**: `HomeScreen.kt` itera staticamente su `HomeSection.entries` (`ALL`, `MOVIES`, `SERIES`, `ANIME`), visualizzando sempre il pulsante "Anime" anche con un profilo Kids come `Leandro` (6+).
- **Mancata persistenza del Rating**: `AppPreferences.kt` e `ProfileRepository.kt` memorizzano in DataStore solo `id`, `name` e `avatar`, perdendo il `ratingFilter` ("6+", "ALL", etc.) fornito da `ProfileDto`.
- **Filtri Difensivi**: Se un bambino accede alla Home o alla Ricerca, `HomeViewModel` e `SearchViewModel` non controllano lo stato "Kids" per ripulire la UI o le sezioni speciali.

---

## Scope & Non-Goals

- **In Scope**:
  - `streaming-hub-ha`:
    - `rating_filter.py`: Definire `is_anime_title()` e bloccare categoricamente gli anime in `is_title_allowed_for_profile()` quando `get_profile_max_rating(profile) <= 6`.
    - `main.py`: Fail-fast in `/api/catalog/anime` se `get_profile_max_rating(profile) <= 6`.
    - `tests/test_rating_filter.py`: Aggiungere test di regressione per profili bambini e anime.
  - `streaming-hub-tv`:
    - `AppPreferences.kt`: Salvare ed esporre `activeProfileRating` e funzione utility `isKidsRating(ratingFilter: String): Boolean`.
    - `ProfileRepository.kt`: Salvare `ratingFilter` durante `selectProfile()`.
    - `HomeViewModel.kt`: Esporre in `HomeUiState` `isKidsProfile` e `availableSections` (senza `ANIME` per profili <= 6), reimpostare a `ALL` se su `ANIME`, filtrare anime da caroselli, preferiti e continue watching.
    - `HomeScreen.kt`: Renderizzare le tab in `TopTvNavigationBar` basandosi su `availableSections`.
    - `SearchViewModel.kt`: Filtrare risultati anime per profili bambini.
    - `DetailsViewModel.kt`: Impedire caricamento dettagli anime per profili bambini.
- **Non-Goals (YAGNI)**:
  - Non modificare il comportamento per profili con rating >= 14 o ALL (gli anime rimangono disponibili).
  - Non creare impostazioni manuali aggiuntive: la regola si basa direttamente sul rating del profilo (`<= 6`).

---

## Task Checklist

### Fase 1: Backend (`streaming-hub-ha`)
- [ ] **Task 1.1**: In `streaming_hub/backend/rating_filter.py`, creare `is_anime_title(title_item)` e aggiornare `is_title_allowed_for_profile()` per rifiutare anime quando `get_profile_max_rating(profile) <= 6`.
- [ ] **Task 1.2**: In `streaming_hub/backend/main.py`, aggiungere fail-fast in `get_anime_catalog()` per profili bambini (<= 6).
- [ ] **Task 1.3**: Aggiungere test unitari in `tests/test_rating_filter.py` per validare il blocco anime su profili `T`, `0`, `6+` e consentirli su `14+`, `18+`, `ALL`.
- [ ] **Task 1.4**: Eseguire suite test backend e linter ruff.

### Fase 2: Client Android TV (`streaming-hub-tv`)
- [ ] **Task 2.1**: In `AppPreferences.kt`, aggiungere `KEY_ACTIVE_PROFILE_RATING`, il flow `activeProfileRating` e la funzione `isKidsRating(rating: String): Boolean`.
- [ ] **Task 2.2**: In `ProfileRepository.kt`, aggiornare `selectProfile()` per salvare `ratingFilter`.
- [ ] **Task 2.3**: In `HomeViewModel.kt`, gestire `isKidsProfile` e `availableSections` (escludendo `HomeSection.ANIME` per i profili bambini), con reset su `ALL` e filtri su caroselli/personalizzati.
- [ ] **Task 2.4**: In `HomeScreen.kt`, aggiornare `TopTvNavigationBar` per visualizzare solo le sezioni presenti in `availableSections`.
- [ ] **Task 2.5**: In `SearchViewModel.kt` e `DetailsViewModel.kt`, aggiungere filtri difensivi contro contenuti anime per profili bambini.
- [ ] **Task 2.6**: Verificare compilazione Kotlin `./gradlew compileDebugKotlin`.

### Fase 3: Documentazione & Handoff
- [ ] **Task 3.1**: Spuntare i task completati nel piano.
- [ ] **Task 3.2**: Aggiornare e sovrascrivere `docs/HANDOFF.md` secondo i requisiti globali.

---

## Strategia di Verifica

```bash
# 1. Backend: Esecuzione test unitari e linter
cd /Users/andrea/Repository/streaming-hub-ha
.venv/bin/pytest tests/test_rating_filter.py -v
.venv/bin/ruff check streaming_hub/ tests/

# 2. Client TV: Compilazione Android Kotlin
cd /Users/andrea/Repository/streaming-hub-tv
./gradlew compileDebugKotlin

# 3. Test logico manuale backend (simulazione profilo 6+ vs ALL)
.venv/bin/python -c "
from streaming_hub.backend.models import Profile, TvSeries, Movie
from streaming_hub.backend.rating_filter import is_title_allowed_for_profile

p_kids = Profile(id='leandro', name='Leandro', rating_filter='6+')
p_adult = Profile(id='andrea', name='Andrea', rating_filter='ALL')

anime = TvSeries(id='anime-123', title='One Piece', is_anime=True, catalogs=['anime'])
movie = Movie(id='mov-1', title='Toy Story', genres=['Animazione', 'Famiglia'], certification='T')

assert not is_title_allowed_for_profile(anime, p_kids), 'Anime must be BLOCKED for kids'
assert is_title_allowed_for_profile(anime, p_adult), 'Anime must be ALLOWED for adult'
assert is_title_allowed_for_profile(movie, p_kids), 'Family movie must be ALLOWED for kids'
print('All assertion checks PASSED!')
"
```
