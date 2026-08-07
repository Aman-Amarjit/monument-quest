package com.monumentquest

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MonumentQuestApp : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:100000000000:android:abcdef1234567890")
                    .setProjectId("monument-quest-app")
                    .setApiKey("AIzaSyDemoKeyForMonumentQuest123456")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            Log.w("MonumentQuestApp", "Firebase fallback initialization notice", e)
        }
    }
}
