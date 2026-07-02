package uz.vazifa.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.vazifa.app.data.repository.AnnouncementRepository
import javax.inject.Inject

@AndroidEntryPoint
class AnnouncementAckReceiver : BroadcastReceiver() {
    @Inject lateinit var announcementRepository: AnnouncementRepository

    override fun onReceive(context: Context, intent: Intent) {
        val announcementId = intent.getStringExtra(EXTRA_ANNOUNCEMENT_ID) ?: return
        val notifyId = intent.getIntExtra(EXTRA_NOTIFY_ID, announcementId.hashCode())
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            runCatching { announcementRepository.acknowledge(announcementId) }
            NotificationManagerCompat.from(context).cancel(notifyId)
            pendingResult.finish()
        }
    }

    companion object {
        const val ACTION_ACKNOWLEDGE = "uz.vazifa.app.ANNOUNCEMENT_ACK"
        const val EXTRA_ANNOUNCEMENT_ID = "announcement_id"
        const val EXTRA_NOTIFY_ID = "notify_id"
    }
}
