# Piano di Lavoro: Auto-Update tramite GitHub Releases

## Obiettivo
Abilitare l'aggiornamento automatico e su richiesta dell'app Android TV `streaming-hub-tv` direttamente dai rilasci pubblici su GitHub Releases (`JMaroz/streaming-hub-tv`), senza passare dal Google Play Store, replicando il pattern nativo e robusto di `free-my-tv` (SmartTube-style).

## Scope & Non-Goals
- **In Scope**:
  - Verifica automatica (in background/all'avvio) e manuale (da UI Impostazioni/Setup) delle nuove release su GitHub.
  - Download in streaming in storage privato protetto (`noBackupFilesDir/updates`).
  - Validazione crittografica preventiva: SHA-256 e controllo corrispondenza certificato di firma APK con la chiave dell'app installata.
  - Installazione non presidiata/delegata tramite le API native `PackageInstaller` di Android TV con richiesta di conferma di sistema.
  - Pulizia automatica dei residui dopo l'aggiornamento (`MY_PACKAGE_REPLACED`).
  - Creazione del workflow GitHub Actions (`.github/workflows/release.yml`) per la compilazione, firma automatica e rilascio degli APK su GitHub.
- **Non-Goals (YAGNI / Ponytail Philosophy)**:
  - Nessuna dipendenza o framework di terze parti per gli update (es. AppCenter o librerie terze pesanti).
  - Nessun server di update custom intermedio: usiamo direttamente le API ufficiali di GitHub Releases.

## Task Checklist
- [x] **Fase 1: Configurazione Permessi e Manifest**
  - [x] Aggiungere `REQUEST_INSTALL_PACKAGES` e `RECEIVE_BOOT_COMPLETED` in `AndroidManifest.xml`.
  - [x] Registrare `UpdateReceiver` e `UpdateJobService` in `AndroidManifest.xml`.
- [x] **Fase 2: Core Update Engine & Receiver**
  - [x] Creare `it.streaminghub.tv.data.update.AppUpdateManager.kt` con logica di query API GitHub, download streaming, calcolo SHA-256 e verifica firma.
  - [x] Implementare sessione `PackageInstaller` con `PendingIntent` conforme ad Android 12+ (`FLAG_MUTABLE`).
  - [x] Creare `it.streaminghub.tv.receiver.UpdateReceiver.kt` per gestire `STATUS_PENDING_USER_ACTION`, `ACTION_MY_PACKAGE_REPLACED` e boot.
  - [x] Creare `it.streaminghub.tv.data.update.UpdateJobService.kt` per il controllo periodico con `JobScheduler`.
- [x] **Fase 3: Integrazione UI & Presentazione TV**
  - [x] Creare `it.streaminghub.tv.ui.components.UpdateDialog.kt` per il prompt D-Pad friendly su Android TV.
  - [x] Aggiornare `SetupScreen.kt` e `SetupViewModel.kt` per includere la sezione informativa e il tasto "Verifica Aggiornamenti".
  - [x] Collegare l'osservazione dello stato in `MainActivity.kt`.
- [x] **Fase 4: Configurazione Build & CI/CD GitHub Actions**
  - [x] Aggiornare `app/build.gradle.kts` per supportare variabili d'ambiente di signing (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, ecc.) e injection automatica di `versionCode` e `versionName`.
  - [x] Configurare `release-please` (`release-please-config.json` e `.release-please-manifest.json`) identico al workflow di `streaming-hub-ha`.
  - [x] Creare `.github/workflows/release-please.yml` con automazione Release PR, calcolo progressivo del `versionCode` (`git rev-list --count HEAD`), firma release e upload asset APK.
  - [x] Creare `.github/workflows/check.yml` per il controllo continuativo dei build su PR e push.
- [x] **Fase 5: Validazione e Test**
  - [x] Eseguire build `./gradlew assembleDebug` e `./gradlew assembleRelease`.
  - [x] Verificare conformità linter e assenza di regressioni.

## Strategia di Verifica
```bash
# 1. Verifica compilazione Debug
./gradlew assembleDebug

# 2. Verifica compilazione Release con signing locale
./gradlew assembleRelease

# 3. Verifica lint / syntax check
./gradlew check --dry-run
```
