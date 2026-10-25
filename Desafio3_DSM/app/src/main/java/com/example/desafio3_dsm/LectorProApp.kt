package com.example.desafio3_dsm

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class LectorProApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Forzar tema claro en toda la app sin importar la configuración del dispositivo
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
