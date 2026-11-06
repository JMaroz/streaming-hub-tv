# Handoff - Streaming Hub TV

## Stato Corrente
- Completata con successo l'implementazione del sistema di aggiornamento in-app da GitHub Releases e la pipeline CI/CD con Release Please:
  1. **Release `v1.1.0` verificata**: la release contiene esclusivamente l'asset standard `streaming-hub-tv.apk` e il rispettivo `checksums.txt` (eliminato l'asset duplicato con tag nel nome).
  2. **Pipeline GitHub Actions snellita**: `.github/workflows/release-please.yml` produce e carica unicamente `streaming-hub-tv.apk` e `checksums.txt`, garantendo coerenza con il client Android TV.
  3. **Sanitizzazione Credenziali**: credenziali keystore sanificate con `.trim()` in `app/build.gradle.kts` e rimozione spazi via `sed` nel workflow CI per prevenire errori di alias o password.
  4. **In-App Auto-Update Engine**: completata l'integrazione nativa (`AppUpdateManager`, `PackageInstaller`, `UpdateReceiver`, dialog D-pad friendly e controllo manuale da Setup).

## File Rilevanti
- `.github/workflows/release-please.yml`: Pipeline Release Please + build/sign e upload di solo `streaming-hub-tv.apk` e `checksums.txt`.
- `app/build.gradle.kts`: Sanitizzazione `trim()` per credenziali keystore e iniezione dinamica di `versionCode`/`versionName`.
- `app/src/main/kotlin/it/streaminghub/tv/data/update/AppUpdateManager.kt`: Engine nativo per controllo release, download streaming, calcolo SHA-256 e verifica certificato APK.
- `app/src/main/kotlin/it/streaminghub/tv/receiver/UpdateReceiver.kt`: Gestione prompt di sistema e cleanup residui post-installazione (`MY_PACKAGE_REPLACED`).
- `app/src/main/kotlin/it/streaminghub/tv/ui/components/UpdateDialog.kt`: Dialog di aggiornamento D-pad friendly.
- `docs/plans/2026-11-06_github_releases_auto_update.md`: Piano di lavoro operativo completato.

## Prossimi Passi (Next Steps)
- Testare l'app su dispositivo Android TV / emulatore.
- Per le future versioni: i commit su `main` con prefissi Conventional Commits (`feat:`, `fix:`) alimenteranno la Release PR di Release Please. Al merge, il workflow pubblicherà in automatico `streaming-hub-tv.apk` e `checksums.txt`.

## Comandi Utili
- **Build APK Debug Locale**: `./gradlew assembleDebug`
- **Build APK Release Locale**: `./gradlew assembleRelease` (richiede credenziali in `local.properties`)
- **Verifica Release GitHub**: `gh release view v1.1.0`
