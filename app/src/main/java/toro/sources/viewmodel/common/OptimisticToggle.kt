package toro.sources.viewmodel.common

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun <T> optimisticToggle(
    scope: CoroutineScope,
    applyOptimistically: () -> T,
    networkCall: suspend () -> Unit,
    rollback: (previous: T) -> Unit
) {
    val previous = applyOptimistically()
    scope.launch {
        try {
            withContext(Dispatchers.IO) { networkCall() }
        } catch (e: Exception) {
            Log.e("Toggle", "${e.message}")
            rollback(previous)
        }
    }
}