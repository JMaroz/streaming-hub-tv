package it.streaminghub.tv.data.update

import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Log
import it.streaminghub.tv.BuildConfig
import it.streaminghub.tv.receiver.UpdateReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data class UpdateAvailable(
        val version: String,
        val releaseNotes: String,
        val apkUrl: String,
        val apkSize: Long,
        val sha256: String?
    ) : UpdateStatus
    data class Downloading(val progress: Float) : UpdateStatus
    data class ReadyToInstall(val apkFile: File, val version: String) : UpdateStatus
    data object Installing : UpdateStatus
    data class UpToDate(val currentVersion: String) : UpdateStatus
    data class Error(val message: String) : UpdateStatus
}

class AppUpdateManager private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val updatesDir: File by lazy {
        File(appContext.noBackupFilesDir, "updates").apply { mkdirs() }
    }

    /**
     * Checks GitHub Releases for a newer version of the application.
     */
    fun checkForUpdates(silent: Boolean = false) {
        if (_status.value is UpdateStatus.Checking || _status.value is UpdateStatus.Downloading) {
            return
        }

        scope.launch {
            if (!silent) {
                _status.value = UpdateStatus.Checking
            }

            try {
                val request = Request.Builder()
                    .url(LATEST_RELEASE_URL)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "StreamingHubTV/${BuildConfig.VERSION_NAME}")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val code = response.code
                    response.close()
                    if (!silent) {
                        _status.value = UpdateStatus.Error("GitHub API HTTP $code")
                    }
                    return@launch
                }

                val body = response.body.string()
                response.close()

                if (body.isBlank()) {
                    if (!silent) _status.value = UpdateStatus.Error("Risposta vuota da GitHub")
                    return@launch
                }

                val json = JSONObject(body)
                val rawTag = json.optString("tag_name", "")
                val releaseVersion = rawTag.removePrefix("v").removePrefix("V").trim()
                val bodyNotes = json.optString("body", "Nessuna nota di rilascio fornita.")
                val currentVersion = BuildConfig.VERSION_NAME

                if (releaseVersion.isBlank()) {
                    if (!silent) _status.value = UpdateStatus.Error("Tag release mancante")
                    return@launch
                }

                Log.d(TAG, "Ultima release GitHub: $releaseVersion, Versione installata: $currentVersion")

                if (compareVersions(releaseVersion, currentVersion) <= 0) {
                    _status.value = UpdateStatus.UpToDate(currentVersion)
                    return@launch
                }

                // Cerca l'asset APK all'interno della release
                val assets = json.optJSONArray("assets")
                var targetApkUrl: String? = null
                var targetApkSize: Long = 0
                var targetDigest: String? = null

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            targetApkUrl = asset.optString("browser_download_url")
                            targetApkSize = asset.optLong("size", 0L)
                            targetDigest = asset.optString("digest", "").takeIf { it.isNotBlank() }
                            break
                        }
                    }
                }

                if (targetApkUrl.isNullOrBlank()) {
                    if (!silent) _status.value = UpdateStatus.Error("Nessun APK trovato nella release $releaseVersion")
                    return@launch
                }

                _status.value = UpdateStatus.UpdateAvailable(
                    version = releaseVersion,
                    releaseNotes = bodyNotes,
                    apkUrl = targetApkUrl,
                    apkSize = targetApkSize,
                    sha256 = targetDigest
                )
            } catch (e: Exception) {
                Log.w(TAG, "Errore verifica aggiornamenti: ${e.message}")
                if (!silent) {
                    _status.value = UpdateStatus.Error(e.localizedMessage ?: "Errore di connessione")
                }
            }
        }
    }

    /**
     * Downloads the APK file and triggers installation.
     */
    fun startDownloadAndInstall(available: UpdateStatus.UpdateAvailable) {
        scope.launch {
            _status.value = UpdateStatus.Downloading(0f)

            val fileName = "StreamingHub-${available.version}.apk"
            val targetFile = File(updatesDir, fileName)
            val partFile = File(updatesDir, "$fileName.part")

            try {
                val request = Request.Builder()
                    .url(available.apkUrl)
                    .header("User-Agent", "StreamingHubTV/${BuildConfig.VERSION_NAME}")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val code = response.code
                    response.close()
                    _status.value = UpdateStatus.Error("Download fallito (HTTP $code)")
                    return@launch
                }

                val body = response.body
                val totalBytes = if (available.apkSize > 0) available.apkSize else body.contentLength()
                val sha256Digest = MessageDigest.getInstance("SHA-256")

                body.byteStream().use { input ->
                    FileOutputStream(partFile).use { output ->
                        val buffer = java.io.ByteArrayOutputStream()
                        val byteChunk = ByteArray(16384)
                        var bytesRead: Int
                        var downloadedBytes = 0L

                        while (input.read(byteChunk).also { bytesRead = it } != -1) {
                            output.write(byteChunk, 0, bytesRead)
                            sha256Digest.update(byteChunk, 0, bytesRead)
                            downloadedBytes += bytesRead

                            if (totalBytes > 0) {
                                val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                _status.value = UpdateStatus.Downloading(progress)
                            }
                        }
                        output.flush()
                    }
                }
                response.close()

                // Verifica integrità SHA-256 se dichiarato
                val calculatedSha = sha256Digest.digest().joinToString("") { "%02x".format(it) }
                if (available.sha256 != null && available.sha256.startsWith("sha256:", ignoreCase = true)) {
                    val expectedSha = available.sha256.substring(7)
                    if (!expectedSha.equals(calculatedSha, ignoreCase = true)) {
                        partFile.delete()
                        _status.value = UpdateStatus.Error("Verifica SHA-256 fallita")
                        return@launch
                    }
                }

                if (targetFile.exists()) {
                    targetFile.delete()
                }
                if (!partFile.renameTo(targetFile)) {
                    _status.value = UpdateStatus.Error("Impossibile salvare il file di aggiornamento")
                    return@launch
                }

                // Verifica crittografica firma del pacchetto prima dell'installazione
                val validationError = verifyApk(appContext, targetFile)
                if (validationError != null) {
                    Log.e(TAG, "Validazione APK fallita: $validationError")
                    targetFile.delete()
                    _status.value = UpdateStatus.Error(validationError)
                    return@launch
                }

                _status.value = UpdateStatus.ReadyToInstall(targetFile, available.version)
                installApk(targetFile)
            } catch (e: Exception) {
                Log.e(TAG, "Errore durante il download dell'APK", e)
                partFile.delete()
                _status.value = UpdateStatus.Error(e.localizedMessage ?: "Errore di download")
            }
        }
    }

    /**
     * Submits the APK to Android's PackageInstaller session.
     */
    private fun installApk(apkFile: File) {
        _status.value = UpdateStatus.Installing

        try {
            val packageInstaller = appContext.packageManager.packageInstaller

            // Pulisce eventuali sessioni orfane
            packageInstaller.mySessions.forEach { sessionInfo ->
                try {
                    packageInstaller.abandonSession(sessionInfo.sessionId)
                } catch (_: Exception) {}
            }

            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(appContext.packageName)
                setSize(apkFile.length())
            }

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            session.use { activeSession ->
                apkFile.inputStream().use { inputStream ->
                    activeSession.openWrite("streaming_hub_update", 0, apkFile.length()).use { outputStream ->
                        inputStream.copyTo(outputStream, bufferSize = 16384)
                        activeSession.fsync(outputStream)
                    }
                }

                val callbackIntent = Intent(appContext, UpdateReceiver::class.java).apply {
                    action = ACTION_INSTALL_STATUS
                    putExtra(EXTRA_SESSION_ID, sessionId)
                }

                val flags = PendingIntent.FLAG_UPDATE_CURRENT or (
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                )
                val pendingIntent = PendingIntent.getBroadcast(appContext, sessionId, callbackIntent, flags)

                activeSession.commit(pendingIntent.intentSender)
                Log.i(TAG, "Sessione di installazione APK $sessionId inviata con successo")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore avvio installazione PackageInstaller", e)
            _status.value = UpdateStatus.Error("Errore installer: ${e.localizedMessage}")
        }
    }

    fun dismiss() {
        _status.value = UpdateStatus.Idle
    }

    fun onInstallSuccess() {
        cleanUp()
        _status.value = UpdateStatus.UpToDate(BuildConfig.VERSION_NAME)
    }

    fun onInstallFailed(reason: String) {
        _status.value = UpdateStatus.Error(reason)
    }

    /**
     * Rimuove i file APK temporanei scaricati.
     */
    fun cleanUp() {
        try {
            updatesDir.listFiles()?.forEach { file ->
                file.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pulizia directory updates fallita: ${e.message}")
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "it.streaminghub.tv.ACTION_INSTALL_STATUS"
        const val EXTRA_SESSION_ID = "extra_session_id"
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/JMaroz/streaming-hub-tv/releases/latest"
        private const val TAG = "AppUpdateManager"
        private const val JOB_ID = 4200

        @Volatile
        private var instance: AppUpdateManager? = null

        fun getInstance(context: Context): AppUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: AppUpdateManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Confronta due versioni stringa semver (es. "1.1.0" vs "1.0.0").
         */
        fun compareVersions(v1: String, v2: String): Int {
            val parts1 = v1.removePrefix("v").removePrefix("V").split(".", "-")
            val parts2 = v2.removePrefix("v").removePrefix("V").split(".", "-")
            val maxLen = maxOf(parts1.size, parts2.size)

            for (i in 0 until maxLen) {
                val num1 = parts1.getOrNull(i)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
                val num2 = parts2.getOrNull(i)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
                if (num1 != num2) {
                    return num1.compareTo(num2)
                }
            }
            return 0
        }

        /**
         * Verifica preventivamente l'APK scaricato prima di passarlo al PackageInstaller.
         */
        @Suppress("DEPRECATION")
        fun verifyApk(context: Context, apkFile: File): String? {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }

            val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
                ?: return "File APK corrotto o illeggibile"

            if (archiveInfo.packageName != context.packageName) {
                return "Package name non corrispondente (${archiveInfo.packageName} vs ${context.packageName})"
            }

            // Verifica che l'APK scaricato sia firmato con la stessa chiave dell'app installata
            val currentPkg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                pm.getPackageInfo(context.packageName, flags)
            }

            val currentCert = getCertSha256(currentPkg)
            val downloadedCert = getCertSha256(archiveInfo)

            if (currentCert != null && downloadedCert != null && !currentCert.equals(downloadedCert, ignoreCase = true)) {
                return "Firma APK non corrispondente al certificato dell'app installata"
            }

            return null
        }

        @Suppress("DEPRECATION")
        private fun getCertSha256(pkg: PackageInfo): String? {
            val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.signingInfo?.apkContentsSigners
            } else {
                pkg.signatures
            }

            if (signatures.isNullOrEmpty()) return null
            return try {
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(signatures[0].toByteArray())
                digest.joinToString("") { "%02x".format(it) }
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Schedula il controllo periodico con JobScheduler (es. ogni 24 ore su rete attiva).
         */
        fun schedulePeriodicCheck(context: Context) {
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            if (scheduler.getPendingJob(JOB_ID) != null) return

            val job = JobInfo.Builder(JOB_ID, ComponentName(context, UpdateJobService::class.java))
                .setPeriodic(24 * 60 * 60 * 1000L) // 24 ore
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setBackoffCriteria(30 * 60 * 1000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .build()

            scheduler.schedule(job)
            Log.d(TAG, "Controllo periodico aggiornamenti schedulato con successo")
        }
    }
}
