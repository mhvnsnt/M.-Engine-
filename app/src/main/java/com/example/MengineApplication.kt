package com.example

import android.app.Application
import android.content.Context

class MengineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.example.network.FirebaseManager.initialize(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }
    }
