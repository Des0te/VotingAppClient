package com.example.votingclient.di

import com.example.votingclient.data.local.SessionStore
import com.example.votingclient.data.local.SettingsStore
import com.example.votingclient.data.network.ApiService
import com.example.votingclient.data.repository.VotingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {
    single { SessionStore(androidContext()) }
    single { SettingsStore(androidContext()) }
    single { provideApi(get()) }
    single { VotingRepository(get(), get()) }
}

private fun provideApi(sessionStore: SessionStore): ApiService {
    val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = runBlocking { sessionStore.token.first() }
            val request = chain.request().newBuilder().apply {
                if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
            }.build()
            chain.proceed(request)
        }
        .build()

    return Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8080/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}
