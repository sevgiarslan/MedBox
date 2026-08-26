package com.medbox.app

import android.app.Application
import com.medbox.app.notification.ExpirationCheckWorker
import com.medbox.app.notification.NotificationHelper
import com.medbox.app.util.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedBoxApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)
        ExpirationCheckWorker.schedule(this)

        CoroutineScope(Dispatchers.IO).launch {
            container.repository.ensureDefaultTags()
        }
    }
}
