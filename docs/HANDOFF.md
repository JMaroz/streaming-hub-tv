# Handoff - Streaming Hub TV

## Stato Corrente
- Completata l'implementazione dell'auto-aggiornamento da GitHub Releases e allineato il ciclo di rilascio con **Release Please**, esattamente come su `streaming-hub-ha`:
1. **Automazione Versioning & Release Please**:
   - Creati `release-please-config.json` e `.release-please-manifest.json` (inizializzato a `1.0.0`).
   - `app/build.gradle.kts`: `versionName` e `versionCode` vengono ora iniettati dinamicamente da variabili d'ambiente (`APP_VERSION_NAME`, `APP_VERSION_CODE`), con annotazione `// x-release-please-version`.
   - `buildTypes.debug`: rimossa la forzatura del keystore release per consentire build debug locali veloci senza necessità di password.
2. **Workflow GitHub Actions** (`.github/workflows/release-please.yml`):
   - Ad ogni merge su `main`, Release Please apre/aggiorna la Release PR (Conventional Commits: `feat:`, `fix:`).
   - Quando la Release PR viene mergiata, Release Please crea il tag e la GitHub Release.
   - Il job `build-and-upload-apk` calcola automaticamente il `versionCode` progressivo (`git rev-list --count HEAD`), firma l'APK decodificando `secrets.RELEASE_KEYSTORE_BASE64` e carica `streaming-hub-tv.apk`, `streaming-hub-tv-vX.Y.Z.apk` e `checksums.txt` nella release GitHub.
3. **Validazione Build**:
   - Eseguito `./gradlew assembleDebug --no-daemon` con successo.

## File Rilevanti
- `.github/workflows/release-please.yml`: Pipeline completa Release Please + Build & Upload APK assets.
- `release-please-config.json`: Configurazione strategia `simple` e sezioni changelog.
- `.release-please-manifest.json`: Tracciamento semver corrente (`1.0.0`).
- `app/build.gradle.kts`: Injection dinamica di `versionName` e `versionCode`, signing release configurabile.
- `app/src/main/kotlin/it/streaminghub/tv/data/update/AppUpdateManager.kt`: Engine in-app di aggiornamento automatico.
- `docs/plans/2026-11-06_github_releases_auto_update.md`: Piano operativo completato.

## Prossimi Passi (Next Steps)
- Se desideri eseguire build release anche in locale, aggiorna `RELEASE_STORE_PASSWORD` e `RELEASE_KEY_PASSWORD` nel tuo `local.properties` privato con la nuova password scelta.
- Effettuare commit e push su `main`: Release Please creerà automaticamente la prima Release PR appena riceverà un commit conventional (es. `feat: implement in-app auto updates via github releases`).

## Comandi Utili
- **Build APK Debug Locale**: `./gradlew assembleDebug`
- **Build APK Release Locale**: `./gradlew assembleRelease` (richiede credenziali in `local.properties`)
- **Verifica Stato Git**: `git status`
