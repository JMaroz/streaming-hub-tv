package it.streaminghub.tv.data.update

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Job periodico di sistema per controllare gli aggiornamenti su GitHub Releases in background.
 */
class UpdateJobService : JobService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Esecuzione job periodico controllo aggiornamenti...")
        scope.launch {
            try {
                AppUpdateManager.getInstance(applicationContext).checkForUpdates(silent = true)
            } catch (t: Throwable) {
                Log.w(TAG, "Job controllo aggiornamenti fallito: ${t.message}")
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        scope.cancel()
        return false
    }

    companion object {
        private const val TAG = "UpdateJobService"
    }
}
