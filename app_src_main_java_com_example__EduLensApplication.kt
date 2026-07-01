package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.EduLensRepository

class EduLensApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: EduLensRepository
        private set

    lateinit var preferences: UserPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        repository = EduLensRepository(database.scanHistoryDao())
        preferences = UserPreferences(this)
    }
}
