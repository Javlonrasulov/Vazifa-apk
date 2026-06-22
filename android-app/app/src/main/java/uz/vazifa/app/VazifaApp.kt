package uz.vazifa.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import uz.vazifa.app.notifications.VazifaNotificationHelper

object AppForegroundState {
    @Volatile
    var isInForeground: Boolean = false
        private set

    internal fun setForeground(value: Boolean) {
        isInForeground = value
    }
}

@HiltAndroidApp
class VazifaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        VazifaNotificationHelper.createChannels(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                AppForegroundState.setForeground(true)
            }

            override fun onStop(owner: LifecycleOwner) {
                AppForegroundState.setForeground(false)
            }
        })
    }
}
