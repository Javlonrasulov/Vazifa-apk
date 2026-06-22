package uz.vazifa.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import uz.vazifa.app.AppForegroundState
import uz.vazifa.app.R
import uz.vazifa.app.presentation.MainActivity

object VazifaNotificationHelper {
    const val CHANNEL_TASKS = "vazifa_tasks"
    private const val GROUP_TASKS = "vazifa_task_group"

    private val vibrationPattern = longArrayOf(0, 600, 200, 600, 200, 600, 200, 800)

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val existing = manager.getNotificationChannel(CHANNEL_TASKS)
        if (existing != null) return
        val channel = NotificationChannel(CHANNEL_TASKS, "Vazifalar", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Yangi vazifa va eslatmalar"
            enableVibration(true)
            this.vibrationPattern = vibrationPattern
            setSound(sound, attrs)
            enableLights(true)
            setShowBadge(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun canShowNotifications(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun show(
        context: Context,
        title: String,
        body: String,
        taskId: String?,
        type: String?,
    ) {
        if (!canShowNotifications(context)) return

        createChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            taskId?.let { putExtra(EXTRA_TASK_ID, it) }
            type?.let { putExtra(EXTRA_TYPE, it) }
        }
        val pending = PendingIntent.getActivity(
            context,
            (taskId?.hashCode() ?: title.hashCode()),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setGroup(GROUP_TASKS)
            .setOnlyAlertOnce(false)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(vibrationPattern)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                taskId?.hashCode() ?: System.currentTimeMillis().toInt(),
                notification,
            )
        } catch (_: SecurityException) {
            return
        }

        if (AppForegroundState.isInForeground) {
            playAlertSound(context)
        }
    }

    private fun playAlertSound(context: Context) {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
        val ringtone: Ringtone = RingtoneManager.getRingtone(context, uri) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        }
        ringtone.play()
    }

    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TYPE = "notif_type"
}
