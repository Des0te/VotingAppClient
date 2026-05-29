package com.example.votingclient

import android.app.Application
import com.example.votingclient.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class VotingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@VotingApplication)
            modules(appModule)
        }
    }
}
