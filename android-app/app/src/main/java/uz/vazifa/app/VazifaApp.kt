package uz.vazifa.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors
import uz.vazifa.app.di.ImageLoaderEntryPoint
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
class VazifaApp : Application(), ImageLoaderFactory {
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

    override fun newImageLoader(): ImageLoader {
        val entryPoint = EntryPointAccessors.fromApplication(this, ImageLoaderEntryPoint::class.java)
        return entryPoint.imageLoader()
    }
}
