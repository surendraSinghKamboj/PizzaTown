package com.pizzatown.admin

import android.app.Application
import com.pizzatown.admin.core.notifications.AdminNotificationRegistrar
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PizzaTownAdminApp : Application() {

    @Inject lateinit var notificationRegistrar: AdminNotificationRegistrar

    override fun onCreate() {
        super.onCreate()
        notificationRegistrar.ensureChannel()
    }
}
