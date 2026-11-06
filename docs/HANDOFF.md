# Handoff - Streaming Hub TV

## Stato Corrente
- Completata con successo l'implementazione del sistema di aggiornamento in-app da GitHub Releases e la pipeline CI/CD con Release Please:
  1. **Release `v1.1.0` integrata con APK**: risolto il problema della mancata presenza degli APK causato da uno spazio di fine riga nel secret `RELEASE_KEY_ALIAS`. L'azione è stata eseguita e ha compilato, firmato e caricato `streaming-hub-tv.apk`, `streaming-hub-tv-v1.1.0.apk` e `checksums.txt` negli asset della release GitHub.
  2. **Sanitizzazione Credenziali**: aggiunta sanitizzazione con `.trim()` in `app/build.gradle.kts` e pulizia via `sed` nel workflow GitHub Actions per evitare problemi legati a whitespace accidentali nei secrets.
  3. **Supporto `workflow_dispatch`**: `.github/workflows/release-please.yml` ora supporta anche l'esecuzione manuale con parametro `release_tag` per compilare e allegare retroattivamente APK a qualsiasi release esistente.
  4. **In-App Auto-Update Engine**: completata l'integrazione nativa (`AppUpdateManager`, `PackageInstaller`, `UpdateReceiver`, dialog D-pad friendly e controllo da Impostazioni).

## File Rilevanti
- `.github/workflows/release-please.yml`: Pipeline Release Please + build/sign APK + supporto `workflow_dispatch`.
- `app/build.gradle.kts`: Sanitizzazione `trim()` per credenziali keystore e iniezione dinamica di `versionCode`/`versionName`.
- `app/src/main/kotlin/it/streaminghub/tv/data/update/AppUpdateManager.kt`: Engine nativo per controllo release, download streaming, calcolo SHA-256 e verifica certificato APK.
- `app/src/main/kotlin/it/streaminghub/tv/receiver/UpdateReceiver.kt`: Gestione prompt di sistema e cleanup residui post-installazione (`MY_PACKAGE_REPLACED`).
- `app/src/main/kotlin/it/streaminghub/tv/ui/components/UpdateDialog.kt`: Dialog di aggiornamento D-pad friendly.
- `docs/plans/2026-11-06_github_releases_auto_update.md`: Piano di lavoro operativo completato.

## Prossimi Passi (Next Steps)
- Testare l'app installata su una TV/emulatore Android TV con versione precedente o verificare il pulsante "Verifica Aggiornamenti" nella schermata Setup/Impostazioni.
- Per le future versioni: i commit su `main` con prefissi Conventional Commits (`feat:`, `fix:`) aggiorneranno automaticamente la Release PR creata da Release Please. Al merge della PR, la compilazione e l'upload degli APK avverranno in automatico.

## Comandi Utili
- **Build APK Debug Locale**: `./gradlew assembleDebug`
- **Build APK Release Locale**: `./gradlew assembleRelease` (richiede credenziali in `local.properties`)
- **Trigger Manuale Release Actions**: `gh workflow run "Release Please" -f release_tag=vX.Y.Z`
- **Verifica Release GitHub**: `gh release view v1.1.0`
