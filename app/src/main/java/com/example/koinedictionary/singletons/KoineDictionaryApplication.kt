package com.example.koinedictionary.singletons

import android.app.Application

class KoineDictionaryApplication: Application() {
    companion object {
        lateinit var instance: KoineDictionaryApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}