package com.pizzatown.customer

import android.app.Application
import com.pizzatown.customer.core.notifications.NotificationRegistrar
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PizzaTownCustomerApp : Application() {

    @Inject lateinit var notificationRegistrar: NotificationRegistrar

    override fun onCreate() {
        super.onCreate()
        notificationRegistrar.ensureChannel()
    }
}
