package org.teslasoft.id.example

import android.app.Application
import android.content.Context
import com.google.android.material.color.DynamicColors

public class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MainApp.appContext = applicationContext
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

    companion object {
        lateinit var appContext : Context
    }
}
