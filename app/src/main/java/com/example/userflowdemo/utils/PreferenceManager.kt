package com.example.userflowdemo.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveUserName(name: String) {
        sharedPreferences.edit().putString("user_name", name).apply()
    }

    fun getUserName(): String {
        return sharedPreferences.getString("user_name", "") ?: ""
    }

    fun setOnboardingCompleted() {
        sharedPreferences.edit().putBoolean("has_onboarded", true).apply()
    }

    fun hasOnboarded(): Boolean {
        return sharedPreferences.getBoolean("has_onboarded", false)
    }
}
