package toro.sources

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import toro.sources.db.CanvasDatabase
import toro.sources.network.RetrofitClient

class MessageSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = CanvasDatabase.getDatabase(applicationContext)
            val pendingMessages = db.chatMessageDao().getPendingMessages()

            if (pendingMessages.isEmpty()) {
                return@withContext Result.success()
            }

            Log.i("MessageSync", "Attempting to sync ${pendingMessages.size} pending messages")

            val response = RetrofitClient.comicApiService.syncPendingMessages(pendingMessages)

            if (response.isSuccessful) {
                pendingMessages.forEach { msg ->
                    db.chatMessageDao().updateMessageDeliveryStatus(msg.id, true)
                }
                Log.i("MessageSync", "Successfully synced all pending messages")
                Result.success()
            } else {
                Log.e("MessageSync", "Server rejected sync")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("MessageSync", "Sync failed due to network error: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "message_sync_worker"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<MessageSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}