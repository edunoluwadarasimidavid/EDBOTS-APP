package com.example.di

import android.content.Context
import com.example.data.local.EdBotsDatabase
import com.example.data.local.EdBotsPreferences
import com.example.data.remote.AppwriteApiClient
import com.example.data.remote.EdBotsApiClient
import com.example.data.repository.AuthRepository
import com.example.data.repository.BotRepository

class AppContainer(context: Context) {
    val preferences: EdBotsPreferences by lazy { EdBotsPreferences(context) }
    val database: EdBotsDatabase by lazy { EdBotsDatabase.getInstance(context) }

    val appwriteClient: AppwriteApiClient by lazy { AppwriteApiClient(preferences) }
    val edbotsClient: EdBotsApiClient by lazy { EdBotsApiClient(preferences) }

    val authRepository: AuthRepository by lazy { AuthRepository(appwriteClient, preferences) }
    val botRepository: BotRepository by lazy { BotRepository(edbotsClient, database, preferences) }
}
