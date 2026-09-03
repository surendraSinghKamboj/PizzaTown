package com.pizzatown.delivery

import com.pizzatown.delivery.core.notifications.DeliveryNotificationRegistrar

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DeliveryApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        DeliveryNotificationRegistrar().ensureChannel()
        DeliveryNotificationRegistrar().registerCurrentToken()
    }
}
