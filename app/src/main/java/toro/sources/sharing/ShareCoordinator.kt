package toro.sources.sharing

import com.toro.models.SharedContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareCoordinator @Inject constructor() {
    private val _sharedContent = MutableStateFlow<SharedContent?>(null)
    val sharedContent = _sharedContent.asStateFlow()

    fun setSharedContent(content: SharedContent?) {
        _sharedContent.value = content
    }
}