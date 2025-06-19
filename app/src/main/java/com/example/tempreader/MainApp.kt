package com.example.tempreader

import android.app.Application
import com.example.tempreader.data.local.AppDatabase
import com.example.tempreader.data.repository.ReadingRepository
import com.example.tempreader.ui.auth.AuthViewModel
import com.example.tempreader.ui.dashboard.DashboardViewModel
import com.google.firebase.auth.FirebaseAuth
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MainApp)
            modules(
                module {
                    single {
                        AppDatabase.getDatabase(get()).readingDao()
                    }
                    single {
                        FirebaseAuth.getInstance()
                    }
                    single {
                        ReadingRepository(get(), get())
                    }
                    viewModel { DashboardViewModel(get()) }
                    viewModel { AuthViewModel(get()) }
                },
            )
        }
    }
}