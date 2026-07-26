package com.whoisbarry.pocketgreekdictionary.singletons

import android.app.Application

class PocketGreekDictionaryApplication: Application() {
    companion object {
        lateinit var instance: PocketGreekDictionaryApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}