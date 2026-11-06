package it.streaminghub.tv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import it.streaminghub.tv.data.update.AppUpdateManager

/**
 * Riceve gli eventi di stato dall'installer di sistema Android (`PackageInstaller`),
 * il completamento del boot (`BOOT_COMPLETED`) e l'avvenuto rimpiazzo del pacchetto (`MY_PACKAGE_REPLACED`).
 */
class UpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Ricevuta broadcast action: $action")

        when (action) {
            AppUpdateManager.ACTION_INSTALL_STATUS -> {
                val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.i(TAG, "Status PackageInstaller: $status (${message ?: "nessun messaggio"})")

                when (status) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        // Android richiede la conferma dell'utente. Avvia la schermata di dialogo di sistema
                        val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(Intent.EXTRA_INTENT)
                        }

                        if (confirmIntent != null) {
                            confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                context.startActivity(confirmIntent)
                            } catch (e: Exception) {
                                Log.e(TAG, "Impossibile aprire il dialogo di conferma di sistema", e)
                                AppUpdateManager.getInstance(context).onInstallFailed("Impossibile aprire l'installer")
                            }
                        } else {
                            AppUpdateManager.getInstance(context).onInstallFailed("Intent di conferma nullo")
                        }
                    }

                    PackageInstaller.STATUS_SUCCESS -> {
                        Log.i(TAG, "Aggiornamento completato con successo!")
                        AppUpdateManager.getInstance(context).onInstallSuccess()
                    }

                    PackageInstaller.STATUS_FAILURE_ABORTED -> {
                        Log.w(TAG, "Installazione annullata dall'utente")
                        AppUpdateManager.getInstance(context).dismiss()
                    }

                    else -> {
                        val errorDesc = message ?: "Errore sconosciuto ($status)"
                        Log.e(TAG, "Installazione fallita: $errorDesc")
                        AppUpdateManager.getInstance(context).onInstallFailed(errorDesc)
                    }
                }
            }

            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.i(TAG, "Applicazione aggiornata a nuova versione. Pulizia residui...")
                val updateManager = AppUpdateManager.getInstance(context)
                updateManager.cleanUp()
                AppUpdateManager.schedulePeriodicCheck(context)
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Boot TV completato. Ripristino schedulazione verifica aggiornamenti...")
                AppUpdateManager.schedulePeriodicCheck(context)
            }
        }
    }

    companion object {
        private const val TAG = "UpdateReceiver"
    }
}
