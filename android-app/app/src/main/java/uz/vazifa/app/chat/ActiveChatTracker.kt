package uz.vazifa.app.chat

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Hozir ochiq DM chat (push kelganda darhol yangilash uchun) */
object ActiveChatTracker {
    @Volatile
    var peerId: String? = null
        private set

    private val _refreshRequests = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val refreshRequests: SharedFlow<String> = _refreshRequests

    fun setActive(peerId: String?) {
        this.peerId = peerId?.takeIf { it.isNotBlank() }
    }

    fun requestRefresh(peerId: String) {
        if (peerId.isBlank()) return
        _refreshRequests.tryEmit(peerId)
    }
}
