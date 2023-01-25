package org.ligi.blexplorer

import androidx.multidex.MultiDexApplication
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        if(BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        bluetoothController = BluetoothController(this)
    }
}

internal lateinit var bluetoothController: BluetoothController
    private set