package toro.sources

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
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
            val myUserId = RetrofitClient.preferenceManager.getUserDataSync().userId

            val pendingMessages = db.chatMessageDao().getPendingMessages(myUserId ?: "")

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
}